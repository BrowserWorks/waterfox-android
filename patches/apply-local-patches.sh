#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

pushd "$root_dir/glean" >/dev/null
git apply "$root_dir/patches/glean-noop.patch"
popd >/dev/null
