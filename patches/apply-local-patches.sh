#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

patch_file="$root_dir/patches/glean-noop.patch"
expected_revision="92ecd18105fd2936e11936da749183369a491580"

if [ "$(git -C "$root_dir/glean" rev-parse HEAD)" != "$expected_revision" ]; then
    echo "The no-op patch requires Glean v68.0.1 ($expected_revision); no files were changed." >&2
    echo "Preserve any local Glean changes before updating the submodule." >&2
    exit 1
fi

if git -C "$root_dir/glean" apply --reverse --check "$patch_file" >/dev/null 2>&1; then
    echo "The current no-op Glean patch is already applied."
elif git -C "$root_dir/glean" apply --check "$patch_file"; then
    git -C "$root_dir/glean" apply "$patch_file"
else
    echo "Cannot apply the current no-op Glean patch; no files were changed." >&2
    echo "Preserve any local Glean changes and use a clean checkout of the pinned submodule." >&2
    exit 1
fi
