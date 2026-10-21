#!/usr/bin/env python3
"""Fail closed on signing certificate mismatch before publishing an upgrade APK.

Expected installed certificate is a SHA-256 digest obtained read-only from the
existing installed LANU package. No device modifications or private keys here.
"""
import argparse
import re
import sys


def normalized_digest(value: str) -> str:
    digest = re.sub(r"[:\s]", "", value).lower()
    if not re.fullmatch(r"[0-9a-f]{64}", digest):
        raise ValueError("Expected exactly one 64-digit SHA-256 certificate fingerprint")
    return digest


def verify(expected_installed: str, expected_release: str, actual_apk: str) -> None:
    if not expected_installed.strip():
        raise ValueError("LANU_INSTALLED_APP_CERT_SHA256 missing: cannot prove in-place upgrade compatibility")
    installed = normalized_digest(expected_installed)
    release = normalized_digest(expected_release)
    actual = normalized_digest(actual_apk)
    if installed != release or installed != actual:
        raise ValueError("Installed, pinned release and APK signing certificates differ; publication blocked")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--installed", required=True)
    parser.add_argument("--pinned", required=True)
    parser.add_argument("--actual", required=True)
    args = parser.parse_args()
    try:
        verify(args.installed, args.pinned, args.actual)
    except ValueError as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1
    print("Verified installed, pinned and APK signing certificate SHA-256 equality.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
