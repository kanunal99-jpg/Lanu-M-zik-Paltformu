# LANU Müzik — GitHub Releases OTA güncelleme

## Kullanıcı akışı
1. Alt menüdeki **Güncelleme** sekmesini aç; **Güncelleme Kontrol** ekranı GitHub Releases'i otomatik tarar.
2. **Güncellemeleri denetle** ile istediğin anda yeniden kontrol et.
3. Yüklenmiş sürümden daha yeni, stabil, imzalı **app-release.apk** ve **app-release.apk.sha256** bulunan yayın varsa **Güncellemeyi indir ve doğrula** çıkar.
4. Uygulama APK'yı kendi önbelleğine indirir; boyutu, SHA-256 özeti, paket kimliği, gerçek APK versionCode değeri ve imza sertifikasını kontrol eder.
5. Doğrulanmışsa **Güncellemeyi yükle** Android'in sistem kurulum ekranını başlatır. Android 8+ cihazlarda kullanıcıdan **Bu kaynaktan yüklemeye izin ver** ayarı istenebilir.
6. Kullanıcı Android sistem onayını verir. Uygulama sessiz kurulum yapmaz, kendini kaldırmaz veya uygulama verilerine müdahale etmez.

## İlk yayın kurulumu
- GitHub Actions signed_release işi, şu kalıcı signing secret alanlarını bekler: LANU_SIGNING_KEYSTORE_BASE64, LANU_SIGNING_STORE_PASSWORD, LANU_SIGNING_KEY_PASSWORD, LANU_SIGNING_KEY_ALIAS.
- Önceki yayınlarda yalnızca app-debug.apk bulunduğu doğrulanmıştır; CI üzerinde debug imzası sabit değildir. **Debug APK dosyaları OTA'ya kabul edilmez.**
- Kullanıcı farklı imzayla APK yüklediyse başka sertifikayla imzalı APK'ya veri kaybı olmadan yükseltme mümkün değildir. **Mevcut uygulamayı kaldırmayın.** Aynı imzayla üretim veya veri yedekleme/taşıma planı gerekir.
- Sürüm kodu CI GITHUB_RUN_NUMBER değerinden türetilir. Release tag biçimi lanu-music-{run number}-{40 haneli commit SHA}.
- Release job önce debug APK yayımlar; signed_release işi sonra aynı release'e imzalı APK ekler. Bu sırada uygulama 'imzalı sürüm bekleniyor' gösterebilir.
- Kaynak GitHub Releases'dir; Play Store dağıtımında Google Play In-App Updates ayrıca değerlendirilmelidir.

## Test kabul
- UpdateReleasePolicyTest: kötü niyetli URL, debug-only release, eski sürüm, imzalı sürüm sıralaması, boş yayınlar, checksum formatı.
- GitHub Actions üzerinden Android build/test.
- Fiziksel cihazda sabit imzalı APK ile doğrulama: indirme, hash/imza/paket kontrolü, Android kurulum onayı, kullanıcı verilerini ve playlistleri koruyarak yükseltme.
- Başarısız indirme ya da kontrol mevcut uygulamayı bozmamalıdır.

## Resmî referanslar
- https://developer.android.com/google/play/app-updates
- https://developer.android.com/reference/android/content/pm/PackageManager
- https://developer.android.com/reference/androidx/core/content/FileProvider
