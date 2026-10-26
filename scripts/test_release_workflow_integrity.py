"""Offline contract test for the signed-release path when signing secrets are absent.

This checks syntax and required byte-verification commands. It does NOT prove that
an actual production-signed APK exists or that an in-place device OTA succeeds.
"""
from pathlib import Path
import subprocess
import unittest


class SignedReleaseWorkflowIntegrityTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        workflow = Path(__file__).resolve().parents[1] / ".github/workflows/android.yml"
        lines = workflow.read_text(encoding="utf-8").splitlines()
        step_marker = "      - name: Attach signed APK only after verification"
        cls_step = lines.index(step_marker)
        remaining = lines[cls_step + 1:]
        stop = next(
            (i for i, line in enumerate(remaining) if line.startswith("      - name: ")),
            len(remaining)
        )
        step_lines = remaining[:stop]
        run_line = step_lines.index("        run: |")
        script_lines = step_lines[run_line + 1:]
        cls.script = "\n".join(
            line[10:] if line.startswith("          ") else line
            for line in script_lines
        ) + "\n"

    def test_signed_release_shell_is_syntactically_valid(self):
        result = subprocess.run(
            ["bash", "-n"], input=self.script, text=True, capture_output=True,
            check=False
        )
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_remote_asset_download_and_actual_bytes_are_required(self):
        script = self.script
        self.assertIn("set -euo pipefail", script)
        self.assertIn("gh release download", script)
        self.assertIn("--pattern 'app-release.apk'", script)
        self.assertIn("--pattern 'app-release.apk.sha256'", script)
        self.assertIn('cmp app/build/outputs/apk/release/app-release.apk "', script)
        self.assertIn('cmp app/build/outputs/apk/release/app-release.apk.sha256 "', script)
        self.assertIn('calculated_sha="$(sha256sum "', script)
        self.assertIn('[[ "${published_sha}" == "${calculated_sha}" ]]', script)
        self.assertIn('trap \'rm -rf "${verification_dir}"\' EXIT', script)


if __name__ == "__main__":
    unittest.main()
