import importlib.util
from pathlib import Path
import unittest

path = Path(__file__).with_name("verify_installed_signing_certificate.py")
spec = importlib.util.spec_from_file_location("signing_gate", path)
gate = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gate)


class SigningGateTests(unittest.TestCase):
    def test_equal_certificates(self):
        fingerprint = "a1" * 32
        gate.verify(fingerprint.upper(), ":".join(["A1"] * 32), fingerprint)

    def test_missing_installed_certificate_fails_closed(self):
        with self.assertRaises(ValueError):
            gate.verify("", "a1" * 32, "a1" * 32)

    def test_mismatched_installed_certificate_fails_closed(self):
        with self.assertRaises(ValueError):
            gate.verify("b2" * 32, "a1" * 32, "a1" * 32)

    def test_mismatched_apk_certificate_fails_closed(self):
        with self.assertRaises(ValueError):
            gate.verify("a1" * 32, "a1" * 32, "b2" * 32)

    def test_malformed_certificate_fails_closed(self):
        with self.assertRaises(ValueError):
            gate.verify("not-a-fingerprint", "a1" * 32, "a1" * 32)


if __name__ == "__main__":
    unittest.main()
