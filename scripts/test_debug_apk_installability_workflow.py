#!/usr/bin/env python3
"""Offline CI contract for the exact APK users download from GitHub Releases.

This protects the release against publishing an unverified/corrupt/wrong-package
debug APK; it is NOT proof of permission to sideload on a specific Android phone.
"""
from pathlib import Path
import subprocess
import unittest


class DebugApkInstallabilityWorkflowTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        workflow = (Path(__file__).resolve().parents[1] /
                    ".github/workflows/android.yml").read_text(encoding="utf-8")
        marker = "      - name: Verify debug APK installability before publishing\n"
        cls.block = workflow.split(marker, 1)[1].split("      - name:", 1)[0]
        cls.script = "\n".join(
            line[10:] if line.startswith("          ") else line
            for line in cls.block.split("        run: |\n", 1)[1].splitlines()
        ) + "\n"
        cls.workflow = workflow

    def test_shell_syntax(self):
        result = subprocess.run(
            ["bash", "-n"], input=self.script, text=True, capture_output=True,
            check=False
        )
        self.assertEqual(0, result.returncode, result.stderr)

    def test_actual_published_apk_is_checked_before_sha_and_upload(self):
        workflow = self.workflow
        guard = workflow.index("      - name: Verify debug APK installability before publishing")
        sha = workflow.index("      - name: Generate APK SHA-256")
        upload = workflow.index("      - name: Upload Debug APK Artifact")
        self.assertLess(guard, sha)
        self.assertLess(sha, upload)
        self.assertIn('apk="app/build/outputs/apk/debug/app-debug.apk"', self.script)
        self.assertIn('unzip -t "${apk}"', self.script)
        self.assertIn('verify --verbose --print-certs --min-sdk-version 24', self.script)
        self.assertIn('verify --verbose --min-sdk-version 36', self.script)
        self.assertIn("com.aistudio.lanumusic.kpxzqa", self.script)
        self.assertIn("versionCode='${GITHUB_RUN_NUMBER}'", self.script)
        self.assertIn("sdkVersion:'24'", self.script)
        self.assertIn("targetSdkVersion:'36'", self.script)
        self.assertIn("test_debug_apk_installability_workflow.py", workflow)


if __name__ == "__main__":
    unittest.main()
