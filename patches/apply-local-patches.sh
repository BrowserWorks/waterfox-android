#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

patch_file="$root_dir/patches/glean-noop.patch"
toolchain_patch_file="$root_dir/patches/glean-157-toolchain.patch"
expected_revision="9d987fd33e1dc3f2a89e5f5c4565ae696245f6eb"

if [ "$(git -C "$root_dir/glean" rev-parse HEAD)" != "$expected_revision" ]; then
    echo "The no-op patch requires Glean v70.2.0 ($expected_revision); no files were changed." >&2
    echo "Preserve any local Glean changes before updating the submodule." >&2
    exit 1
fi

if git -C "$root_dir/glean" apply --reverse --check "$patch_file" >/dev/null 2>&1; then
    echo "The current no-op Glean patch is already applied."
elif git -C "$root_dir/glean" apply --check "$patch_file" >/dev/null 2>&1; then
    git -C "$root_dir/glean" apply "$patch_file"
elif git -C "$root_dir/glean" apply --reverse --check --exclude=gradle/libs.versions.toml "$patch_file" >/dev/null 2>&1 \
    && git -C "$root_dir/glean" apply --check "$toolchain_patch_file" >/dev/null 2>&1; then
    git -C "$root_dir/glean" apply "$toolchain_patch_file"
    echo "Updated the existing no-op Glean patch to the Firefox Android 158.0 toolchain."
else
    echo "Cannot apply the current no-op Glean patch; no files were changed." >&2
    echo "Preserve any local Glean changes and use a clean checkout of the pinned submodule." >&2
    exit 1
fi
