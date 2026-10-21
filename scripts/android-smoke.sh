#!/usr/bin/env bash
# LANU Android test smoke. Default is READ-ONLY preflight; mutation needs a
# named dedicated device and an exact consent token. NEVER uninstall/clear data.
set -Eeuo pipefail
usage() {
  cat <<'USAGE'
Usage: android-smoke.sh BASE.apk [UPGRADE.apk]
Default mode: LANU_SMOKE_MODE=preflight (read-only, no install/launch).
To run only on a FRESH dedicated emulator/test device:
  LANU_SMOKE_MODE=install
  LANU_SMOKE_DEVICE_SERIAL=<adb serial>
  LANU_SMOKE_EXPECTED_PACKAGE=<approved package>
  LANU_SMOKE_ENABLE_INSTALL=I_APPROVE_DEDICATED_TEST_DEVICE
For a physical DEDICATED test device also explicitly set:
  LANU_SMOKE_ALLOW_PHYSICAL_DEVICE=I_APPROVE_PHYSICAL_TEST_DEVICE
If the package is already installed, the script always refuses install.
Use the separate read-only device script to inspect a personal phone.
This checks process health only, NOT playlist or Room data retention.
USAGE
}
if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then usage; exit 0; fi
[[ $# -ge 1 && $# -le 2 ]] || { usage >&2; exit 2; }
BASE_APK="$1"
UPGRADE_APK="${2:-}"
MODE="${LANU_SMOKE_MODE:-preflight}"
ADB="${ADB:-adb}"
AAPT="${AAPT:-aapt}"
APKSIGNER="${APKSIGNER:-apksigner}"
SERIAL="${LANU_SMOKE_DEVICE_SERIAL:-}"
fail() { echo "ERROR: $*" >&2; exit 1; }
[[ "$MODE" == "preflight" || "$MODE" == "install" ]] || fail "Unknown LANU_SMOKE_MODE"
[[ "$SERIAL" =~ ^[A-Za-z0-9._:-]+$ ]] || fail "Explicit LANU_SMOKE_DEVICE_SERIAL required"
for tool in "$ADB" "$AAPT" "$APKSIGNER"; do
  command -v "$tool" >/dev/null || fail "Missing tool: $tool"
done
[[ -s "$BASE_APK" ]] || fail "Base APK missing or empty"
[[ -z "$UPGRADE_APK" || -s "$UPGRADE_APK" ]] || fail "Upgrade APK missing or empty"
adb_device() { "$ADB" -s "$SERIAL" "$@"; }
[[ "$(adb_device get-serialno | tr -d '\r')" == "$SERIAL" ]] || fail "ADB target does not match explicit serial"
[[ "$(adb_device shell getprop sys.boot_completed | tr -d '\r')" == "1" ]] || fail "Device not booted"
package_name() {
  "$AAPT" dump badging "$1" |
    sed -n "s/^package: name='\\([^']*\\)'.*/\\1/p" | head -1
}
version_code() {
  "$AAPT" dump badging "$1" |
    sed -n "s/^package:.*versionCode='\\([0-9]*\\)'.*/\\1/p" | head -1
}
certificate() {
  "$APKSIGNER" verify --print-certs "$1" |
    awk -F': ' '/Signer #1 certificate SHA-256 digest:/ {print tolower($2)}'
}
PKG="$(package_name "$BASE_APK")"
BASE_VERSION="$(version_code "$BASE_APK")"
[[ "$PKG" =~ ^[A-Za-z][A-Za-z0-9_.]+$ && "$BASE_VERSION" =~ ^[0-9]+$ ]] || fail "Invalid base APK package/version"
[[ "$PKG" == "${LANU_SMOKE_EXPECTED_PACKAGE:-}" ]] || fail "Package not explicitly approved"
BASE_CERT="$(certificate "$BASE_APK")"
[[ "$BASE_CERT" =~ ^[a-f0-9]{64}$ ]] || fail "Base APK signature missing or not unique"
if [[ -n "$UPGRADE_APK" ]]; then
  [[ "$(package_name "$UPGRADE_APK")" == "$PKG" ]] || fail "Upgrade package differs"
  NEW_VERSION="$(version_code "$UPGRADE_APK")"
  [[ "$NEW_VERSION" =~ ^[0-9]+$ ]] || fail "Invalid upgrade version"
  (( 10#$NEW_VERSION > 10#$BASE_VERSION )) || fail "Upgrade version must strictly increase"
  NEW_CERT="$(certificate "$UPGRADE_APK")"
  [[ "$NEW_CERT" =~ ^[a-f0-9]{64}$ && "$NEW_CERT" == "$BASE_CERT" ]] ||
    fail "APK signer mismatch: do not install or bypass certificate checks"
fi
# Read-only preflight is the default, even if a real user phone is attached.
if [[ "$MODE" == "preflight" ]]; then
  echo "PASS: read-only APK, device, version and signature preflight for $PKG"
  echo "NO INSTALL performed. User data unchanged."
  exit 0
fi
[[ "${LANU_SMOKE_ENABLE_INSTALL:-}" == "I_APPROVE_DEDICATED_TEST_DEVICE" ]] ||
  fail "Refusing install without explicit dedicated-test-device approval"
if [[ "$(adb_device shell getprop ro.kernel.qemu | tr -d '\r')" != "1" ]]; then
  [[ "${LANU_SMOKE_ALLOW_PHYSICAL_DEVICE:-}" == "I_APPROVE_PHYSICAL_TEST_DEVICE" ]] ||
    fail "Physical devices require a separate explicit approval"
fi
# Protect an installed account/library: this suite must begin on a fresh test
# device. A real user installation is NEVER replaced by the smoke script.
INSTALLED_PATH="$(adb_device shell pm path "$PKG" 2>/dev/null || true)"
[[ -z "$INSTALLED_PATH" ]] || fail "Package already installed. Refusing to overwrite an existing library."
echo "Dedicated-device installation approved for $SERIAL, package $PKG"
adb_device install -r "$BASE_APK" || fail "Base install failed; no recovery uninstall is attempted"
launch_check() {
  adb_device shell am force-stop "$PKG"
  adb_device shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null ||
    fail "Launch failed"
  local elapsed=0 max_wait="${SMOKE_TIMEOUT:-45}"
  [[ "$max_wait" =~ ^[0-9]+$ ]] || fail "Invalid SMOKE_TIMEOUT"
  while (( elapsed < max_wait )); do
    if adb_device shell pidof "$PKG" | grep -Eq '[0-9]'; then break; fi
    sleep 1; elapsed=$((elapsed+1))
  done
  adb_device shell pidof "$PKG" | grep -Eq '[0-9]' || fail "App not alive"
  sleep 3
  adb_device shell pidof "$PKG" | grep -Eq '[0-9]' || fail "App terminated after launch"
  echo "PASS: process smoke for $PKG (not data-retention proof)"
}
launch_check
if [[ -n "$UPGRADE_APK" ]]; then
  # Only the APK freshly installed above may be upgraded here.
  adb_device install -r "$UPGRADE_APK" || fail "In-place test upgrade failed; never uninstall"
  INSTALLED="$(adb_device shell dumpsys package "$PKG" |
    sed -n 's/.*versionCode=\([0-9]*\).*/\1/p' | head -1)"
  [[ "$INSTALLED" == "$NEW_VERSION" ]] || fail "Installed version does not match"
  launch_check
  echo "PASS: test-device upgrade; data integrity still requires separate assertions"
fi
echo "PASS: dedicated-device Android smoke"
