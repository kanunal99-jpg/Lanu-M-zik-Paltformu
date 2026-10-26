"""Offline, no-network tests for persistent OTA signing setup."""
import importlib.util
import pathlib
import subprocess
import tempfile
import unittest
from unittest import mock

source = pathlib.Path(__file__).with_name("bootstrap_ota_signing.py")
spec = importlib.util.spec_from_file_location("bootstrap_ota_signing", source)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class CertificateContinuityTests(unittest.TestCase):
    def test_exact_and_formatted_fingerprints(self):
        digest = "a1" * 32
        self.assertEqual(digest, module.normalize_fingerprint("A1:" * 31 + "A1"))
        self.assertEqual(digest, module.check_matching_identity(digest.upper(), digest))

    def test_mismatch_and_missing_fingerprints_fail_closed(self):
        for value in ("", "a1" * 31, "not a digest", "x" * 64):
            with self.subTest(value=value), self.assertRaises(module.SetupError):
                module.normalize_fingerprint(value)
        with self.assertRaises(module.SetupError):
            module.check_matching_identity("a1" * 32, "b2" * 32)

    def test_absent_or_invalid_pem_refused(self):
        for content in ("", "-----BEGIN CERTIFICATE-----invalid-----END CERTIFICATE-----",
                        "-----BEGIN PRIVATE KEY-----AAA-----END PRIVATE KEY-----"):
            with self.subTest(content=content), self.assertRaises(module.SetupError):
                module.fingerprint_from_pem(content)

    def test_generated_test_keystore_matches_cert_and_private_password(self):
        with tempfile.TemporaryDirectory() as temp:
            key = pathlib.Path(temp) / "test.jks"
            subprocess.run([
                "keytool", "-genkeypair", "-noprompt", "-alias", "upload",
                "-keyalg", "RSA", "-keysize", "2048", "-validity", "2", "-storetype", "JKS",
                "-dname", "CN=LANU OTA test only", "-keystore", str(key),
                "-storepass", "onlyfortesting123", "-keypass", "keyonlyfortesting123"
            ], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            digest = module.certificate_from_existing_keystore(
                key, "upload", "onlyfortesting123", "keyonlyfortesting123"
            )
            self.assertRegex(digest, r"^[0-9a-f]{64}$")
            self.assertEqual(digest, module.check_matching_identity(digest, digest))
            with self.assertRaises(module.SetupError):
                module.certificate_from_existing_keystore(
                    key, "upload", "onlyfortesting123", "wrong-key-pass"
                )
            with self.assertRaises(module.SetupError):
                module.check_matching_identity("ab" * 32, digest)

    def test_only_frozen_repository_is_target_and_private_values_use_stdin(self):
        digest = "12" * 32
        with tempfile.TemporaryDirectory() as temp:
            keystore = pathlib.Path(temp) / "fake.jks"
            keystore.write_bytes(b"private key-like test material")
            command_history = []
            def fake_run(args, *, secret_input=None, env=None):
                command_history.append((args, secret_input))
                if args[:3] == ["gh", "repo", "view"]:
                    return module.REPOSITORY + "\n"
                return ""
            with mock.patch.object(module, "run_checked", side_effect=fake_run):
                module.configure_github(keystore, "upload", "storepw", "keypw", digest)
            self.assertEqual(len(command_history), 7)
            self.assertTrue(all(module.REPOSITORY in cmd for cmd, _ in command_history))
            for cmd, value in command_history[1:5]:
                self.assertEqual(cmd[:3], ["gh", "secret", "set"])
                self.assertIsNotNone(value)
                self.assertNotIn(value, " ".join(cmd))
            for cmd, value in command_history[5:]:
                self.assertEqual(cmd[:3], ["gh", "variable", "set"])
                self.assertIn(digest, cmd)
                self.assertIsNone(value)

    def test_repo_mismatch_blocks_before_any_secrets_written(self):
        with tempfile.TemporaryDirectory() as temp:
            key = pathlib.Path(temp) / "existing.jks"
            key.write_bytes(b"fake")
            with mock.patch.object(module, "run_checked", return_value="other-org/bad-repo") as call:
                with self.assertRaises(module.SetupError):
                    module.configure_github(key, "upload", "store", "key", "1a"*32)
                self.assertEqual(call.call_count, 1)

    def test_main_default_dry_run_and_mismatch_write_nothing(self):
        with tempfile.TemporaryDirectory() as temp:
            key = pathlib.Path(temp) / "existing.jks"
            key.write_bytes(b"0" * 1024)
            cmd = ["--keystore", str(key), "--alias", "upload",
                   "--installed-cert-sha256", "a1"*32]
            with mock.patch.object(module.getpass, "getpass", side_effect=["store", "key"]), \
                 mock.patch.object(module, "certificate_from_existing_keystore", return_value="a1"*32), \
                 mock.patch.object(module, "configure_github") as configure:
                self.assertEqual(0, module.main(cmd))
                configure.assert_not_called()
            with mock.patch.object(module.getpass, "getpass", side_effect=["store", "key"]), \
                 mock.patch.object(module, "certificate_from_existing_keystore", return_value="bb"*32), \
                 mock.patch.object(module, "configure_github") as configure:
                self.assertEqual(1, module.main(cmd + ["--apply"]))
                configure.assert_not_called()


if __name__ == "__main__":
    unittest.main()
