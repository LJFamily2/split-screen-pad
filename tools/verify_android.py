#!/usr/bin/env python3
"""
Static checks for the Android module.

The Android SDK is not available in every environment (CI builds the real APK),
so this script catches the mistakes that a missing compile would otherwise hide:

  1. malformed XML anywhere under res/ or in the manifest
  2. @color / @string / @drawable / @style / @mipmap / @xml references that do
     not resolve
  3. R.<type>.<name> references in Kotlin that do not exist
  4. findViewById<Type>(R.id.x) where the layout declares an incompatible widget
  5. ids declared in a layout that no Kotlin file ever binds — the exact bug that
     left the old home screen's buttons dead

Run: python3 tools/verify_android.py
"""

from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "android/app/src/main/res"
JAVA = ROOT / "android/app/src/main/java"
MANIFEST = ROOT / "android/app/src/main/AndroidManifest.xml"

ANDROID_NS = "{http://schemas.android.com/apk/res/android}"

errors: list[str] = []
warnings: list[str] = []

# Widget inheritance, enough to validate the casts this project actually makes.
SUPERTYPES = {
    "View": [],
    "TextView": ["View"],
    "EditText": ["TextView", "View"],
    "Button": ["TextView", "View"],
    "ToggleButton": ["Button", "TextView", "View"],
    "ImageView": ["View"],
    "ImageButton": ["ImageView", "View"],
    "ProgressBar": ["View"],
    "ViewGroup": ["View"],
    "FrameLayout": ["ViewGroup", "View"],
    "LinearLayout": ["ViewGroup", "View"],
    "RelativeLayout": ["ViewGroup", "View"],
    "ScrollView": ["FrameLayout", "ViewGroup", "View"],
    "HorizontalScrollView": ["FrameLayout", "ViewGroup", "View"],
    "WebView": ["ViewGroup", "View"],
    "RecyclerView": ["ViewGroup", "View"],
    "ListView": ["ViewGroup", "View"],
}


def simple_tag(tag: str) -> str:
    return tag.rsplit(".", 1)[-1]


def is_a(actual: str, expected: str) -> bool:
    if actual == expected:
        return True
    return expected in SUPERTYPES.get(actual, [])


def parse_xml(path: Path):
    try:
        return ET.parse(path).getroot()
    except ET.ParseError as exc:
        errors.append(f"{path.relative_to(ROOT)}: malformed XML — {exc}")
        return None


def collect_resources():
    """name -> set of resource types defined in res/values and res/<dir>/."""
    defined = defaultdict(set)
    for values_file in RES.glob("values*/*.xml"):
        root = parse_xml(values_file)
        if root is None:
            continue
        for child in root:
            if child.tag is ET.Comment:
                continue
            name = child.get("name")
            if not name:
                continue
            kind = "string" if child.tag == "string" else child.tag
            if child.tag == "item":
                kind = child.get("type", "item")
            defined[name].add(kind)
    for folder in RES.iterdir():
        if not folder.is_dir():
            continue
        kind = folder.name.split("-")[0]
        if kind in ("values",):
            continue
        for f in folder.iterdir():
            defined[f.stem].add(kind)
    return defined


def collect_layout_ids():
    """id -> {layout file: widget tag}"""
    ids = defaultdict(dict)
    for layout in sorted(RES.glob("layout*/*.xml")):
        root = parse_xml(layout)
        if root is None:
            continue
        for element in root.iter():
            if element.tag is ET.Comment:
                continue
            raw = element.get(f"{ANDROID_NS}id")
            if not raw:
                continue
            name = raw.split("/")[-1]
            ids[name][layout.name] = simple_tag(element.tag)
    return ids


REF = re.compile(r'"@(?!\+)(?:(\w+):)?(\w+)/([\w.]+)"')

# Styles inherited from the framework / Material Components are resolved by the
# build, not declared in this project.
LIBRARY_STYLE_PREFIXES = (
    "Widget.",
    "Theme.",
    "ThemeOverlay.",
    "TextAppearance.",
    "ShapeAppearance.",
    "Base.",
    "Platform.",
    "MaterialAlertDialog.",
    "Animation.",
    "Widget.AppCompat",
)


def check_xml_references(defined):
    files = list(RES.rglob("*.xml")) + [MANIFEST]
    for path in files:
        text = path.read_text(encoding="utf-8")
        for pkg, kind, name in REF.findall(text):
            if pkg == "android":
                continue
            if kind == "id":
                continue  # ids are resolved separately
            if kind not in defined.get(name, set()):
                if kind == "style" and name.startswith(LIBRARY_STYLE_PREFIXES):
                    continue
                errors.append(
                    f"{path.relative_to(ROOT)}: @{kind}/{name} does not resolve"
                )


# `android.R.string.ok` is the framework's, not ours — only match a bare `R.`.
R_REF = re.compile(r"(?<![\w.])R\.(\w+)\.(\w+)\b")
FIND_VIEW = re.compile(r"findViewById<(\w+)>\(R\.id\.(\w+)\)")
TYPED_FIND = re.compile(r"val\s+\w+\s*:\s*(\w+)\s*=\s*\w*\.?findViewById\(R\.id\.(\w+)\)")


def check_kotlin(defined, layout_ids):
    used_ids = set()
    for src in sorted(JAVA.rglob("*.kt")):
        text = src.read_text(encoding="utf-8")
        rel = src.relative_to(ROOT)

        for kind, name in R_REF.findall(text):
            if kind == "id":
                used_ids.add(name)
                if name not in layout_ids:
                    errors.append(f"{rel}: R.id.{name} is not declared in any layout")
            elif kind in ("layout", "string", "drawable", "color", "mipmap", "style", "xml"):
                if kind not in defined.get(name, set()):
                    errors.append(f"{rel}: R.{kind}.{name} does not resolve")

        for expected, ident in list(FIND_VIEW.findall(text)) + list(TYPED_FIND.findall(text)):
            for layout, actual in layout_ids.get(ident, {}).items():
                if not is_a(actual, expected):
                    errors.append(
                        f"{rel}: findViewById<{expected}>(R.id.{ident}) but "
                        f"{layout} declares a {actual}"
                    )
    return used_ids


def check_orphan_ids(layout_ids, used_ids):
    """Ids sitting in a layout that nothing ever binds are usually dead UI."""
    ignore_prefixes = ("divider_grip", "floating_root", "bubble_root", "main_root", "top_toolbar")
    for name, layouts in sorted(layout_ids.items()):
        if name in used_ids or name.startswith(ignore_prefixes):
            continue
        where = ", ".join(sorted(layouts))
        warnings.append(f"{where}: @+id/{name} is never used from Kotlin")


def main() -> int:
    if not RES.is_dir():
        print(f"res/ not found at {RES}", file=sys.stderr)
        return 2

    defined = collect_resources()
    layout_ids = collect_layout_ids()
    check_xml_references(defined)
    used_ids = check_kotlin(defined, layout_ids)
    check_orphan_ids(layout_ids, used_ids)

    for warning in warnings:
        print(f"WARN  {warning}")
    for error in errors:
        print(f"ERROR {error}")

    print(
        f"\n{len(layout_ids)} layout ids · {len(defined)} resources · "
        f"{len(errors)} errors · {len(warnings)} warnings"
    )
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
