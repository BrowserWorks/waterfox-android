# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

import ctypes
import hashlib
import os
import ssl
import sys
from pathlib import Path
from urllib.request import urlopen

import certifi
from mach.site import PythonVirtualenv
from mach.util import get_state_dir

# Keep in sync with rust-version in top-level Cargo.toml.
MINIMUM_RUST_VERSION = "1.99.0"


def is_win_aarch64_host():
    """Whether the native machine is Windows on aarch64/ARM64.

    Uses IsWow64Process2 so it detects the native machine even from an
    emulated x86/x86_64 process. Returns False on non-Windows."""
    if not sys.platform.startswith("win32"):
        return False

    from ctypes import wintypes

    kernel32 = ctypes.windll.kernel32
    IMAGE_FILE_MACHINE_UNKNOWN = 0
    IMAGE_FILE_MACHINE_ARM64 = 0xAA64

    try:
        iswow64process2 = kernel32.IsWow64Process2
    except Exception:
        # If we can't access the symbol, we know we're not on aarch64.
        return False

    currentProcess = kernel32.GetCurrentProcess()
    processMachine = wintypes.USHORT(IMAGE_FILE_MACHINE_UNKNOWN)
    nativeMachine = wintypes.USHORT(IMAGE_FILE_MACHINE_UNKNOWN)

    gotValue = iswow64process2(
        currentProcess, ctypes.byref(processMachine), ctypes.byref(nativeMachine)
    )
    # If this call fails, we have no idea.
    if not gotValue:
        return False

    return nativeMachine.value == IMAGE_FILE_MACHINE_ARM64


def get_tools_dir(srcdir=False):
    if os.environ.get("MOZ_AUTOMATION") and "MOZ_FETCHES_DIR" in os.environ:
        return os.environ["MOZ_FETCHES_DIR"]
    return get_state_dir(srcdir)


def get_mach_virtualenv_root():
    return Path(get_state_dir(specific_to_topsrcdir=True)) / "_virtualenvs" / "mach"


def get_mach_virtualenv_binary():
    root = get_mach_virtualenv_root()
    return Path(PythonVirtualenv(str(root)).python_path)


def http_download_and_save(url, dest: Path, hexhash, digest="sha256"):
    """Download the given url and save it to dest.  hexhash is a checksum
    that will be used to validate the downloaded file using the given
    digest algorithm.  The value of digest can be any value accepted by
    hashlib.new.  The default digest used is 'sha256'."""
    ssl_context = ssl.create_default_context(cafile=certifi.where())
    f = urlopen(url, context=ssl_context)
    h = hashlib.new(digest)
    with open(dest, "wb") as out:
        while True:
            data = f.read(4096)
            if data:
                out.write(data)
                h.update(data)
            else:
                break
    if h.hexdigest() != hexhash:
        dest.unlink()
        raise ValueError("Hash of downloaded file does not match expected hash")
