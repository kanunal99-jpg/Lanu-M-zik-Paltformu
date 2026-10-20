#!/usr/bin/env bash
# LANU manufacturer-neutral Android device read-only inventory.
# Never installs/uninstalls packages, clears data, reads IMEI/serial or changes settings.
set -euo pipefail

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  cat <<'HELP'
Usage: bash scripts/android_device_readonly_smoke.sh
  Run with ONE connected authorized Android device, or set ANDROID_SERIAL.
  Requires adb. Outputs only manufacturer/model, API version, display size,
  presence of Google Mobile Services and LANU package installation.
  Does not access personal information, install/uninstall or modify the device.
HELP
  exit 0
fi
if [[ $# -gt 0 ]]; then
  echo "ERROR: Unsupported args; try --help." >&2
  exit 2
fi
command -v adb >/dev/null 2>&1 || { echo "ERROR: adb not installed." >&2; exit 1; }
if [[ -z "${ANDROID_SERIAL:-}" ]]; then
  count="$(adb devices | awk '$2 == "device" { n++ } END { print n+0 }')"
  if [[ "$count" != "1" ]]; then
    echo "ERROR: Connect exactly one authorized device, or select with ANDROID_SERIAL." >&2
    exit 1
  fi
fi
if [[ "$(adb get-state 2>/dev/null)" != "device" ]]; then
  echo "ERROR: Device not authorized or not available." >&2
  exit 1
fi
prop() { adb shell getprop "$1" 2>/dev/null | tr -d '\r\n'; }
manufacturer="$(prop ro.product.manufacturer)"
model="$(prop ro.product.model)"
sdk="$(prop ro.build.version.sdk)"
version="$(prop ro.build.version.release)"
if [[ ! "$sdk" =~ ^[0-9]+$ ]]; then
  echo "ERROR: Could not read Android API level." >&2
  exit 1
fi
echo "Manufacturer: ${manufacturer:-unknown}"
echo "Model: ${model:-unknown}"
echo "Android: ${version:-unknown} | API: $sdk"
if (( sdk < 24 )); then echo "Status: NOT SUPPORTED (app minSdk 24)"
elif (( sdk > 37 )); then echo "Status: Android newer than audited API 37; verify"
else echo "Status: Within API 24-37 test PLAN (not certified)"; fi
size="$(adb shell wm size 2>/dev/null | tr -d '\r' || true)"
density="$(adb shell wm density 2>/dev/null | tr -d '\r' || true)"
echo "Screen: ${size:-unknown}; ${density:-unknown}"
if adb shell pm list packages com.google.android.gms 2>/dev/null | tr -d '\r' | grep -Fxq "package:com.google.android.gms"; then
  echo "Google Mobile Services package: present (does not prove API access)"
else
  echo "Google Mobile Services package: absent/unavailable (test offline-first fallback)"
fi
if adb shell pm path com.aistudio.lanumusic.kpxzqa 2>/dev/null | grep -q '^package:'; then
  echo "LANU package: installed (read-only detection)"
else
  echo "LANU package: not installed; this script will NOT install anything"
fi
echo "Manual acceptance STILL REQUIRED: identity/favorites/playlists, MediaStore, Media3,"
echo "OTA certificate+in-place upgrade, audio focus, background restrictions, resize,"
echo "network/GMS failure, TalkBack, and data comparison across app updates."
