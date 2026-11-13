#!/usr/bin/env bash
# Guarded Android 14 CI acceptance: install previous RELEASE, seed real Room and
# local library data using same-signed instrumentation, then adb install -r new
# RELEASE and verify unchanged data. Never erase local app data or expose secrets.
set -Eeuo pipefail
if (( $# != 6 )); then
  echo "usage: ci_music_ota_upgrade_acceptance.sh OLD.apk NEW.apk OLD_TEST.apk NEW_TEST.apk OLD_CODE NEW_CODE" >&2
  exit 2
fi
old="$1"; candidate="$2"; old_test="$3"; new_test="$4"; old_code="$5"; new_code="$6"
for artifact in "$old" "$candidate" "$old_test" "$new_test"; do
  [[ -s "$artifact" ]] || { echo "::error::Missing OTA acceptance APK" >&2; exit 1; }
done
[[ "$old_code" =~ ^[0-9]+$ && "$new_code" =~ ^[0-9]+$ ]] || exit 2
(( new_code > old_code )) || { echo "::error::OTA version order invalid"; exit 1; }

sdk="${ANDROID_HOME:?}/build-tools"
aapt="$(find "$sdk" -mindepth 2 -maxdepth 2 -type f -name aapt | sort -V | tail -n 1)"
apksigner="$(find "$sdk" -mindepth 2 -maxdepth 2 -type f -name apksigner | sort -V | tail -n 1)"
[[ -x "$aapt" && -x "$apksigner" ]] || exit 1
package="com.aistudio.lanumusic.kpxzqa"
runner="$package.test/androidx.test.runner.AndroidJUnitRunner"

pkg() { "$aapt" dump badging "$1" | sed -nE "s/^package: name='([^']+)'.*/\\1/p" | head -n 1; }
version() { "$aapt" dump badging "$1" | sed -nE "s/^package:.*versionCode='([0-9]+)'.*/\\1/p" | head -n 1; }
signer() {
  "$apksigner" verify --print-certs "$1" |
    python3 scripts/extract_apk_cert_sha256.py
}
[[ "$(pkg "$old")" == "$package" && "$(pkg "$candidate")" == "$package" ]] ||
  { echo "::error::OTA app identity mismatch"; exit 1; }
[[ "$(pkg "$old_test")" == "$package.test" && "$(pkg "$new_test")" == "$package.test" ]] ||
  { echo "::error::OTA instrumentation identity mismatch"; exit 1; }
[[ "$(version "$old")" == "$old_code" && "$(version "$candidate")" == "$new_code" ]] ||
  { echo "::error::OTA release version mismatch"; exit 1; }

old_signer="$(signer "$old")"; new_signer="$(signer "$candidate")"
old_test_signer="$(signer "$old_test")"; new_test_signer="$(signer "$new_test")"
[[ "$old_signer" =~ ^[a-f0-9]{64}$ ]] || exit 1
if [[ "$old_signer" != "$new_signer" || "$old_signer" != "$old_test_signer" ||
      "$old_signer" != "$new_test_signer" ]]; then
  echo "::error::OTA certificate mismatch: existing app preserved" >&2
  exit 1
fi

probe() {
  local phase="$1" code="$2" method="$3" output rc=0
  output="$(adb shell am instrument -w -r \
    -e lanu_ota_ci isolated-emulator-v1 \
    -e lanu_ota_phase "$phase" \
    -e lanu_expected_version "$code" \
    -e class "com.example.RoomActualOtaUpgradeTest#$method" \
    "$runner" 2>&1)" || rc=$?
  if (( rc != 0 )) || ! grep -Fq 'OK (1 test)' <<<"$output" ||
      grep -Eq 'FAILURES|INSTRUMENTATION_FAILED|INSTRUMENTATION_CODE: 0' <<<"$output"; then
    echo "::error::Signed OTA Android data preservation failed in $phase phase" >&2
    # Never print instrumentation output: it can contain private app bytes.
    exit 1
  fi
  echo "OTA emulator $phase: passed."
}

echo "OTA: installing previously published signed RELEASE (no data wipe)"
adb install -r "$old" > /dev/null
adb install -r -t "$old_test" > /dev/null
probe seed "$old_code" seedPreviouslyInstalledStableRelease
echo "OTA: performing exact signed APK replacement with adb install -r"
adb install -r "$candidate" > /dev/null
adb install -r -t "$new_test" > /dev/null
probe verify "$new_code" verifySameAccountAndLibraryAfterRealApkUpgrade
echo "OTA: local identity, favorites, playlists, history, Room, downloads and offline file preserved."
