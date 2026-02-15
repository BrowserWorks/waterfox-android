#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

REPLACEMENTS = {
    "Firefox": "Waterfox",
    "Mozilla": "BrowserWorks",
}
STRING_PATTERN = re.compile(r"(<string\b[^>]*>)(.*?)(</string>)", re.DOTALL)


def replace_in_strings(text: str) -> str:
    def repl(match: re.Match) -> str:
        inner = match.group(2)
        for src, dst in REPLACEMENTS.items():
            inner = inner.replace(src, dst)
        return f"{match.group(1)}{inner}{match.group(3)}"

    return STRING_PATTERN.sub(repl, text)


def is_values_file(path: Path) -> bool:
    if path.name not in {"strings.xml", "static_strings.xml", "mozonline_strings.xml"}:
        return False
    return any(part.startswith("values") for part in path.parts)


def process_file(path: Path) -> bool:
    if not is_values_file(path):
        return False
    original = path.read_text(encoding="utf-8")
    updated = replace_in_strings(original)
    if updated == original:
        return False
    path.write_text(updated, encoding="utf-8")
    return True


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Replace branding strings in user-facing values."
    )
    parser.add_argument("paths", nargs="+", help="Files or directories to process.")
    args = parser.parse_args()

    changed = 0
    for raw in args.paths:
        path = Path(raw)
        if path.is_dir():
            for file_path in path.rglob("*.xml"):
                if process_file(file_path):
                    changed += 1
        elif path.is_file():
            if process_file(path):
                changed += 1
        else:
            print(f"Skipping missing path: {path}", file=sys.stderr)

    print(f"Updated {changed} file(s).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
