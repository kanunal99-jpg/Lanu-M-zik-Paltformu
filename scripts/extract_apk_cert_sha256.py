#!/usr/bin/env python3
"""Extract the ONE release APK certificate SHA-256 from verified apksigner output.

Requires `apksigner verify --verbose --print-certs <apk>` on stdin.
Supports new Android SDK output such as "V2 Signer: certificate SHA-256 digest"
and legacy "Signer #1 certificate SHA-256 digest". Fail closed on any
ambiguity, missing verified v2/v3 signature or multiple signers.
"""
from __future__ import annotations

import re
import sys


class CertificateParseError(ValueError):
    pass


CERTIFICATE_PATTERN = re.compile(
    r"^(?:V(?:2|3|3\.1|3\.2) Signer(?: #\d+)?:|Signer #\d+) "
    r"certificate SHA-256 digest: ([a-fA-F0-9]{64})\s*$",
    re.MULTILINE,
)
SIGNER_COUNT_PATTERN = re.compile(r"^Number of signers:\s*(\d+)\s*$", re.MULTILINE)
VERIFIED_PATTERN = re.compile(
    r"^Verified using v(?:2|3|3\.1|3\.2) scheme[^\r\n]*:\s*true\s*$",
    re.MULTILINE,
)


def extract_single_signing_certificate(output: str) -> str:
    counts = SIGNER_COUNT_PATTERN.findall(output)
    if len(counts) != 1 or counts[0] != "1":
        raise CertificateParseError("Exactly one APK signer must be reported")
    if not VERIFIED_PATTERN.search(output):
        raise CertificateParseError("No verified Android APK v2/v3 signature reported")
    digests = CERTIFICATE_PATTERN.findall(output)
    if not digests:
        raise CertificateParseError("Missing APK signer SHA-256 certificate digest")
    distinct = {digest.lower() for digest in digests}
    if len(distinct) != 1:
        raise CertificateParseError("Conflicting certificate SHA-256 digests")
    return next(iter(distinct))


def main() -> int:
    try:
        digest = extract_single_signing_certificate(sys.stdin.read())
    except CertificateParseError as exc:
        print("ERROR: Unsafe APK signing certificate output: " + str(exc), file=sys.stderr)
        return 1
    print(digest)
    return 0


if __name__ == "__main__":
    sys.exit(main())
