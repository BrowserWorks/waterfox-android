#!/usr/bin/env python3
import argparse
import os
import zipfile


def iter_maven_files(maven_root, include_snapshots):
    for root, _, files in os.walk(maven_root):
        if not include_snapshots and "-SNAPSHOT" in root:
            continue
        for filename in files:
            yield os.path.join(root, filename)


def create_zip(objdir, include_snapshots):
    gradle_dir = os.path.join(objdir, "gradle")
    maven_dir = os.path.join(gradle_dir, "maven")
    out_zip = os.path.join(gradle_dir, "target.maven.zip")

    if not os.path.isdir(maven_dir):
        raise FileNotFoundError(f"Missing maven directory: {maven_dir}")

    with zipfile.ZipFile(out_zip, "w") as zf:
        for abs_path in iter_maven_files(maven_dir, include_snapshots):
            rel = os.path.relpath(abs_path, maven_dir)
            zf.write(abs_path, arcname=os.path.join("geckoview", rel))

    return out_zip


def find_geckoview_aars(objdir, include_snapshots):
    gradle_dir = os.path.join(objdir, "gradle")
    maven_dir = os.path.join(gradle_dir, "maven")
    return [
        abs_path
        for abs_path in iter_maven_files(maven_dir, include_snapshots)
        if abs_path.endswith(".aar")
        and os.path.basename(abs_path).startswith("geckoview-")
    ]


def main():
    parser = argparse.ArgumentParser(description="Create target.maven.zip from gradle/maven.")
    parser.add_argument("--objdir", default="objdir", help="Top object directory")
    parser.add_argument(
        "--include-snapshots",
        action="store_true",
        help="Include -SNAPSHOT Maven artifacts",
    )
    parser.add_argument(
        "--require-aar",
        action="store_true",
        help="Fail unless exactly one geckoview-*.aar is present in the Maven repo",
    )
    args = parser.parse_args()

    if args.require_aar:
        aars = find_geckoview_aars(args.objdir, args.include_snapshots)
        if len(aars) != 1:
            maven_dir = os.path.join(args.objdir, "gradle", "maven")
            candidates = sorted(os.path.relpath(path, maven_dir) for path in aars)
            raise SystemExit(
                f"Expected exactly one geckoview-*.aar under gradle/maven, "
                f"found {len(aars)}: {candidates}"
            )

    out_zip = create_zip(args.objdir, args.include_snapshots)
    print(out_zip)


if __name__ == "__main__":
    main()