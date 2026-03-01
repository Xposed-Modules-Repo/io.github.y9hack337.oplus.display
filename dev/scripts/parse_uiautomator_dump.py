#!/usr/bin/env python3
"""
Parse Android uiautomator XML dump and print matching nodes.

Usage examples:
  python dev/scripts/parse_uiautomator_dump.py window_dump.xml
  python dev/scripts/parse_uiautomator_dump.py window_dump.xml --contains "Cast"
  python dev/scripts/parse_uiautomator_dump.py window_dump.xml --package com.android.systemui --only-clickable
"""

from __future__ import annotations

import argparse
import sys
import xml.etree.ElementTree as ET


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Parse uiautomator XML dump.")
    parser.add_argument("xml_path", help="Path to window_dump.xml")
    parser.add_argument(
        "--contains",
        default="",
        help="Case-insensitive substring to match against text/resource-id/content-desc/class",
    )
    parser.add_argument("--package", dest="package_name", default="", help="Filter by package name")
    parser.add_argument(
        "--only-clickable",
        action="store_true",
        help="Show only nodes with clickable=true",
    )
    return parser.parse_args()


def matched(node: ET.Element, query: str, package_name: str, only_clickable: bool) -> bool:
    attrs = node.attrib

    if package_name and attrs.get("package", "") != package_name:
        return False

    if only_clickable and attrs.get("clickable", "false") != "true":
        return False

    if not query:
        return True

    haystack = " | ".join(
        [
            attrs.get("text", ""),
            attrs.get("resource-id", ""),
            attrs.get("content-desc", ""),
            attrs.get("class", ""),
        ]
    ).lower()
    return query.lower() in haystack


def main() -> int:
    args = parse_args()

    try:
        root = ET.parse(args.xml_path).getroot()
    except Exception as exc:
        print(f"Failed to parse XML: {exc}", file=sys.stderr)
        return 2

    total = 0
    shown = 0

    for node in root.iter("node"):
        total += 1
        if not matched(node, args.contains, args.package_name, args.only_clickable):
            continue

        shown += 1
        attrs = node.attrib
        print(
            f"[{shown:03}] "
            f"text={attrs.get('text', '')!r} "
            f"res_id={attrs.get('resource-id', '')!r} "
            f"class={attrs.get('class', '')!r} "
            f"pkg={attrs.get('package', '')!r} "
            f"clickable={attrs.get('clickable', '')!r} "
            f"enabled={attrs.get('enabled', '')!r} "
            f"bounds={attrs.get('bounds', '')!r}"
        )

    print(f"\nScanned nodes: {total}, matched: {shown}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
