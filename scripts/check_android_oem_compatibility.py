#!/usr/bin/env python3
"""Fast repository-level Android OEM neutrality gate. No device or vendor SDK needed.

Static policy checks are NOT proof of runtime compatibility on any named phone.
"""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent
GRADLE = ROOT / "app/build.gradle.kts"
MANIFEST = ROOT / "app/src/main/AndroidManifest.xml"
ANDROID = "{http://schemas.android.com/apk/res/android}"
BLOCKED_PACKAGES = ("com.samsung.", "com.huawei.", "com.miui.", "com.xiaomi.",
                    "com.sec.android.", "com.google.android.gms")


def check() -> None:
    build = GRADLE.read_text(encoding="utf-8")
    sdk_min = re.search(r"\bminSdk\s*=\s*(\d+)\b", build)
    sdk_target = re.search(r"\btargetSdk\s*=\s*(\d+)\b", build)
    sdk_compile = re.search(r"\bcompileSdk\s*\{\s*version\s*=\s*release\((\d+)\)", build)
    if not sdk_min or int(sdk_min.group(1)) != 24:
        raise ValueError("Unexpected minimum API: vendor compatibility baseline is API 24")
    if not sdk_target or int(sdk_target.group(1)) < 36:
        raise ValueError("Target API must be >= 36 (2026 baseline)")
    if not sdk_compile or int(sdk_compile.group(1)) < 36:
        raise ValueError("Compile SDK must be >= 36")
    if not re.search(r'applicationId\s*=\s*"com\.aistudio\.lanumusic\.kpxzqa"', build):
        raise ValueError("Package ID changed: in-place APK updates would lose identity")
    root = ET.parse(MANIFEST).getroot()
    if root.tag != "manifest":
        raise ValueError("Invalid manifest root")
    for feature in root.findall("uses-feature"):
        if feature.get(ANDROID + "required", "true") == "true":
            raise ValueError("Required hardware/software feature excludes some Android OEMs: " +
                             str(feature.attrib))
    for element in root.iter():
        for value in element.attrib.values():
            lower = value.lower()
            if any(vendor in lower for vendor in BLOCKED_PACKAGES):
                raise ValueError("Vendor/Google-services-specific manifest requirement: " + value)
        if element.tag in {"uses-sdk", "supports-screens"}:
            if element.get(ANDROID + "maxSdkVersion") is not None:
                raise ValueError("Maximum platform version restriction detected")
    for activity in root.iter("activity"):
        if activity.get(ANDROID + "screenOrientation") is not None:
            raise ValueError("Orientation lock conflicts with tablet/foldable acceptance")
        if activity.get(ANDROID + "resizeableActivity", "true") == "false":
            raise ValueError("Activity resizing disabled")
    print("PASS: Android OEM-neutral manifest; API 24 min, target/compile >= 36; "
          "stable package identity and no mandatory vendor services or hardware.")
    print("NOTE: Emulator + Samsung/Xiaomi/Huawei/AOSP + tablets and OTA checks still required.")


if __name__ == "__main__":
    try:
        check()
    except (ValueError, ET.ParseError, OSError) as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        sys.exit(1)
