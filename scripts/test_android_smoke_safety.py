"""Fail-closed smoke safety tests. All ADB/aapt/apksigner are local fakes."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).with_name("android-smoke.sh")


class AndroidSmokeSafetyTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.base = self.root / "base.apk"
        self.upgrade = self.root / "upgrade.apk"
        self.base.write_bytes(b"local synthetic base")
        self.upgrade.write_bytes(b"local synthetic upgrade")
        self.log = self.root / "adb-actions.log"
        stub = """#!/usr/bin/env python3
import os,sys
from pathlib import Path
name=Path(sys.argv[0]).name
args=sys.argv[1:]
if name == "aapt":
    apk=args[-1]
    package=os.environ.get("FAKE_UPGRADE_PACKAGE", "com.example.lanu") if "upgrade" in apk else "com.example.lanu"
    version=os.environ.get("FAKE_UPGRADE_VERSION", "6") if "upgrade" in apk else "5"
    print("package: name='%s' versionCode='%s' versionName='1.0'" % (package,version))
elif name == "apksigner":
    cert=os.environ.get("FAKE_UPGRADE_CERT", "a"*64) if "upgrade" in args[-1] else "a"*64
    print("Signer #1 certificate SHA-256 digest: "+cert)
elif name == "adb":
    with open(os.environ["FAKE_ADB_LOG"],"a") as f: f.write(" ".join(args)+chr(10))
    assert args[:2] == ["-s","emulator-5554"],args
    cmd=args[2:]
    if cmd == ["get-serialno"]: print("emulator-5554")
    elif cmd == ["shell","getprop","sys.boot_completed"]: print("1")
    elif cmd == ["shell","getprop","ro.kernel.qemu"]: print(os.environ.get("FAKE_QEMU", "1"))
    elif cmd == ["shell","pm","path","com.example.lanu"]:
        print(os.environ.get("FAKE_INSTALLED_PATH", ""))
    elif cmd[:2] == ["shell","pidof"]: print("12345")
    elif cmd[:3] == ["shell","dumpsys","package"]: print("versionCode=6")
    elif cmd[:1] == ["install"]: print("Success")
"""
        for name in ["adb", "aapt", "apksigner"]:
            f = self.root / name
            f.write_text(stub)
            f.chmod(0o755)
        sleeper = self.root / "sleep"
        sleeper.write_text("#!/bin/sh\nexit 0\n")
        sleeper.chmod(0o755)
        self.env = {
            **os.environ, "PATH": f"{self.root}:{os.environ['PATH']}",
            "ADB": str(self.root / "adb"), "AAPT": str(self.root / "aapt"),
            "APKSIGNER": str(self.root / "apksigner"),
            "LANU_SMOKE_DEVICE_SERIAL": "emulator-5554",
            "LANU_SMOKE_EXPECTED_PACKAGE": "com.example.lanu",
            "FAKE_ADB_LOG": str(self.log), "SMOKE_TIMEOUT": "2",
        }

    def run_script(self, *args, **env):
        return subprocess.run(["bash", str(SCRIPT), str(self.base), *map(str, args)],
                              env={**self.env, **env}, text=True, capture_output=True)

    def installed(self):
        return " install " in (" " + self.log.read_text()) if self.log.exists() else False

    def test_read_only_preflight_never_installs(self):
        r = self.run_script(self.upgrade)
        self.assertEqual(0, r.returncode, r.stderr)
        self.assertIn("NO INSTALL", r.stdout)
        self.assertFalse(self.installed())

    def test_explicit_approval_required_for_install(self):
        r = self.run_script(LANU_SMOKE_MODE="install")
        self.assertNotEqual(0, r.returncode)
        self.assertIn("approval", r.stderr)
        self.assertFalse(self.installed())

    def test_mismatched_signer_rejected_before_install(self):
        r = self.run_script(self.upgrade, LANU_SMOKE_MODE="install",
                            LANU_SMOKE_ENABLE_INSTALL="I_APPROVE_DEDICATED_TEST_DEVICE",
                            FAKE_UPGRADE_CERT="b"*64)
        self.assertNotEqual(0, r.returncode)
        self.assertIn("signer mismatch", r.stderr.lower())
        self.assertFalse(self.installed())

    def test_wrong_or_equal_version_rejected_before_install(self):
        for version in ("4", "5"):
            r = self.run_script(self.upgrade, LANU_SMOKE_MODE="install",
                                LANU_SMOKE_ENABLE_INSTALL="I_APPROVE_DEDICATED_TEST_DEVICE",
                                FAKE_UPGRADE_VERSION=version)
            self.assertNotEqual(0, r.returncode)
            self.assertIn("strictly increase", r.stderr)
            self.assertFalse(self.installed())

    def test_existing_installed_app_is_never_overwritten(self):
        r = self.run_script(LANU_SMOKE_MODE="install",
                            LANU_SMOKE_ENABLE_INSTALL="I_APPROVE_DEDICATED_TEST_DEVICE",
                            FAKE_INSTALLED_PATH="package:/data/app/exists.apk")
        self.assertNotEqual(0, r.returncode)
        self.assertIn("already installed", r.stderr)
        self.assertFalse(self.installed())

    def test_physical_device_requires_second_explicit_permission(self):
        r = self.run_script(LANU_SMOKE_MODE="install",
                            LANU_SMOKE_ENABLE_INSTALL="I_APPROVE_DEDICATED_TEST_DEVICE",
                            FAKE_QEMU="0")
        self.assertNotEqual(0, r.returncode)
        self.assertIn("Physical devices", r.stderr)
        self.assertFalse(self.installed())

    def test_unapproved_package_refused(self):
        r = self.run_script(LANU_SMOKE_MODE="install",
                            LANU_SMOKE_ENABLE_INSTALL="I_APPROVE_DEDICATED_TEST_DEVICE",
                            LANU_SMOKE_EXPECTED_PACKAGE="com.evil")
        self.assertNotEqual(0, r.returncode)
        self.assertIn("not explicitly approved", r.stderr)
        self.assertFalse(self.installed())

    def test_fresh_dedicated_emulator_can_verify_upgrade(self):
        r = self.run_script(self.upgrade, LANU_SMOKE_MODE="install",
                            LANU_SMOKE_ENABLE_INSTALL="I_APPROVE_DEDICATED_TEST_DEVICE")
        self.assertEqual(0, r.returncode, r.stderr)
        self.assertIn("not data-retention proof", r.stdout)
        installs=[x for x in self.log.read_text().splitlines() if " install " in " "+x]
        self.assertEqual(2, len(installs))
        self.assertFalse(any("uninstall" in x or "clear" in x for x in self.log.read_text().splitlines()))


if __name__ == "__main__":
    unittest.main()
