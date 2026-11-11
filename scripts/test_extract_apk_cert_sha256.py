"""Offline contract tests for Android SDK apksigner v2/v3 signer output."""
import importlib.util
from pathlib import Path
import subprocess
import sys
import unittest


SOURCE = Path(__file__).with_name("extract_apk_cert_sha256.py")
spec = importlib.util.spec_from_file_location("extract_apk_cert_sha256", SOURCE)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
SHA = "dc949667ed0219ac64e0870b2dbef644ac94a08ca6ee6b13fe78bf7904f5d822"
ALT = "aa" * 32


def sample(*lines):
    return "\n".join([
        "Verifies",
        "Verified using v1 scheme (JAR signing): false",
        "Verified using v2 scheme (APK Signature Scheme v2): true",
        "Verified using v3 scheme (APK Signature Scheme v3): false",
        "Number of signers: 1",
        *lines,
    ]) + "\n"


class ApkCertOutputParsingTests(unittest.TestCase):
    def test_real_world_new_apksigner_v2_output(self):
        actual = sample(
            "V2 Signer: certificate DN: CN=LANU Music Permanent OTA Signer, O=LANU, C=TR",
            "V2 Signer: certificate SHA-256 digest: " + SHA,
            "V2 Signer: certificate SHA-1 digest: dec05f1588a9bcaccfc9ba136a043531e4b0bce9",
            "V2 Signer: public key SHA-256 digest: " + ALT,
        )
        self.assertEqual(SHA, module.extract_single_signing_certificate(actual))

    def test_legacy_signer_number_format(self):
        text = sample("Signer #1 certificate SHA-256 digest: " + SHA)
        self.assertEqual(SHA, module.extract_single_signing_certificate(text))

    def test_multiple_algorithms_agree(self):
        text = sample(
            "V2 Signer: certificate SHA-256 digest: " + SHA,
            "V3 Signer: certificate SHA-256 digest: " + SHA.upper(),
        )
        self.assertEqual(SHA, module.extract_single_signing_certificate(text))

    def test_v3_only_also_verifies(self):
        text = "\n".join([
            "Verified using v2 scheme (APK Signature Scheme v2): false",
            "Verified using v3 scheme (APK Signature Scheme v3): true",
            "Number of signers: 1",
            "V3 Signer: certificate SHA-256 digest: " + SHA,
        ])
        self.assertEqual(SHA, module.extract_single_signing_certificate(text))

    def test_invalid_or_ambiguous_certificate_fails_closed(self):
        variants = (
            "",
            sample("V2 Signer: public key SHA-256 digest: " + SHA),
            sample("V2 Signer: certificate SHA-256 digest: bad"),
            sample("V2 Signer: certificate SHA-256 digest: " + SHA).replace("Number of signers: 1", "Number of signers: 2"),
            sample("V2 Signer: certificate SHA-256 digest: " + SHA).replace("Verified using v2 scheme (APK Signature Scheme v2): true", "Verified using v2 scheme (APK Signature Scheme v2): false"),
            sample("V2 Signer: certificate SHA-256 digest: " + SHA).replace("Number of signers: 1", ""),
            sample("V2 Signer: certificate SHA-256 digest: " + SHA, "V3 Signer: certificate SHA-256 digest: " + ALT),
        )
        for variant in variants:
            with self.subTest(variant=variant[:120]), self.assertRaises(module.CertificateParseError):
                module.extract_single_signing_certificate(variant)

    def test_cli_success_and_error_exit_status(self):
        ok = subprocess.run([sys.executable, str(SOURCE)], input=sample(
            "V2 Signer: certificate SHA-256 digest: " + SHA,
        ), text=True, capture_output=True)
        self.assertEqual(0, ok.returncode, ok.stderr)
        self.assertEqual(SHA, ok.stdout.strip())
        bad = subprocess.run([sys.executable, str(SOURCE)], input="malformed",
                             text=True, capture_output=True)
        self.assertNotEqual(0, bad.returncode)
        self.assertEqual("", bad.stdout)


if __name__ == "__main__":
    unittest.main()
