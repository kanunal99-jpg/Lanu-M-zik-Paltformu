"""Static guards for real signed LANU Music Android APK upgrade acceptance.

The workflow/emulator provides the actual device proof; these checks only guard
dangerous workflow regressions in untrusted pull-request CI.
"""
from pathlib import Path
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[1]


class MusicOtaAcceptanceContractTest(unittest.TestCase):
    def test_shell_syntax_is_valid_and_never_uninstalls(self):
        script = ROOT / "scripts/ci_music_ota_upgrade_acceptance.sh"
        checked = subprocess.run(["bash", "-n", str(script)], capture_output=True, text=True)
        self.assertEqual(0, checked.returncode, checked.stderr)
        body = script.read_text()
        for forbidden in ("adb uninstall", "pm clear", "rm -rf /data/data", "INSTALL_FAILED_UPDATE_INCOMPATIBLE || true"):
            self.assertNotIn(forbidden, body)
        for required in ("adb install -r \"$old\"", "adb install -r \"$candidate\"",
                         "seedPreviouslyInstalledStableRelease",
                         "verifySameAccountAndLibraryAfterRealApkUpgrade",
                         "old_signer", "new_signer", "previously published"):
            self.assertIn(required, body)

    def test_signed_release_publish_requires_real_ota_emulator_gate(self):
        workflow = (ROOT / ".github/workflows/android.yml").read_text()
        signed = workflow.split("  signed_release:\n", 1)[1]
        self.assertIn("Real signed APK OTA preservation (Android 14 emulator)", signed)
        self.assertIn("reactivecircus/android-emulator-runner@v2", signed)
        self.assertIn("scripts/ci_music_ota_upgrade_acceptance.sh", signed)
        self.assertLess(signed.index("Real signed APK OTA preservation (Android 14 emulator)"),
                        signed.index("Attach signed APK only after verification"))
        self.assertIn("Build two matching-signer Android instrumentation APKs", signed)
        self.assertIn("app-release.apk.sha256", signed)
        self.assertIn('[[ "${declared}" == "$(sha256sum "${old_apk}"', signed)

    def test_seed_and_verify_target_private_music_library(self):
        source = (ROOT / "app/src/androidTest/java/com/example/RoomActualOtaUpgradeTest.kt").read_text()
        for required in ("LocalAuthBackend", "LocalUserLibraryBackend", "AppDatabase",
                         "FavoriteSongEntity", "PlaylistSongEntity", "DownloadedSongEntity",
                         "LocalSongEntity", "offline_audio", "lanu_ota_ci_fixture",
                         "isolated-emulator-v1"):
            self.assertIn(required, source)


if __name__ == "__main__":
    unittest.main()
