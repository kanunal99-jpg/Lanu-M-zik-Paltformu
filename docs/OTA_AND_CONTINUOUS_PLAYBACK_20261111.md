# LANU Müzik — Tam müzik ve güvenli OTA kabulü (11 Kasım 2026)

## 30 saniye kök nedeni ve çözüm sınırları
Deezer public API, `preview` URL'si ile yalnız resmî kısa örnek sağlar; tam ses dosyasını veya lisansını sağlamaz. Uygulama önizlemeyi tam şarkıya dönüştürmez, döngüye almaz, DRM atlatmaz veya başka eseri aynı eser diye göstermez.

- Arama ve ana sayfa varsayılan olarak yalnız izinli tam kayıtları öne çıkarır.
- Deezer önizlemeleri arama filtresi isteğe bağlı kaldırıldığında yalnız katalog bilgisi olarak görünür. Oynatıcı ve Media3 ayrı giriş kapıları `continuousSource` ile örnek URL'yi reddeder.
- Cihazdaki MediaStore dosyaları ve lisans koşullarını sağlayan Audius/Jamendo kaynakları normal oynar. Jamendo için client ID ve ticari kullanım lisansı teyidi gerekir.
- Tam kayıt yoksa dürüstçe bildirilir. Kullanıcının favori, playlist, kimlik, Room kayıtları ve çevrimdışı dosyaları korunur.

## Uygulama içinden OTA
LANU Hesap Makinesi'ndeki güvenli imza sürekliliği ilkesi esas alınır.

1. Güncellemeler sekmesi ve açılış kontrolü, resmî GitHub Releases'ten daha yüksek versionCode arar.
2. Güncelleme yalnız `app-release.apk` + `app-release.apk.sha256` için; taslak, ön sürüm ve `app-debug.apk` reddedilir.
3. HTTPS yönlendirme/doğru byte-range, kısmi indirmeye devam, SHA-256, Android paketi, APK signer ve kurulu uygulama sertifikası doğrulanır.
4. APK, Android PackageInstaller üzerinden aynı pakete kullanıcı onayıyla kurulur; alternatif sistem kurucusu vardır. Hiçbir adım uninstall, data clear veya gizli kurulum yapmaz.
5. `main` push veya `workflow_dispatch` sonrası, CI yalnız kalıcı anahtar ve sertifika kontrollerinden geçen **ayrı signed_release** işiyle `app-release.apk` yayımlar ve GitHub'dan geri indirerek gerçek SHA-256 bytes karşılaştırması yapar.

## Zorunlu üretim önkoşulları (henüz doğrulanmadı)
- Telefonda kurulu LANU'nun Güncellemeler sekmesinden SHA-256 sertifika izi alınır.
- Mevcut *kalıcı* keystore imzasıyla uyuştuğu kanıtlanır. Eski debug APK farklı imzalıysa Android'in veri kaybı olmadan üstüne kurulumuna imkân yoktur. Bypass edilmez.
- `scripts/bootstrap_ota_signing.py` önce dry-run yapılır; özel keystore veya parola sohbet, GitHub issue veya kodda yayımlanmaz.
- GitHub secrets: `LANU_SIGNING_KEYSTORE_BASE64`, `LANU_SIGNING_STORE_PASSWORD`, `LANU_SIGNING_KEY_PASSWORD`, `LANU_SIGNING_KEY_ALIAS`.
- GitHub variables: `LANU_SIGNING_CERT_SHA256`, `LANU_INSTALLED_APP_CERT_SHA256`.
- Başarılı main CI veya debug APK yayımlanması **OTA tamam** demek değildir. Signed Release, gerçek indirme, üstüne kurulum, sürüm artışı, favoriler/playlist/kullanıcı/indirmeler korunumu ve Samsung A55 ile diğer Android OEM smoke gereklidir.

## Regresyon
- `PlaybackUriPolicyTest`: örnek kaynak metadata olarak tanınır ama tam parça oynatılamaz.
- `MediaLibrarySongPolicyTest`: Media3 yalnız izinli tam kaynaklara erişir.
- `test_release_workflow_integrity.py`: manuel/otomatik imza kapıları ve SHA-256 asset kontrolleri.
- Ana CI: unit, Robolectric, Android lint, R8, imza ve artefakt kontrolü.
