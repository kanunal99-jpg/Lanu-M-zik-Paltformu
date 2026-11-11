"""Offline safety tests for new-install-only persistent signer bootstrap."""
import importlib.util
import os
import shutil
from pathlib import Path
import sys
import tempfile
import unittest
from unittest import mock

SCRIPTS = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPTS))
spec = importlib.util.spec_from_file_location(
    "bootstrap_fresh_ota_signing", SCRIPTS / "bootstrap_fresh_ota_signing.py"
)
fresh = importlib.util.module_from_spec(spec)
spec.loader.exec_module(fresh)


class FreshSignerSafetyTests(unittest.TestCase):
    def test_never_store_keystore_inside_git_checkout(self):
        with self.assertRaises(fresh.SetupError):
            fresh.safe_private_directory(fresh.REPO_ROOT / "keys")
        with self.assertRaises(fresh.SetupError):
            fresh.safe_private_directory(Path("relative"))
        self.assertEqual(
            Path(tempfile.gettempdir()) / "lanu-safe-ota",
            fresh.safe_private_directory(Path(tempfile.gettempdir()) / "lanu-safe-ota"),
        )

    def test_init_is_local_atomic_non_overwriting_and_private(self):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp) / "new-ota"
            fingerprint = "cd" * 32
            def fake_run(args, *, env=None, secret_input=None):
                self.assertEqual(args[:3], ["keytool", "-genkeypair", "-noprompt"])
                self.assertIn("-storepass:env", args)
                self.assertIn("-keypass:env", args)
                self.assertNotIn(env["LANU_FRESH_OTA_STORE_PASSWORD"], " ".join(args))
                (directory / fresh.KEYSTORE_FILENAME).write_bytes(b"test-only fake keystore")
                return ""
            with mock.patch.object(fresh, "run_checked", side_effect=fake_run), \
                 mock.patch.object(fresh, "certificate_from_existing_keystore", return_value=fingerprint):
                result = fresh.initialize_new_signer(directory)
                self.assertEqual(result["fingerprint"], fingerprint)
                self.assertEqual(result["alias"], fresh.ALIAS)
                self.assertEqual(result["repository"], fresh.REPOSITORY)
                self.assertTrue(len(result["password"]) >= 40)
                self.assertEqual(0o700, directory.stat().st_mode & 0o777)
                for filename in (fresh.KEYSTORE_FILENAME, fresh.RECOVERY_FILENAME):
                    self.assertEqual(0o600, (directory / filename).stat().st_mode & 0o777)
                with self.assertRaises(fresh.SetupError):
                    fresh.initialize_new_signer(directory)
                self.assertEqual(result, fresh.verify_owned_material(directory))

    @unittest.skipUnless(shutil.which("keytool"), "JDK keytool required")
    def test_generated_real_pkcs12_keystore_is_recoverable_locally(self):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp) / "real-new-signing"
            created = fresh.initialize_new_signer(directory)
            self.assertRegex(created["fingerprint"], r"^[a-f0-9]{64}$")
            recovered = fresh.verify_owned_material(directory)
            self.assertEqual(created["fingerprint"], recovered["fingerprint"])
            self.assertEqual(created["alias"], recovered["alias"])
            self.assertEqual(created["password"], recovered["password"])

    def test_public_fingerprint_verified_before_any_secret_upload(self):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            (directory / fresh.KEYSTORE_FILENAME).write_bytes(b"placeholder")
            calls = []
            def spy(args, *, env=None, secret_input=None):
                calls.append((args, secret_input))
                if args[:3] == ["gh", "repo", "view"]:
                    return fresh.REPOSITORY + "\n"
                return ""
            manifest = {"alias": fresh.ALIAS, "password": "private", "fingerprint": "ab"*32}
            with mock.patch.object(fresh, "run_checked", side_effect=spy):
                fresh.enroll_github(directory, manifest)
            self.assertEqual(len(calls), 7)
            self.assertEqual(calls[0][0][:3], ["gh", "repo", "view"])
            self.assertIn("NEW_INSTALL_ONLY", calls[1][0])
            self.assertEqual(calls[1][0][0:3], ["gh", "variable", "set"])
            self.assertTrue(all(c[0][:3] == ["gh", "secret", "set"] for c in calls[2:6]))
            self.assertTrue(all(c[1] is not None for c in calls[2:6]))
            self.assertEqual(calls[6][0][:3], ["gh", "variable", "set"])
            self.assertIn("LANU_SIGNING_CERT_SHA256", calls[6][0])
            self.assertFalse(any("LANU_INSTALLED_APP_CERT_SHA256" in " ".join(c[0]) for c in calls))
            self.assertFalse(any("private" in " ".join(c[0]) for c in calls))

    def test_repo_mismatch_never_uploads_secrets_or_channel(self):
        with tempfile.TemporaryDirectory() as temp, \
             mock.patch.object(fresh, "run_checked", return_value="other/bad") as call:
            with self.assertRaises(fresh.SetupError):
                fresh.enroll_github(Path(temp), {
                    "alias": fresh.ALIAS, "password": "password", "fingerprint": "aa"*32
                })
            self.assertEqual(call.call_count, 1)

    def test_never_create_without_interactive_acknowledgement(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "new"
            with mock.patch("builtins.input", return_value="no"):
                self.assertEqual(1, fresh.main(["--private-dir", str(path), "--init"]))
                self.assertFalse(path.exists())

    def test_recovery_file_and_keystore_must_be_private(self):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp) / "vault"
            directory.mkdir(mode=0o700)
            for name in (fresh.KEYSTORE_FILENAME, fresh.RECOVERY_FILENAME):
                (directory / name).write_text("test", encoding="utf-8")
                os.chmod(directory / name, 0o600)
            with self.assertRaises(ValueError):
                fresh.verify_owned_material(directory)
            os.chmod(directory / fresh.RECOVERY_FILENAME, 0o644)
            with self.assertRaises(fresh.SetupError):
                fresh.verify_owned_material(directory)


if __name__ == "__main__":
    unittest.main()
