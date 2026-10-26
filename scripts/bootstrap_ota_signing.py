#!/usr/bin/env python3
"""LANU existing-key OTA signing setup, fail closed on certificate mismatch.

This script NEVER creates or exports a private signing key, never uninstalls
Android apps and NEVER puts keystore/password bytes in arguments or output.
It configures GitHub secrets only after matching the installed app signer.
"""
from __future__ import annotations

import argparse
import base64
import getpass
import hashlib
import os
from pathlib import Path
import re
import ssl
import subprocess
import sys
import tempfile

REPOSITORY = "kanunal99-jpg/Lanu-M-zik-Paltformu"
CERT_RE = re.compile(r"-----BEGIN CERTIFICATE-----\s*[A-Za-z0-9+/=\s]+\s*-----END CERTIFICATE-----")


class SetupError(RuntimeError):
    pass


def normalize_fingerprint(raw: str) -> str:
    normalized = re.sub(r"[:\s]", "", raw).lower()
    if not re.fullmatch(r"[0-9a-f]{64}", normalized):
        raise SetupError("Kurulu uygulamanin SHA-256 sertifika parmak izi 64 hex karakter olmali.")
    return normalized


def fingerprint_from_pem(pem_text: str) -> str:
    match = CERT_RE.search(pem_text)
    if match is None:
        raise SetupError("Keystore sertifikasi okunamadi; islem durduruldu.")
    try:
        encoded = ssl.PEM_cert_to_DER_cert(match.group())
    except ValueError as exc:
        raise SetupError("Keystore sertifikasi hatali; islem durduruldu.") from exc
    return hashlib.sha256(encoded).hexdigest()


def check_matching_identity(expected_installed: str, actual_keystore: str) -> str:
    installed = normalize_fingerprint(expected_installed)
    signing = normalize_fingerprint(actual_keystore)
    if installed != signing:
        raise SetupError(
            "IMZA UYUSMAZLIGI: kurulu LANU sertifikasi ile keystore farkli. "
            "Bu anahtarla mevcut uygulamanin ustune veri kayipsiz guncelleme yapilamaz. "
            "GitHub Secrets degistirilmedi; uygulamayi kaldirmayin."
        )
    return installed


def run_checked(args: list[str], *, secret_input: str | None = None,
                env: dict[str, str] | None = None) -> str:
    try:
        proc = subprocess.run(
            args, input=secret_input, text=True, capture_output=True,
            env=env, check=False, timeout=50
        )
    except (OSError, subprocess.TimeoutExpired) as exc:
        raise SetupError("Gerekli arac calistirilamadi: " + args[0]) from exc
    if proc.returncode != 0:
        # Intentionally omit untrusted command stdout/stderr (may disclose secrets).
        raise SetupError("Guvenli kurulum adimi basarisiz: " + args[0] + " (cikis " + str(proc.returncode) + ")")
    return proc.stdout


def certificate_from_existing_keystore(keystore: Path, alias: str,
                                       store_password: str, key_password: str) -> str:
    local_env = os.environ.copy()
    local_env["LANU_OTA_SETUP_STORE_PASSWORD"] = store_password
    local_env["LANU_OTA_SETUP_KEY_PASSWORD"] = key_password
    local_env["LC_ALL"] = "C"
    base = ["-keystore", str(keystore), "-alias", alias,
            "-storepass:env", "LANU_OTA_SETUP_STORE_PASSWORD"]
    listing = run_checked(["keytool", "-list", "-v", *base], env=local_env)
    if not re.search(r"Entry type:\s*PrivateKeyEntry\b", listing):
        raise SetupError("Keystore alias ozel imza anahtari (PrivateKeyEntry) icermiyor.")
    if re.search(r"Keystore type:\s*PKCS12\b", listing, re.IGNORECASE) and key_password != store_password:
        raise SetupError("PKCS12 anahtar sifresi keystore sifresiyle ayni olmali (Java PKCS12 ayri anahtar sifresini desteklemeyebilir).")
    public_certificate = run_checked(
        ["keytool", "-exportcert", "-rfc", *base], env=local_env
    )
    with tempfile.TemporaryDirectory(prefix="lanu-ota-keycheck-") as temporary:
        # A CSR contains public identity only. Creation proves KEY_PASSWORD can
        # access the private key; temporary CSR is deleted after this check.
        run_checked([
            "keytool", "-certreq", "-alias", alias,
            "-keystore", str(keystore),
            "-storepass:env", "LANU_OTA_SETUP_STORE_PASSWORD",
            "-keypass:env", "LANU_OTA_SETUP_KEY_PASSWORD",
            "-file", str(Path(temporary) / "signing-check.csr"),
        ], env=local_env)
    return fingerprint_from_pem(public_certificate)


def configure_github(keystore: Path, alias: str, store_password: str,
                     key_password: str, fingerprint: str) -> None:
    repo_name = run_checked([
        "gh", "repo", "view", REPOSITORY, "--json", "nameWithOwner",
        "--jq", ".nameWithOwner"
    ]).strip()
    if repo_name != REPOSITORY:
        raise SetupError("GitHub hedef deposu dogrulanamadi; gizli anahtarlar GONDERILMEDI.")
    # Each secret goes via STDIN, never process args, shell expansion, echo or log.
    secrets = (
        ("LANU_SIGNING_KEYSTORE_BASE64", base64.b64encode(keystore.read_bytes()).decode("ascii")),
        ("LANU_SIGNING_STORE_PASSWORD", store_password),
        ("LANU_SIGNING_KEY_PASSWORD", key_password),
        ("LANU_SIGNING_KEY_ALIAS", alias),
    )
    for name, value in secrets:
        run_checked(["gh", "secret", "set", name, "--repo", REPOSITORY],
                    secret_input=value)
    # Certificate hashes are PUBLIC, not private key material.
    for name in ("LANU_SIGNING_CERT_SHA256", "LANU_INSTALLED_APP_CERT_SHA256"):
        run_checked(["gh", "variable", "set", name, "--repo", REPOSITORY,
                     "--body", fingerprint])


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=(
        "Validate an EXISTING LANU private keystore against the already installed "
        "app signer. --apply is mandatory before any GitHub change."
    ))
    parser.add_argument("--keystore", type=Path, required=True,
                        help="Trusted, existing private keystore JKS or PKCS12 (never commit it)")
    parser.add_argument("--alias", required=True, help="Private key alias")
    parser.add_argument("--installed-cert-sha256", required=True,
                        help="PUBLIC fingerprint copied from LANU > Guncellemeler")
    parser.add_argument("--apply", action="store_true",
                        help="After checks, explicitly configure protected GitHub Actions values")
    args = parser.parse_args(argv)
    try:
        keystore = args.keystore.expanduser().resolve(strict=True)
        if not keystore.is_file() or keystore.stat().st_size < 128 or keystore.stat().st_size > 16 * 1024 * 1024:
            raise SetupError("Keystore dosyasi eksik veya makul boyut sinirinin disinda.")
        if not 0 < len(args.alias) <= 128 or any(ord(c) < 32 for c in args.alias):
            raise SetupError("Anahtar takma adi gecersiz.")
        installed = normalize_fingerprint(args.installed_cert_sha256)
        print("Mevcut imza anahtari kontrol ediliyor. Sifreler terminalde GORUNMEZ.")
        store_password = getpass.getpass("Keystore sifresi: ")
        key_password = getpass.getpass("Ozel anahtar sifresi (ayniysa tekrar girin): ")
        if not store_password or not key_password:
            raise SetupError("Bos sifreyle islem yapilamaz.")
        actual = certificate_from_existing_keystore(
            keystore, args.alias, store_password, key_password
        )
        check_matching_identity(installed, actual)
        print("OK: Kurulu uygulama sertifikasi ve mevcut Ozel keystore eslesiyor.")
        print("Genel sertifika SHA-256: " + installed)
        if not args.apply:
            print("Sadece kontrol yapildi. GitHub ayarlari DEGISTIRILMEDI.")
            print("Guvenli yuklemeyi baslatmak icin --apply kullanin.")
            return 0
        print("GitHub Actions Secrets alanina gizli anahtar/sifreler yazilacak.")
        if input("Onay icin birebir 'LANU IMZA ONAY' yazin: ").strip() != "LANU IMZA ONAY":
            raise SetupError("Onay verilmedi; GitHub ayarlari degistirilmedi.")
        configure_github(keystore, args.alias, store_password, key_password, installed)
        print("GitHub imza bilgileri ayarlandi. SONRAKI main CI calismasinda ")
        print("app-release.apk dosyasinin dogrulanmasi gerekir; telefonda OTA testi halen gerekli.")
        return 0
    except (SetupError, OSError, EOFError) as exc:
        print("HATA: " + str(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
