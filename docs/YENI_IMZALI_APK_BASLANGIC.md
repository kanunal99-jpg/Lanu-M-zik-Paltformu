# LANU Müzik — YENİ kalıcı imzalı APK'yı başlangıç sürümü yapmak

> **İstenen yeni başlangıç hattı.** Bu bir **NEW_INSTALL_ONLY** imza kanalıdır:
> eski debug/başka imzalı kurulumların üzerine otomatik kurulumu ve eski
> kullanıcı verilerinin taşınmasını **garanti etmez**. Mevcut uygulamayı
> **silmeyin**; özellikle favoriler, çalma listeleri ve çevrimdışı veri kaybolabilir.
> Yalnız yeni/boş bir kurulum bu APK'yı doğrudan alabilir.

## Neden eski Build 601 kullanılmıyor?

Build 601 GitHub'daki `app-debug.apk` bir deneme paketidir. CI tarafından üretilen geçici debug anahtarı kalıcı yayın imzası olarak **kullanılamaz**. Yeni sürümlerin aynı uygulamanın üzerine yüklenebilmesi için ilk andan itibaren **değiştirilmeyen, özel, güvenle yedeklenen üretim imzası** gereklidir.

**Yeni APK'nın üretilebilmesi için tek seferlik sahip adımı:** yeni keystore'un kullanıcının kendi güvenli bilgisayarında oluşturulması ve GitHub Actions Secrets'a kayıt edilmesi. Keystore veya şifre asla bu sohbete, issue'ya veya Git koduna eklenmez. Bu işlemi bir başkası/CI geçici anahtarla sizin yerinize yaparsa ilerideki tüm güncellemeler risk altındadır.

## Adım A — Kalıcı imza oluştur (bilgisayarda, yalnız ilk sefer)

Gerekenler: Python 3, Java/JDK `keytool`, GitHub CLI `gh`, depo sahibinin erişimi ve erişimi yalnız size ait, **repo dışında** güvenli bir klasörün üst dizini. Örnek Linux/macOS/WSL komutları:

```bash
git clone https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu.git
cd Lanu-M-zik-Paltformu

python3 scripts/bootstrap_fresh_ota_signing.py \
  --private-dir "$HOME/lanu-secret-signing-v1" \
  --init
```

Betik sizden `YENI KURULUM IMZASI ONAY` ifadesini isteyecek. Rastgele güçlü parola ve 3072-bit RSA, PKCS12 dosyasını **yalnız bu bilgisayarda** üretir. Klasör izinleri `0700`; keystore ve kurtarma JSON'u `0600` olur. `lanu-music-release-recovery.json` **şifreyi içerir**. Bunu ekran görüntüsü, GitHub, bulut paylaşımı veya sohbet ile göndermeyin.

**Çok önemli:** `lanu-music-release.p12` ve kurtarma dosyasını birlikte iki ayrı güvenli çevrimdışı noktaya yedekleyin. Gelecekte aynı imzayı tekrar üretmenin bir yolu **yoktur**. `--init` mevcut dosyanın üzerine yazmaz.

## Adım B — GitHub'a güvenli imza kaydı (bir kez)

```bash
gh auth login
gh auth status

python3 scripts/bootstrap_fresh_ota_signing.py \
  --private-dir "$HOME/lanu-secret-signing-v1" \
  --apply
```

Betik önce yerel keystore sertifikasını kurtarma dosyasıyla karşılaştırır, ardından ayrıca onay ister. GitHub'da şu alanları yazar:

- **Secrets:** `LANU_SIGNING_KEYSTORE_BASE64`, `LANU_SIGNING_STORE_PASSWORD`, `LANU_SIGNING_KEY_PASSWORD`, `LANU_SIGNING_KEY_ALIAS` (yalnız stdin).
- **Variables:** `LANU_SIGNING_CERT_SHA256`, `LANU_OTA_CHANNEL=NEW_INSTALL_ONLY`.

**Bilinçli güvenlik ayrımı:** Betik `LANU_INSTALLED_APP_CERT_SHA256` değerini üretmez veya eski cihazda yeni imzanın kurulu olduğunu iddia etmez. Gerekirse var olan eski değişken bu modda kullanılmaz. Bir uygulama zaten eski/başka sertifikayla kurulmuşsa yeni APK'nın üzerine kurulumu Android tarafından engellenir; silme veya imza bypass yok.

## Adım C — Yeni, sabit imzalı APK üret

[GitHub Actions → LANU Music Platform CI](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/workflows/android.yml) sayfasında **Run workflow → main**. Alternatif:

```bash
gh workflow run android.yml --repo kanunal99-jpg/Lanu-M-zik-Paltformu --ref main
gh run list --repo kanunal99-jpg/Lanu-M-zik-Paltformu --workflow android.yml --limit 5
```

Yayın kabulü için:
1. `build` → **success**: Lint, birim testleri, R8 ve APK smoke.
2. `signing_readiness` → **enabled=true** (sırların bulunduğuna dair, içerikleri göstermeyen kontrol).
3. `signed_release` → **success**, skip değil. Gerçek `app-release.apk`, `app-release.apk.sha256`; GitHub'dan yeniden indirildikten sonra SHA-256 eşit.
4. Paket adı `com.aistudio.lanumusic.kpxzqa`, sürüm kodu CI `run_number`, imza SHA-256 değeri `LANU_SIGNING_CERT_SHA256` ile aynı.
5. Yayın **prerelease** kalır, başlık **SIGNED new-install baseline; NO OTA from old APK** içerir. `releases/latest` hâlâ eski kararlı sürüm olabilir; bu normaldir.

Bu aşamada yeni APK indirilebilir, ancak **eski farklı imzalı kurulumu silmeden güncelleyemez**. Bunu kullanıcılara yanlış biçimde OTA olarak duyurmayın.

## Adım D — Bu yeni APK'dan sonraki tüm güncellemeler

1. Yeni imzalı APK'yı **boş/yeni** test cihazına kurup açılış, oynatma, favoriler, indirmeler ve Güncellemeler sekmesindeki sertifika SHA-256'yı doğrulayın.
2. Aynı dosyanın bir kopyasıyla değil, **aynı GitHub Secrets içindeki değişmez imza anahtarıyla** hazırlanmış bir *daha yüksek versionCode* APK üretin.
3. Yeni kurulumun üzerine Android PackageInstaller ile APK'yı **silmeden** güncelleyin. Seçili test cihazında favori/playlist/Room verilerinin korunmasını gösteren kayıt alın.
4. **Ancak yeni sürümün gerçekten kurulu olduğu cihazda** sertifika SHA-256 eşleşmesi doğrulanınca depo sahibi GitHub Variables'ı ayarlar:
   - `LANU_INSTALLED_APP_CERT_SHA256`: cihazdaki **gerçek** SHA-256
   - `LANU_OTA_CHANNEL`: `EXISTING_INSTALL`
5. Sonraki `main` CI yayınlarında imzalı APK, SHA-256 ve kurulu imza eşleşirse normal **stable/latest** sürüm olarak yayımlanır ve uygulamanın içinden güncellenebilir.
6. Bu yeni hattın **eski imzalı** cihazlara OTA getirdiğini asla iddia etmeyin; onların imzası farklı kalır.

### Hata / güvenlik matrisi

| Durum | Sistem davranışı |
|---|---|
| Orijinal eski uygulamaya yedekli veri kaybı olmadan yükseltme isteniyor | Yeni anahtarlı APK uygun değil; orijinal eski özel imza anahtarı gerekir |
| Yeni anahtar klasörü zaten mevcut | `--init` reddedilir; yanlışlıkla imza rotasyonu yapılmaz |
| GitHub sırları eksik | Debug **prerelease**, signed job **skipped**; OTA yok |
| Sertifika/keystore uyuşmazlığı | İmza doğrulaması durur, APK stable yapılmaz |
| `NEW_INSTALL_ONLY` etkin | Doğrulanmış signed APK yalnız **prerelease**, eski cihazlara stable OTA değil |
| `EXISTING_INSTALL` etkin ama gerçek kurulu sertifika eşleşmiyor | İmzalı yayın durdurulur |
| İmza var, SHA-256 bozuk | İndirilen APK reddedilir, stable promotion yapılmaz |
| Ağ/cihaz kurulumu başarısız | Kurtarma: uninstall/veri silme **yok**; aynı sertifikalı paketle yeniden deneme |

**Doğrulanmış durum — 12.11.2026:** Yeni kalıcı imza sahibi tarafından Termux üzerinde üretildi ve imzalama sırları GitHub Actions'a güvenli olarak yapılandırıldı. [Build #610 CI](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/43095085848) başarıyla tamamlandı. [SIGNED Build 610 prerelease](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/releases/tag/lanu-music-610-e7dc17bd2bc43409d0b576f1c0d6c7ca10259858) içindeki `app-release.apk` ve `app-release.apk.sha256` doğrulandı. APK SHA-256: `c60785ceced392c60522cef4f180701af5ca92aa5962f49149243767be553eea`; imza sertifikası SHA-256: `dc949667ed0219ac64e0870b2dbef644ac94a08ca6ee6b13fe78bf7904f5d822`. Bu **NEW_INSTALL_ONLY** başlangıcıdır; telefonun mevcut LANU kurulumuna OTA teslim edilmiş veya eski uygulamadan veri kayıpsız geçiş yapılmış **değildir**. Güvenli şifreli yedeğin açılabildiği ve cihaz dışında korunduğu ayrıca teyit edilmelidir.
