#!/usr/bin/env python3
"""Create a NEW, permanently reusable LANU release signer on the owner's computer.

This creates NO GitHub release and makes NO changes to already installed apps.
The first APK signed with this key is NEW INSTALL ONLY when old installations
have a different signer. Never run this on an untrusted/shared computer.
"""
from __future__ import annotations

import argparse
import base64
import json
import os
from pathlib import Path
import secrets
import stat
import sys

from bootstrap_ota_signing import (
    REPOSITORY, SetupError, certificate_from_existing_keystore, run_checked,
)

REPO_ROOT = Path(__file__).resolve().parents[1]
KEYSTORE_FILENAME = "lanu-music-release.p12"
RECOVERY_FILENAME = "lanu-music-release-recovery.json"
ALIAS = "lanu-music-ota"
CONFIRM_TEXT = "YENI KURULUM IMZASI ONAY"


def safe_private_directory(raw: Path) -> Path:
    if not raw.is_absolute():
        raise SetupError("Mutlak bir özel klasör yolu kullanın; repo içinde olamaz.")
    directory = raw.expanduser().resolve()
    if directory == REPO_ROOT or REPO_ROOT in directory.parents:
        raise SetupError("Git deposuna private keystore veya kurtarma dosyası konulamaz.")
    return directory


def write_private_json(path: Path, payload: dict[str, str]) -> None:
    descriptor = os.open(str(path), os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8") as stream:
            json.dump(payload, stream, ensure_ascii=False, indent=2)
            stream.write("\n")
    except BaseException:
        path.unlink(missing_ok=True)
        raise


def verify_owned_material(directory: Path) -> dict[str, str]:
    recovery = directory / RECOVERY_FILENAME
    keystore = directory / KEYSTORE_FILENAME
    for path in (recovery, keystore):
        if not path.is_file() or path.is_symlink():
            raise SetupError("Özel imza dosyası/kurtarma kaydı bulunamadı; durduruldu.")
        if stat.S_IMODE(path.stat().st_mode) & 0o077:
            raise SetupError("Özel imza dosyası başkaları tarafından okunabilir; durduruldu.")
    if stat.S_IMODE(directory.stat().st_mode) & 0o077:
        raise SetupError("Özel imza klasörünün izinleri güvensiz (0700 gerekli).")
    manifest = json.loads(recovery.read_text(encoding="utf-8"))
    for field in ("alias", "password", "fingerprint", "repository"):
        if not isinstance(manifest.get(field), str) or not manifest[field]:
            raise SetupError("Kurtarma dosyasında eksik alan var.")
    if manifest["repository"] != REPOSITORY or manifest["alias"] != ALIAS:
        raise SetupError("Kurtarma kaydı bu uygulama için oluşturulmamış.")
    actual = certificate_from_existing_keystore(
        keystore, manifest["alias"], manifest["password"], manifest["password"]
    )
    if actual != manifest["fingerprint"]:
        raise SetupError("Kurtarma dosyasıyla özel keystore sertifikası eşleşmiyor.")
    return manifest


def initialize_new_signer(directory: Path) -> dict[str, str]:
    if directory.exists():
        raise SetupError("Özel klasör zaten var. Önceki imzanın üzerine ASLA yazılmaz.")
    directory.mkdir(mode=0o700, parents=False)
    password = secrets.token_urlsafe(48)
    env = os.environ.copy()
    env["LANU_FRESH_OTA_STORE_PASSWORD"] = password
    keystore = directory / KEYSTORE_FILENAME
    try:
        run_checked([
            "keytool", "-genkeypair", "-noprompt",
            "-alias", ALIAS,
            "-keyalg", "RSA", "-keysize", "3072",
            "-validity", "9125", "-storetype", "PKCS12",
            "-dname", "CN=LANU Music Permanent OTA Signer, O=LANU, C=TR",
            "-keystore", str(keystore),
            "-storepass:env", "LANU_FRESH_OTA_STORE_PASSWORD",
            "-keypass:env", "LANU_FRESH_OTA_STORE_PASSWORD",
        ], env=env)
        os.chmod(keystore, 0o600)
        fingerprint = certificate_from_existing_keystore(
            keystore, ALIAS, password, password
        )
        manifest = {
            "repository": REPOSITORY,
            "alias": ALIAS,
            "password": password,
            "fingerprint": fingerprint,
            "warning": "KEEP OFFLINE. This file contains a PRIVATE signing password. NEVER COMMIT or SHARE.",
        }
        write_private_json(directory / RECOVERY_FILENAME, manifest)
        verify_owned_material(directory)
        return manifest
    except BaseException:
        # Fail closed: do not accidentally leave an unrecorded signer to reuse.
        # Do not delete a successfully generated private key; report recovery issue.
        raise


def enroll_github(directory: Path, manifest: dict[str, str]) -> None:
    if run_checked([
        "gh", "repo", "view", REPOSITORY, "--json", "nameWithOwner",
        "--jq", ".nameWithOwner",
    ]).strip() != REPOSITORY:
        raise SetupError("GitHub depo kimliği doğrulanamadı. Gizli veriler GÖNDERİLMEDİ.")

    # IMPORTANT: this is NOT a match with older installed/debug APK signers.
    # NEW_INSTALL_ONLY must never be represented as an existing-device OTA pass.
    run_checked([
        "gh", "variable", "set", "LANU_OTA_CHANNEL", "--repo", REPOSITORY,
        "--body", "NEW_INSTALL_ONLY",
    ])
    key_bytes = (directory / KEYSTORE_FILENAME).read_bytes()
    for name, value in (
        ("LANU_SIGNING_KEYSTORE_BASE64", base64.b64encode(key_bytes).decode("ascii")),
        ("LANU_SIGNING_STORE_PASSWORD", manifest["password"]),
        ("LANU_SIGNING_KEY_PASSWORD", manifest["password"]),
        ("LANU_SIGNING_KEY_ALIAS", manifest["alias"]),
    ):
        # Secrets ONLY enter gh via stdin, not args, stdout or repo files.
        run_checked(["gh", "secret", "set", name, "--repo", REPOSITORY], secret_input=value)
    run_checked([
        "gh", "variable", "set", "LANU_SIGNING_CERT_SHA256", "--repo", REPOSITORY,
        "--body", manifest["fingerprint"],
    ])
    # Do not set LANU_INSTALLED_APP_CERT_SHA256 to an unverified fresh signer.
    print("NEW_INSTALL_ONLY GitHub signer configured. No existing APK signer is asserted.")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=(
        "Create or enroll a permanent NEW-ONLY signing key. Never migrates old app data."
    ))
    parser.add_argument("--private-dir", type=Path, required=True,
                        help="Absolute path OUTSIDE git checkout (created mode 0700)")
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--init", action="store_true",
                       help="Generate new keystore plus recovery credentials LOCALLY only")
    group.add_argument("--apply", action="store_true",
                       help="Upload an existing local signer to GitHub Secrets")
    args = parser.parse_args(argv)
    try:
        directory = safe_private_directory(args.private_dir)
        print("UYARI: Yeni imza, eski farklı imzalı uygulamanın üzerine kurulamaz.")
        print("Eski uygulamayı SİLMEYİN; veri aktarımı ve yeni kurulum ayrı planlanmalıdır.")
        if args.init:
            if input("Onay: " + CONFIRM_TEXT + " : ").strip() != CONFIRM_TEXT:
                raise SetupError("İşlem onaylanmadı.")
            manifest = initialize_new_signer(directory)
            print("Yeni kalıcı imza oluşturuldu. Parmak izi (açık veri): " + manifest["fingerprint"])
            print("Kurtarma dosyasını ve keystore'u güvenli çevrimdışı iki yedekte saklayın.")
            print("Hiçbir GitHub değişikliği yapılmadı.")
        else:
            manifest = verify_owned_material(directory)
            print("Var olan yeni anahtar ve yerel kurtarma dosyası eşleşiyor.")
            if input("GitHub Secrets için onay: " + CONFIRM_TEXT + " : ").strip() != CONFIRM_TEXT:
                raise SetupError("GitHub ayarları değiştirilmedi.")
            enroll_github(directory, manifest)
            print("Şimdi GitHub Actions android.yml manual dispatch ile imzalı ÖN SÜRÜM üretin.")
            print("Eski kurulumların üzerine OTA asla zorlanmamalıdır.")
        return 0
    except (SetupError, OSError, EOFError, ValueError, KeyError, json.JSONDecodeError) as exc:
        # No secret values in errors.
        print("HATA: güvenli yeni-anahtar işlemi tamamlanamadı: " + exc.__class__.__name__,
              file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
