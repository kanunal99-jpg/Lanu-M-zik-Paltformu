#!/usr/bin/env bash
# LANU Android smoke/E2E gate: install, launch, process health, optional in-place OTA upgrade.
set -Eeuo pipefail
BASE_APK="${1:?Usage: android-smoke.sh base.apk [upgrade.apk]}"
UPGRADE_APK="${2:-}"
ADB="${ADB:-adb}"
AAPT="${AAPT:-aapt}"
TIMEOUT="${SMOKE_TIMEOUT:-45}"
fail() { echo "::error::$*" >&2; exit 1; }
command -v "$ADB" >/dev/null || fail "adb missing"
command -v "$AAPT" >/dev/null || fail "aapt missing"
[[ -s "$BASE_APK" ]] || fail "Base APK missing or empty"
"$ADB" wait-for-device
[[ "$("$ADB" shell getprop sys.boot_completed | tr -d '\r')" == 1 ]] || fail "Android device not fully booted"
package_name() { "$AAPT" dump badging "$1" | sed -n "s/^package: name='\([^']*\)'.*/\1/p" | head -1; }
version_code() { "$AAPT" dump badging "$1" | sed -n "s/^package:.*versionCode='\([0-9]*\)'.*/\1/p" | head -1; }
PKG="$(package_name "$BASE_APK")"
[[ -n "$PKG" ]] || fail "Cannot read APK package"
BASE_VERSION="$(version_code "$BASE_APK")"
[[ -n "$BASE_VERSION" ]] || fail "Cannot read base version"
echo "Package: $PKG; base version: $BASE_VERSION"
"$ADB" install -r "$BASE_APK" || fail "Base installation failed"
launch_and_check() {
  "$ADB" shell am force-stop "$PKG"
  "$ADB" logcat -c
  "$ADB" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null || fail "Launcher activity failed"
  local elapsed=0
  while (( elapsed < TIMEOUT )); do
    if "$ADB" shell pidof "$PKG" | grep -Eq '[0-9]'; then break; fi
    sleep 1; elapsed=$((elapsed+1))
  done
  "$ADB" shell pidof "$PKG" | grep -Eq '[0-9]' || fail "App process not alive after launch"
  sleep 3
  "$ADB" shell pidof "$PKG" | grep -Eq '[0-9]' || fail "App crashed shortly after launch"
  if "$ADB" logcat -d -v brief | grep -F "FATAL EXCEPTION" | grep -F "$PKG"; then
    fail "Fatal exception for package"
  fi
  echo "PASS: launch smoke ($PKG)"
}
launch_and_check
if [[ -n "$UPGRADE_APK" ]]; then
  [[ -s "$UPGRADE_APK" ]] || fail "Upgrade APK missing"
  [[ "$(package_name "$UPGRADE_APK")" == "$PKG" ]] || fail "OTA package mismatch"
  NEW_VERSION="$(version_code "$UPGRADE_APK")"
  [[ -n "$NEW_VERSION" ]] || fail "Cannot read upgrade version"
  (( NEW_VERSION >= BASE_VERSION )) || fail "Version downgrade rejected"
  # -r preserves app data. Do not uninstall between versions.
  "$ADB" install -r "$UPGRADE_APK" || fail "In-place upgrade failed (signature/version/storage)"
  INSTALLED="$("$ADB" shell dumpsys package "$PKG" | grep -m1 'versionCode=' | sed -E 's/.*versionCode=([0-9]+).*/\1/')"
  [[ "$INSTALLED" == "$NEW_VERSION" ]] || fail "Installed version mismatch"
  launch_and_check
  echo "PASS: in-place upgrade; app data not explicitly deleted (data integrity requires separate assertions)"
fi
echo "PASS: Android smoke gate"
