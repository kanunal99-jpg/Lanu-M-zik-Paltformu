# LANU Müzik — Kullanıcı verilerini silmeden OTA üretim yayını

**Durum (11 Kasım 2026):** PR #137 ana dala birleşti. CI #601 başarılı, Build #601 yalnız **debug ön sürüm** olarak GitHub Releases'e yüklendi. `signed_release` işi **atlandı**: GitHub Actions logu `Stable signing secrets not configured; only debug preview is published` mesajını doğruladı. Henüz üretim OTA sürümü **yayımlanmadı**.

> Bu yönerge **mevcut kurulumun üzerine** güvenli güncelleme içindir. **Uygulamayı kaldırmayın, verisini temizlemeyin, farklı bir anahtarla imza atmayın ve keystore/parolaları sohbet, issue, README veya kod deposuna yüklemeyin.**

## 1. Kullanıcının Samsung A55 cihazında salt okunur hazırlık

1. Zaten **kurulu** LANU Müzik'i açın. **Güncellemeler** sekmesine girin.
2. `Kurulu uygulamanın sertifika SHA-256` değerini **Kopyala** butonuyla alın. Bu 64 karakterlik **açık sertifika parmak izidir, gizli anahtar değildir**.
3. Kurulu sürümün **versionCode** değerini de ekrandan not alın. Yeni sürümün versionCode'u mutlaka bundan **büyük** olmalıdır.
4. Kullanıcının favorileri/çalma listeleri/çevrimdışı dosyaları için güncelleme **öncesi** bir kontrol listesi alın; mümkünse uygulamanın desteklediği güvenli yedekleme yöntemlerini kullanın. Uygulama verisini sıfırlamayın.
5. Ekran okunamıyorsa salt okunur cihaz tanılaması için yetkili bir bilgisayarda `bash scripts/android_device_readonly_smoke.sh` kullanılabilir. Bu betik kurulum veya silme yapmaz.

**Önemli:** Google Play üzerinden Play App Signing ile kurulmuş APK için `upload key` ile cihazdaki **app-signing key** genellikle farklıdır; aynı imza kanıtlanmadan GitHub APK'sı o kurulumun üzerine uygulanamaz. Debug APK veya başkasının APK'sından çıkartılan **genel sertifika**, özel anahtarı geri kazandırmaz.

## 2. Orijinal özel imza anahtarını güvenli bilgisayarda doğrulama

İhtiyaçlar:
- Mevcut uygulamanın imzasını atmış **orijinal** `JKS` veya `PKCS12` keystore dosyası ve alias; bu dosya kullanıcının kendi güvenli bilgisayarında kalmalı.
- Kurulu APK'nın SHA-256 sertifika izi.
- Python 3, JDK `keytool`, GitHub CLI `gh`; GitHub deposunda secret/variable düzenleme yetkisi.
- Bu komutları Android telefonunda değil, tercihen güvenli ve erişimi kısıtlı bilgisayarda çalıştırın.

```bash
git clone https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu.git
cd Lanu-M-zik-Paltformu
gh auth login
gh auth status
python3 scripts/bootstrap_ota_signing.py \
  --keystore "/GIZLI/KONUM/orijinal-imza.jks" \
  --alias "ORIJINAL_ALIAS" \
  --installed-cert-sha256 "CIHAZDAN_KOPYALANAN_64_HEX_KARAKTER"
```

Bu ilk komut **dry-run**'dır: hiçbir GitHub ayarı değiştirmez, anahtar veya parolayı yazdırmaz. Parolaları **terminalin görünmez girişinde** ister, komut satırı argümanlarında istemez. *Sertifika eşleşmezse burada durun*: Android aynı uygulamanın farklı anahtarlı APK'sını veri kaybı olmadan güncelleyemez.

Başarılı eşleşme sonrasında **yalnız depo sahibi ve anahtar yetkilisi** şu işlemi yapabilir:

```bash
python3 scripts/bootstrap_ota_signing.py \
  --keystore "/GIZLI/KONUM/orijinal-imza.jks" \
  --alias "ORIJINAL_ALIAS" \
  --installed-cert-sha256 "CIHAZDAN_KOPYALANAN_64_HEX_KARAKTER" \
  --apply
```

`--apply` ayrıca `LANU IMZA ONAY` şeklinde interaktif onay ister. Değişiklikler `gh secret set` ile **stdin** üzerinden yüklenir; özel keystore'a ait veri git commit'lerine yazılmaz.

## 3. GitHub Actions değerleri

Sistem aşağıdaki **Secrets** değerlerini bekler:

- `LANU_SIGNING_KEYSTORE_BASE64`
- `LANU_SIGNING_STORE_PASSWORD`
- `LANU_SIGNING_KEY_PASSWORD`
- `LANU_SIGNING_KEY_ALIAS`

Aşağıdaki iki **Variables** değeri *birbiriyle aynı*, kurulu uygulamanın 64 hex karakterlik sertifika izi olmalıdır:

- `LANU_SIGNING_CERT_SHA256`
- `LANU_INSTALLED_APP_CERT_SHA256`

`bootstrap_ota_signing.py --apply` yukarıdakileri doğrulanan anahtarla kurar. Değerleri GitHub sohbetine, PR yorumuna ya da uygulama kaynaklarına göndermeyin. GitHub sayfası: [Settings → Secrets and variables → Actions](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/settings/secrets/actions).

**Hata durumları:** Orijinal anahtar yoksa **yeni bir keystore oluşturmayın**; eski kurulumun üzerine güncelleme mümkün değildir. Farklı kurulum kanalları (örneğin Play ve debug) farklı imzalar taşıyorsa her kanal ayrı değerlendirilmeli; otomatik geçişe zorlanmamalıdır.

## 4. Yayın iş akışını manuel tetikleyin

Önce telefondaki kurulu `versionCode`'un bir sonraki CI `github.run_number` değerinden **küçük** olduğunu teyit edin. Küçük değilse sürüm stratejisini düzelten ayrı kod değişikliği ve CI doğrulaması gerekir; `adb install -d`, `--downgrade`, uninstall veya veri silme **yapmayın**.

```bash
gh workflow run android.yml --repo kanunal99-jpg/Lanu-M-zik-Paltformu --ref main
gh run list --repo kanunal99-jpg/Lanu-M-zik-Paltformu --workflow android.yml --limit 5
```

[Actions / Android workflow](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/workflows/android.yml)

Yalnız aşağıdaki koşulların **tamamı** gerçekleşirse üretim APK'sı hazırlanmış sayılır:

1. `build` işi: **success** (canlı katalog, Lint, birim testleri, R8/SDK/APK kontrolü).
2. `release` işi: **success** (debug dosyaları yalnız *prerelease*).
3. `signing_readiness` → `enabled=true`; `signed_release`: **success**, *skipped* olmamalı.
4. APK'nın paket kimliği `com.aistudio.lanumusic.kpxzqa`, sürüm kodu > kurulu sürüm, imzası cihazdaki sertifika ile birebir aynı.
5. GitHub Release içinde `app-release.apk` **ve** `app-release.apk.sha256` mevcut. İş akışı yayımlanan dosyaları GitHub'dan tekrar indirip SHA-256 ve byte karşılaştırması yapar. Yalnız bu kontrol geçince ön sürüm **stable/latest** yapılır.

## 5. Cihaz üzerinde son kabul — silmeden güncelleme

Önce **yalnız size ait ve yedekli bir test kurulumunda** denetleyin; kişisel cihazın verisine zarar verecek deneme yapmayın.

- Aynı paketin imzası ve yeni `versionCode` doğrulandı.
- **Güncellemeler** sekmesinde imzalı sürüm algılanıp indirilebildi.
- Dosya boyutu, SHA-256 ve APK imzası doğrulandı; Android sistem kurulumu **kullanıcı onayı** istedi.
- Uygulama **kaldırılmadan** yükseltildi, yeni sürüm açıldı.
- Favoriler, listeler, Room verisi, giriş/yerel kimlik, çevrimdışı parçalar ve yerel dosya izinleri kaldı.
- Samsung A55; ayrıca mümkün olduğunda Xiaomi ve Huawei (GMS olsun/olmasın), düşük ağ, ağ kesintisi, yeniden indirme, yetersiz alan ve uygulama arka planda iken test edildi.
- Hata olursa uygulama kaldırılmadı; hata günlükleri alındı, kullanıcı verisi korundu.

**Not:** `scripts/android-smoke.sh` yalnız **boş ve özel test cihazına** açık yetkiyle kurulum yapar. Kişisel telefonda kurulu uygulamayı güncellemeye yönelik bir otomatik komut değildir; üretim OTA kabulü için gerçek uygulama içi akış ayrıca elle denenmelidir.

## 6. Başarısızlık ve kurtarma

- **Signing credentials missing:** Orijinal keystore + güvenli `--apply` adımı eksik; debug ön sürümü dışında OTA sunulmaz.
- **Signing certificate changed/mismatch:** Yeni anahtar kullanımı reddedilir; mevcut kurulum silinmez.
- **APK SHA mismatch:** Release kararlıya yükseltilmez. Dosya yeniden üretildikten sonra yeni CI ve yeniden doğrulama gerekir.
- **Eski/aynı versionCode:** Yayın durdurulur; sürüm kodu artırılmadan güncelleme yapılmaz.
- **Farklı cihaz/kurulum imzası:** O cihaz için aynı imzalı üretim hattı yoksa silmeden OTA mümkün olmayabilir; bu kısıtı kullanıcıya açıkça belirtin.
- **Güncellemeler ekranında yeni sürüm görünmüyor:** `/releases/latest`, `app-release.apk` ve `.sha256` kontrol edin. Debug ve prerelease bilinçli olarak yok sayılır.

İlgili takip: [GitHub #29 — OTA imzalı yayın kabulü](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/issues/29). Bu sayfadaki adımlar tamamlanana kadar **kullanıcıya OTA yayımlandı** denmemelidir.
