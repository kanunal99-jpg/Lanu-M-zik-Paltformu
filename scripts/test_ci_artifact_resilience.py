"""CI contract: an optional diagnostic upload must not override release safety gates."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = (ROOT / ".github/workflows/android.yml").read_text(encoding="utf-8")


def step_section(name: str) -> str:
    marker = "      - name: " + name + "\n"
    if WORKFLOW.count(marker) != 1:
        raise AssertionError("Missing/duplicate CI step: " + name)
    remaining = WORKFLOW.split(marker, 1)[1]
    return remaining.split("      - name:", 1)[0]


class DiagnosticArtifactResilienceTests(unittest.TestCase):
    def test_actual_lint_gate_cannot_be_soft_failed(self):
        lint = step_section("Android Lint quality gate")
        self.assertIn("gradle lintDebug --stacktrace", lint)
        self.assertIn('exit "${outcome}"', lint)
        self.assertNotIn("continue-on-error", lint)

    def test_noncritical_report_archive_does_not_block_build(self):
        report = step_section("Archive Lint findings")
        self.assertIn("if: always()", report)
        self.assertIn("continue-on-error: true", report)
        self.assertIn("actions/upload-artifact", report)

    def test_actual_debug_apk_artifact_and_production_verification_stay_required(self):
        apk = step_section("Upload Debug APK Artifact")
        self.assertNotIn("continue-on-error", apk)
        signed = WORKFLOW.split("  signed_release:\n", 1)[1]
        self.assertIn('python3 scripts/extract_apk_cert_sha256.py', signed)
        self.assertIn('gh release download', signed)
        self.assertIn('--prerelease=false --latest', signed)


if __name__ == "__main__":
    unittest.main()
