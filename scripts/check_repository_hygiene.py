#!/usr/bin/env python3
"""Reject obsolete root-level code rewriting scripts and missing project entrypoints.

This is a repository hygiene check, NOT a code/security/Android device test.
"""
from pathlib import Path
import sys


def hygiene_errors(root: Path) -> list[str]:
    errors = []
    for required in ("README.md", "app", "docs", "scripts", ".github"):
        if not (root / required).exists():
            errors.append(f"Missing expected project entrypoint: {required}")

    obsolete = sorted(path.name for path in root.glob("patch_*.py") if path.is_file())
    if (root / "rewrite_services.py").is_file():
        obsolete.append("rewrite_services.py")
    if obsolete:
        errors.append(
            "Legacy code rewrites must not live at repository root: "
            + ", ".join(sorted(obsolete))
        )
    return errors


def main() -> int:
    root = Path(__file__).resolve().parent.parent
    errors = hygiene_errors(root)
    if errors:
        for error in errors:
            print(f"FAIL: {error}", file=sys.stderr)
        return 1
    print("PASS: repository entrypoints exist; unsafe historical root patch scripts are archived.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
