#!/usr/bin/env python3
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

import argparse
import io
import json
import os
import re
import subprocess
import zipfile
from pathlib import Path

import tomlkit

SOURCE = Path(__file__).resolve().parent.parent
L10N_REPOSITORY = "https://github.com/mozilla-l10n/firefox-l10n.git"


def selected_locales(requested):
    configuration = tomlkit.parse(
        (SOURCE / "mobile/android/locales/l10n.toml").read_text()
    )
    changesets = json.loads(
        (SOURCE / "mobile/locales/l10n-changesets.json").read_text()
    )
    supported = (
        set(configuration["locales"])
        - set(configuration["build"]["exclude-multi-locale"])
    ) & set(changesets)
    locales = supported if requested is None else set(requested) - {"en-US"}
    unsupported = locales - supported
    if unsupported:
        raise ValueError(f"Unsupported locales: {', '.join(sorted(unsupported))}")
    revisions = {changesets[locale]["revision"] for locale in locales}
    if len(revisions) > 1:
        raise ValueError(
            "Selected locales have different pinned localization revisions"
        )
    revision = next(iter(revisions), None)
    if revision is not None and not re.fullmatch(r"[0-9a-f]{40}", revision):
        raise ValueError(
            f"Localization revision is not an immutable Git SHA: {revision}"
        )
    return sorted(locales), revision


def verify_omnijar(archive, label, expected):
    try:
        advertised = set(
            re.split(r"[,\s]+", archive.read("res/multilocale.txt").decode().strip())
        )
    except KeyError as error:
        raise ValueError(f"{label}: missing res/multilocale.txt") from error
    missing = expected - advertised
    if missing:
        raise ValueError(f"{label}: missing locales: {', '.join(sorted(missing))}")
    resources = {
        entry.filename.split("/", 2)[1]
        for entry in archive.infolist()
        if entry.filename.startswith("localization/")
        and entry.filename.count("/") >= 2
        and entry.filename.endswith(".ftl")
        and entry.file_size > 0
    }
    missing = expected - resources
    if missing:
        raise ValueError(
            f"{label}: missing localization resources: {', '.join(sorted(missing))}"
        )


def verify_archive(archive, label, expected):
    if "res/multilocale.txt" in archive.namelist():
        verify_omnijar(archive, label, expected)
        return 1
    count = 0
    for name in archive.namelist():
        if name.endswith("/assets/omni.ja") or name == "assets/omni.ja":
            with zipfile.ZipFile(io.BytesIO(archive.read(name))) as omnijar:
                verify_omnijar(omnijar, f"{label}!{name}", expected)
            count += 1
        elif name.endswith(".aar") and Path(name).name.startswith("geckoview-"):
            with zipfile.ZipFile(io.BytesIO(archive.read(name))) as aar:
                count += verify_archive(aar, f"{label}!{name}", expected)
    if not count:
        raise ValueError(f"{label}: no GeckoView omnijar or AAR found")
    return count


def verify(path, locales):
    with zipfile.ZipFile(path) as archive:
        count = verify_archive(archive, str(path), set(locales) | {"en-US"})
    print(f"Verified {path}: {count} omnijar(s), {len(locales) + 1} locales")


def prepare_localizations(checkout, locales, revision):
    if checkout.is_symlink():
        raise ValueError(f"Managed localization checkout is a symlink: {checkout}")
    checkout.mkdir(parents=True, exist_ok=True)

    def git(*arguments, capture=False, **kwargs):
        result = subprocess.run(
            ["git", "-C", str(checkout), *arguments],
            check=True,
            text=True,
            stdout=subprocess.PIPE if capture else None,
            **kwargs,
        )
        return result.stdout.strip() if capture else None

    if not (checkout / ".git").is_dir():
        if any(checkout.iterdir()):
            raise ValueError(f"Managed localization directory is not empty: {checkout}")
        git("init", "--quiet")
        git("remote", "add", "origin", L10N_REPOSITORY)
    if git("remote", "get-url", "origin", capture=True) != L10N_REPOSITORY:
        raise ValueError(f"Unexpected localization repository in {checkout}")
    if git("status", "--porcelain", capture=True):
        raise ValueError(f"Managed localization checkout has local changes: {checkout}")
    git("config", "remote.origin.promisor", "true")
    git("config", "remote.origin.partialclonefilter", "blob:none")
    git("sparse-checkout", "init", "--cone")
    git(
        "sparse-checkout",
        "set",
        "--stdin",
        input="\n".join(
            f"{locale}/{directory}"
            for locale in locales
            for directory in ("dom", "toolkit", "mobile/android")
        )
        + "\n",
    )
    git("fetch", "--no-tags", "--depth=1", "--filter=blob:none", "origin", revision)
    if git("rev-parse", "FETCH_HEAD", capture=True) != revision:
        raise ValueError("Fetched localization revision does not match its pin")
    git("checkout", "--quiet", "--detach", revision)
    if git("rev-parse", "HEAD", capture=True) != revision:
        raise ValueError("Localization checkout does not match its pin")


def package(objdir, locales, revision):
    configuration = json.loads((objdir / "config.status.json").read_text())
    substs = configuration["substs"]
    if Path(configuration["topsrcdir"]).resolve() != SOURCE:
        raise ValueError(
            f"Object directory belongs to another source checkout: {objdir}"
        )
    if "RecursiveMake" not in substs["BUILD_BACKENDS"]:
        raise ValueError("Locale packaging requires the RecursiveMake build backend")
    if substs["MOZ_BUILD_APP"] != "mobile/android":
        raise ValueError("Locale packaging requires an Android object directory")
    environment = dict(
        os.environ,
        GRADLE_INVOKED_WITHIN_MACH_BUILD="1",
        MOZ_CHROME_MULTILOCALE=" ".join(locales),
    )
    make = [substs["GMAKE"], "-C", str(objdir), "-j1"]
    if locales:
        checkout = objdir / "waterfox-l10n"
        prepare_localizations(checkout, locales, revision)
        subprocess.run(
            [
                *make,
                *(f"chrome-{locale}" for locale in locales),
                f"L10NBASEDIR={checkout}",
            ],
            env=environment,
            check=True,
        )
    subprocess.run([*make, "package", "AB_CD=multi"], env=environment, check=True)
    verify(objdir / "dist/geckoview/assets/omni.ja", locales)


def main():
    parser = argparse.ArgumentParser(
        description="Package pinned Gecko locales without rebuilding native code."
    )
    parser.add_argument("--objdir", type=Path, default=SOURCE / "objdir")
    parser.add_argument(
        "--locales",
        nargs="+",
        help="Subset of supported Android locales; en-US is always included",
    )
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument(
        "--print-locales",
        action="store_true",
        help="Print selected non-English locales without changing files",
    )
    mode.add_argument(
        "--verify",
        type=Path,
        nargs="+",
        metavar="PATH",
        help="Verify omnijars, AARs, APKs, AABs, or GeckoView Maven ZIPs without changing files",
    )
    args = parser.parse_args()
    try:
        locales, revision = selected_locales(args.locales)
        if args.print_locales:
            print(" ".join(locales))
        elif args.verify is not None:
            for path in args.verify:
                verify(path, locales)
        else:
            package(args.objdir.resolve(), locales, revision)
    except (
        OSError,
        ValueError,
        KeyError,
        subprocess.CalledProcessError,
        zipfile.BadZipFile,
    ) as error:
        parser.exit(1, f"error: {error}\n")


if __name__ == "__main__":
    main()
