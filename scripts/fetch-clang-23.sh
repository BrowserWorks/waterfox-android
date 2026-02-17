#!/bin/sh
set -eu

case "$(uname -s)-$(uname -m)" in
    Linux-x86_64) ns="linux64-clang-23" ;;
    Linux-aarch64|Linux-arm64) ns="linux64-aarch64-clang-23" ;;
    Darwin-arm64) ns="macosx64-aarch64-clang-23" ;;
    Darwin-x86_64) ns="macosx64-clang-23" ;;
    *) echo "fetch-clang-23: unsupported host $(uname -s)-$(uname -m)" >&2; exit 1 ;;
esac

dest="$HOME/.mozbuild/clang-23"
sysroot="$HOME/.mozbuild/sysroot-wasm32-wasi"
tc_base="https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/gecko.cache.level-3.toolchains.v3"
clang_url="$tc_base.${ns}.latest/artifacts/public/build/clang.tar.zst"
sysroot_url="$tc_base.sysroot-wasm32-wasi-clang-23.latest/artifacts/public/build/sysroot-wasm32-wasi.tar.zst"

mkdir -p "$HOME/.mozbuild"
stage_root=$(mktemp -d "$HOME/.mozbuild/.clang-23-fetch.XXXXXX")
trap 'rm -rf "$stage_root"' 0
trap 'exit 1' 1 2 15

unpack_tar_zst() {
    archive="$1"
    out="$2"
    if tar --zstd --strip-components=1 -xf "$archive" -C "$out" 2>/dev/null; then
        return
    elif command -v zstd >/dev/null 2>&1; then
        zstd -dc "$archive" > "$archive.tar"
    elif command -v 7z >/dev/null 2>&1; then
        7z x "$archive" -so > "$archive.tar"
    else
        tar --strip-components=1 -xf "$archive" -C "$out"
        return
    fi
    tar --strip-components=1 -xf "$archive.tar" -C "$out"
    rm -f "$archive.tar"
}

clang_ready() {
    for compiler in clang clang++; do
        [ -x "$1/bin/$compiler" ] || return 1
        compiler_version=$("$1/bin/$compiler" -dumpversion 2>/dev/null) || return 1
        case "$compiler_version" in
            23|23.*) ;;
            *) return 1 ;;
        esac
    done
}

if clang_ready "$dest"; then
    echo "fetch-clang-23: clang already present, skipping download."
else
    echo "fetch-clang-23: downloading $ns ..."
    mkdir -p "$stage_root/clang"
    curl -L --retry 5 --fail -o "$stage_root/clang.tar.zst" "$clang_url"
    unpack_tar_zst "$stage_root/clang.tar.zst" "$stage_root/clang"
    if ! clang_ready "$stage_root/clang"; then
        echo "fetch-clang-23: ERROR: downloaded clang/clang++ are not executable Clang 23 compilers." >&2
        exit 1
    fi
    rm -rf "$dest"
    mv "$stage_root/clang" "$dest"
fi
"$dest/bin/clang" --version

if [ ! -f "$sysroot/.clang-23" ] ||
   [ ! -f "$sysroot/lib/wasm32-wasip1/crt1.o" ] ||
   [ ! -d "$sysroot/include/wasm32-wasip1" ]; then
    echo "fetch-clang-23: downloading wasi sysroot ..."
    mkdir -p "$stage_root/sysroot"
    curl -L --retry 5 --fail -o "$stage_root/sysroot.tar.zst" "$sysroot_url"
    unpack_tar_zst "$stage_root/sysroot.tar.zst" "$stage_root/sysroot"
    if [ ! -f "$stage_root/sysroot/lib/wasm32-wasip1/crt1.o" ] ||
       [ ! -d "$stage_root/sysroot/include/wasm32-wasip1" ]; then
        echo "fetch-clang-23: ERROR: downloaded Clang 23 WASI sysroot is incomplete." >&2
        exit 1
    fi
    touch "$stage_root/sysroot/.clang-23"
    rm -rf "$sysroot"
    mv "$stage_root/sysroot" "$sysroot"
fi

echo "fetch-clang-23: done."
