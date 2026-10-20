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


## OTA güvenlik güçlendirmesi — 20.10.2026
- Ana ekran her uygulama açılışında en fazla **6 saatte bir** otomatik kontrol yapar; manuel "Güncellemeleri denetle" her zaman kullanılabilir. İmzalı yeni sürüm varsa alt menüde bildirim noktası çıkar.
- Kontrol önce resmî GitHub **/releases/latest** endpoint'ini inceler. Eski taslak yayınların liste başını doldurması nedeniyle en yeni yayının kaçırılması engellenir. En son release sadece debug ise, önceki uygun imzalı yayın için geçmiş de incelenir.
- Android PackageManager, kurulu olmayan APK'nın PackageInfo.signingInfo alanını boş döndürebilir. Android apksig kütüphanesiyle **APK Signature Scheme** doğrudan doğrulanır ve sertifika SHA-256 özetleri kurulu uygulamayla karşılaştırılır. Uyuşmazlıkta kurulum bloklanır.
- Son kurulum öncesi APK doğrulaması arka plan iş parçacığında yapılır; kullanıcı arayüzü takılmaz ve aynı anda ikinci kurulum başlatılmaz.
- APK indirmesinde yavaş mobil ağlar için 10 dakikalık işlem sınırı ve yüzde değiştikçe ilerleme güncellemesi bulunur.
- Hiçbir güvenlik kontrolü kapatılıp Android'in imza kısıtları atlatılmaz.

## Üretim imzalama engeli
Güncel GitHub Actions çalışmasında üretim imza secret'ları eksikse "Assemble signed release" adımı **skipped** kalır. Bu durumda Release'de görünen app-debug.apk, sabit sertifikalı **OTA sürümü değildir** ve uygulama içinden yüklenmeye uygun gösterilmez.

Bu işin kullanıcı hesabında yalnız bir kez tamamlanması gerekir:
1. Önceden cihaza kurulmuş APK'nın imza sertifikası belirlenir. Yüklü uygulamayı kaldırmayın.
2. Aynı imzanın anahtarı yoksa **veri kayıpsız üzerine kurulum** garanti edilemez. Yeni anahtar üretmek bu sorunu çözmez; veri dışa aktarımı/backup planı gerekir.
3. Anahtar ve şifreler yalnız GitHub Settings → Secrets and variables → Actions alanına kaydedilir; kaynak koda veya sohbete konulmaz.
4. Sonraki başarılı signed_release işinin hem app-release.apk hem app-release.apk.sha256 dosyalarını yayımladığı kontrol edilir.
5. Fiziksel Android cihazda sürüm geçişi ve kullanıcı verileri doğrulanır.

**Bitmiş sayılmayan işler:** İmzalı APK'nın gerçek telefonda üzerine kurulması, önceki debug imzasıyla eşleşme ve uygulama verilerinin korunması testi. Bu işler telefondaki yüklü APK ve imza anahtarı olmadan doğrulanamaz.
