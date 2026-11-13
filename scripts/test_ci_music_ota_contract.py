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

    def test_signer_probe_requests_verified_signature_metadata(self):
        """Regression for Build 633: bare --print-certs omits verified signer count."""
        script = (ROOT / "scripts/ci_music_ota_upgrade_acceptance.sh").read_text()
        self.assertIn('"$apksigner" verify --verbose --print-certs "$1" |', script)
        # Keep the fail-closed production certificate parser; never weaken it
        # to accept a digest without verified v2/v3 signature evidence.
        parser = (ROOT / "scripts/extract_apk_cert_sha256.py").read_text()
        self.assertIn('SIGNER_COUNT_PATTERN', parser)
        self.assertIn('VERIFIED_PATTERN', parser)
        self.assertIn('CERTIFICATE_PATTERN', parser)

    def test_device_is_verified_as_isolated_android14_emulator_before_installing(self):
        body = (ROOT / "scripts/ci_music_ota_upgrade_acceptance.sh").read_text()
        first_install = body.index('adb install -r "$old"')
        for required in ("adb get-serialno", "ro.kernel.qemu", "ro.build.version.sdk",
                         'emulator-[0-9]+', '"34"'):
            self.assertIn(required, body)
            self.assertLess(body.index(required), first_install,
                            "Device safety must be checked before any adb install")

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

    def test_emulator_runner_invokes_acceptance_with_all_six_args_on_one_line(self):
        """Regression for Build 631: multiline YAML script ran bash without six arguments."""
        workflow = (ROOT / ".github/workflows/android.yml").read_text()
        signed = workflow.split("  signed_release:\n", 1)[1]
        emulator = signed.split(
            "      - name: Real signed APK OTA preservation (Android 14 emulator)\n", 1
        )[1].split("      - name: Attach signed APK only after verification\n", 1)[0]
        invocation = [
            line.strip() for line in emulator.splitlines()
            if line.strip().startswith("bash scripts/ci_music_ota_upgrade_acceptance.sh ")
        ]
        self.assertEqual(1, len(invocation))
        command = invocation[0]
        self.assertFalse(command.endswith("\\"),
                         "Emulator runner passes only the first script line to bash")
        for required in (
            '"$RUNNER_TEMP/lanu-ota-baseline/app-release.apk"',
            '"app/build/outputs/apk/release/app-release.apk"',
            '"$RUNNER_TEMP/lanu-ota-baseline-test.apk"',
            '"$RUNNER_TEMP/lanu-ota-new-test.apk"',
            'steps.music_ota_baseline.outputs.version',
            'github.run_number',
        ):
            self.assertIn(required, command)
        self.assertEqual(5, command.count('" "'),
                         "All six OTA arguments must appear on one command line")

    def test_seed_and_verify_target_private_music_library(self):
        source = (ROOT / "app/src/androidTest/java/com/example/RoomActualOtaUpgradeTest.kt").read_text()
        for required in ("LocalAuthBackend", "LocalUserLibraryBackend", "AppDatabase",
                         "FavoriteSongEntity", "PlaylistSongEntity", "DownloadedSongEntity",
                         "LocalSongEntity", "offline_audio", "lanu_ota_ci_fixture",
                         "isolated-emulator-v1"):
            self.assertIn(required, source)


if __name__ == "__main__":
    unittest.main()
