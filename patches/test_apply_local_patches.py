#!/usr/bin/env python3
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
PINNED_REVISION = "9d987fd33e1dc3f2a89e5f5c4565ae696245f6eb"


class ApplyLocalPatchesTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory(dir=ROOT)
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        shutil.copytree(ROOT / "patches", self.root / "patches")
        self.glean = self.root / "glean"
        self.run_command(
            "git",
            "clone",
            "--shared",
            "--no-checkout",
            str(ROOT / "glean"),
            str(self.glean),
        )
        self.git("checkout", "--detach", PINNED_REVISION)

    def run_command(self, *args, check=True):
        return subprocess.run(args, cwd=self.root, check=check, capture_output=True)

    def git(self, *args, check=True):
        return self.run_command(
            "git",
            "--no-pager",
            "-C",
            str(self.glean),
            *args,
            check=check,
        )

    def apply_script(self, check=True):
        return self.run_command(
            "bash",
            str(self.root / "patches/apply-local-patches.sh"),
            check=check,
        )

    def snapshot(self, exclude_versions=False):
        paths = ["."]
        if exclude_versions:
            paths.append(":(exclude)gradle/libs.versions.toml")
        diff = self.git("diff", "--binary", "--", *paths).stdout
        untracked = self.git("ls-files", "--others", "--exclude-standard", "-z").stdout
        contents = [
            (name, (self.glean / name.decode()).read_bytes())
            for name in untracked.split(b"\0")
            if name
        ]
        return diff, contents

    def assert_current_patch(self):
        self.git(
            "apply", "--reverse", "--check", str(self.root / "patches/glean-noop.patch")
        )
        versions = (self.glean / "gradle/libs.versions.toml").read_text()
        self.assertIn('android-plugin = "9.4.0"', versions)
        self.assertIn('kotlin-compiler = "2.4.20"', versions)

    def apply_previous_patch(self):
        self.apply_script()
        self.git(
            "apply", "--reverse", str(self.root / "patches/glean-157-toolchain.patch")
        )

    def test_clean_checkout_and_idempotence(self):
        self.apply_script()
        self.assert_current_patch()
        before = self.snapshot()
        self.apply_script()
        self.assertEqual(before, self.snapshot())

    def test_upgrade_preserves_unrelated_changes(self):
        self.apply_previous_patch()
        with (self.glean / "README.md").open("a") as readme:
            readme.write("\nLocal developer change\n")
        (self.glean / "local-note.txt").write_text("Untracked developer file\n")
        before = self.snapshot(exclude_versions=True)
        self.apply_script()
        self.assert_current_patch()
        self.assertEqual(before, self.snapshot(exclude_versions=True))
        current = self.snapshot()
        self.apply_script()
        self.assertEqual(current, self.snapshot())

    def test_unknown_toolchain_is_not_overwritten(self):
        self.apply_previous_patch()
        versions = self.glean / "gradle/libs.versions.toml"
        versions.write_text(
            versions.read_text().replace(
                'android-plugin = "9.2.1"',
                'android-plugin = "9.3.0"',
            )
        )
        before = self.snapshot()
        self.assertNotEqual(0, self.apply_script(check=False).returncode)
        self.assertEqual(before, self.snapshot())

    def test_conflicting_noop_changes_are_not_overwritten(self):
        self.apply_previous_patch()
        build = self.glean / "build.gradle"
        build.write_text(
            build.read_text().replace(
                "compilerOptions.allWarningsAsErrors.set(true)",
                "compilerOptions.allWarningsAsErrors.set(false)",
            )
        )
        before = self.snapshot()
        self.assertNotEqual(0, self.apply_script(check=False).returncode)
        self.assertEqual(before, self.snapshot())

    def test_wrong_revision_is_not_modified(self):
        self.git("checkout", "--detach", PINNED_REVISION + "^")
        before = self.snapshot()
        self.assertNotEqual(0, self.apply_script(check=False).returncode)
        self.assertEqual(before, self.snapshot())


if __name__ == "__main__":
    unittest.main()
