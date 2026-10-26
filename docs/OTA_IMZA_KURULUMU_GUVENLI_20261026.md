# LANU Müzik — aynı uygulamanın üzerine güvenli OTA imzalama hazırlığı

## Neden Güncelleme Kontrol ekranı sürüm buluyor ama kurmuyor?

26.10.2026 tarihinde GitHub Build 590 yalnızca `app-debug.apk` ve SHA-256 dosyasını yayımlıyor.
`app-release.apk` bulunmadığı için LANU'nun `WaitingForSignedApk` ekranı **doğru** şekilde kurulumu engelliyor.
CI başarısı üretim APK'sı yayımlandı demek değildir. Sabit bir özel anahtarla imzalanmış APK ve kurulu
uygulamanın **aynı sertifikaya** sahip olması gerekir. Android PackageInstaller bu sınırı kaldıramaz.

**Mevcut kurulu uygulamayı kaldırmayın, verileri/Room/çalma listelerini temizlemeyin.**

## Var olan aynı imza anahtarı gerçekten elinizdeyse

Bu işlem geliştiricinin **güvenilir bilgisayarında** yapılır, telefonda veya ChatGPT sohbetinde değil.
JDK (`keytool`), Python 3.10+ ve GitHub CLI (`gh`) gerekir. Oturum açmak için `gh auth login` kullanın.
Asıl keystore dosyasını GitHub'a, issue/PR'ye, sohbete veya CI Artifact'ına yüklemeyin.
Keystore'un şifrelenmiş, çevrimdışı en az iki yedeği saklanmalıdır.

1. LANU > **Güncelleme Kontrol** > **SHA-256 parmak izini kopyala**: Bu **açık sertifika kimliğidir**, özel anahtar değildir.
2. Daha önce bu kurulu APK'yi imzalamış gerçek `.jks` veya `.p12` private keystore dosyasını güvenilir bilgisayarda bulun.
   Önceki GitHub CI debug imzasının özel anahtarı saklanmamış olabilir. Bu durumda yeni bir keystore
   **aynı sertifika olamaz** ve mevcut uygulamanın üzerine kurulamaz.
3. Depo kökünden önce salt okunur kontrol çalıştırın:

   ```bash
   python3 scripts/bootstrap_ota_signing.py \
     --keystore "/guvenli/yol/varolan-keystore.jks" \
     --alias "mevcut-anahtar-aliasi" \
     --installed-cert-sha256 "LANU_EKRANINDAN_KOPYALANAN_64_HANELI_SHA256"
   ```

   Şifreler terminalde gizli istenir. Araç var olan `PrivateKeyEntry` ve **anahtar şifresiyle
   imzalama erişimini**, sertifikanın birebir eşleşmesini doğrular. Başarısız olursa **hiçbir GitHub
   değeri değiştirilmez**. Varsayılan çalıştırma daima **dry run**; özel anahtar asla yazdırılmaz.

4. **Yalnızca mevcut kurulu uygulamayla imza birebir eşleştiyse**, açık onayla `--apply` ekleyin.
   Araç `gh secret set` işlemlerini gizli bilgileri komut satırına yazmadan **stdin** üzerinden uygular.
   Yalnız `kanunal99-jpg/Lanu-M-zik-Paltformu` deposuna erişir:
   - Actions Secrets: `LANU_SIGNING_KEYSTORE_BASE64`, `LANU_SIGNING_STORE_PASSWORD`,
     `LANU_SIGNING_KEY_PASSWORD`, `LANU_SIGNING_KEY_ALIAS`
   - Actions Variables (yalnız açık parmak izleri): `LANU_SIGNING_CERT_SHA256`,
     `LANU_INSTALLED_APP_CERT_SHA256`

5. Sonraki `main` derlemesini başlatın. Sadece **Publish verified production-signed APK** işi
   **success** olup Release sayfasında indirilebilir **app-release.apk** ve **app-release.apk.sha256**
   bulunursa sonraki aşamaya geçin. SHA-256, sürüm numarası, uygulama kimliği
   (`com.aistudio.lanumusic.kpxzqa`) ve kurulu sertifikayla eşleşmeyi denetleyin.
6. Samsung A55'te LANU içinden indir → SHA/imza doğrula → Android sistem onayı → üzerine kur
   zincirini deneyin. Kurulum öncesi ve sonrası Room favorileri, çalma listeleri ve çevrimdışı müzik
   verileri kıyaslanmadan OTA tamamlandı denmez. Ardından Xiaomi/Huawei için de smoke gerekir.

## Mevcut debug APK'nin imzalama anahtarı KAYIP ise

**Var olan uygulamayı veri kaybı olmadan üstüne yükseltmek imkânsızdır.** Yeni keystore üretmek,
dosya ismini `app-release.apk` yapmak veya Android'in imza kontrolünü devre dışı bırakmak çözüm
değildir. `bootstrap_ota_signing.py` eşleşmeyen anahtarda işlem yapmaz. Kullanıcıya danışılmadan
uygulama kaldırma, veri silme, yedek varmış gibi davranma veya zorla paket değiştirme yoktur.

Böyle bir durumda ayrı, açıkça kullanıcı tarafından onaylanan **veri dışa aktarma / doğrulanmış
geri yükleme / bir kez temiz kalıcı imzalı kurulum** geçişi tasarlanmalıdır. O geçişten **sonraki**
aynı sertifikalı sürümler gerçek uygulama içi OTA yapabilir. Mevcut otomatik Android sistem
yedeklerini eksiksiz geri yüklenecekmiş gibi varsaymayın.

## Testler

- `python3 -m unittest discover -s scripts -p 'test_bootstrap_ota_signing.py'`: gerçek geçici JKS
  ile anahtar erişimi, yanlış parola, bozuk/missing sertifika, imza uyuşmazlığı, dry-run, sabit
  GitHub repo hedefi ve stdin üzerinden sır paylaşımı; sahte `gh` çağrılarıyla test edilir.
- CI, bu testleri gerçek APK üretiminden önce repository guardrail aşamasında çalıştırır.
- **Gerçek GitHub Secrets bu testlerde oluşturulmaz.** Bu çalışma yayın imzalama anahtarlarını
  üretmez, kurtarmaz ve mevcut Build 590'ın OTA uyumluluğunu tek başına sağlamaz.
