#!/bin/sh
# Install the clang-22 toolchain and WASI sysroot used by local builds.
#
# The mozconfigs point at these paths directly and tell mach not to update them.
# LLVM 22 uses the wasm32-wasip1 target name, while some of the WASI bits still
# ship under wasm32-wasi, so this script also creates the aliases clang expects.
set -eu

case "$(uname -s)-$(uname -m)" in
    Linux-x86_64)  ns="linux64-clang-22" ;;
    Darwin-arm64)  ns="macosx64-aarch64-clang-22" ;;
    Darwin-x86_64) ns="macosx64-clang-22" ;;
    *) echo "fetch-clang-22: unsupported host $(uname -s)-$(uname -m)" >&2; exit 1 ;;
esac

dest="$HOME/.mozbuild/clang-22"
url="https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/gecko.cache.level-3.toolchains.v3.${ns}.latest/artifacts/public/build/clang.tar.zst"
sysroot="$HOME/.mozbuild/sysroot-wasm32-wasi"
sysroot_url="https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/gecko.cache.level-3.toolchains.v3.sysroot-wasm32-wasi-clang-22.latest/artifacts/public/build/sysroot-wasm32-wasi.tar.zst"

# Download clang for this host.
if [ -x "$dest/bin/clang" ]; then
    echo "fetch-clang-22: clang already present, skipping download."
else
    echo "fetch-clang-22: downloading $ns ..."
    rm -rf "$dest"
    mkdir -p "$dest"
    curl -L --retry 5 --fail -o "$dest/clang.tar.zst" "$url"
    if tar --zstd --strip-components=1 -xf "$dest/clang.tar.zst" -C "$dest" 2>/dev/null; then
        :
    elif command -v zstd >/dev/null 2>&1; then
        zstd -dc "$dest/clang.tar.zst" | tar -x --strip-components=1 -C "$dest"
    else
        tar --strip-components=1 -xf "$dest/clang.tar.zst" -C "$dest"
    fi
    rm -f "$dest/clang.tar.zst"
fi
"$dest/bin/clang" --version | head -n1

# Add the WASI builtins path clang 22 expects.
for libdir in "$dest"/lib/clang/*/lib; do
    if [ -f "$libdir/wasi/libclang_rt.builtins-wasm32.a" ]; then
        mkdir -p "$libdir/wasm32-unknown-wasip1"
        ln -sf ../wasi/libclang_rt.builtins-wasm32.a \
            "$libdir/wasm32-unknown-wasip1/libclang_rt.builtins.a"
    fi
done

# Download the WASI sysroot and add wasm32-wasip1 aliases.
if [ ! -d "$sysroot/lib/wasm32-wasi" ]; then
    echo "fetch-clang-22: downloading wasi sysroot ..."
    rm -rf "$sysroot"
    mkdir -p "$sysroot"
    curl -L --retry 5 --fail -o "$sysroot.tar.zst" "$sysroot_url"
    if tar --zstd --strip-components=1 -xf "$sysroot.tar.zst" -C "$sysroot" 2>/dev/null; then
        :
    elif command -v zstd >/dev/null 2>&1; then
        zstd -dc "$sysroot.tar.zst" | tar -x --strip-components=1 -C "$sysroot"
    else
        tar --strip-components=1 -xf "$sysroot.tar.zst" -C "$sysroot"
    fi
    rm -f "$sysroot.tar.zst"
fi
if [ -d "$sysroot/lib/wasm32-wasi" ]; then
    ln -sfn wasm32-wasi "$sysroot/lib/wasm32-wasip1"
    if [ -d "$sysroot/lib/wasm32-wasi-threads" ]; then
        ln -sfn wasm32-wasi-threads "$sysroot/lib/wasm32-wasip1-threads"
    fi
    if [ -d "$sysroot/share/wasm32-wasi" ]; then
        ln -sfn wasm32-wasi "$sysroot/share/wasm32-wasip1"
    fi
    if [ -d "$sysroot/share/wasm32-wasi-threads" ]; then
        ln -sfn wasm32-wasi-threads "$sysroot/share/wasm32-wasip1-threads"
    fi
    echo "fetch-clang-22: aliased wasi sysroot to wasm32-wasip1."
else
    echo "fetch-clang-22: ERROR: wasi sysroot not found after download." >&2
    exit 1
fi

echo "fetch-clang-22: done."
