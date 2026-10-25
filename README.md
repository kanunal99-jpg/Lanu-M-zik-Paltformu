# LANU Müzik

Kotlin + Jetpack Compose tabanlı, **yerel müzik ve izin verilen katalog kaynakları** için Android müzik uygulaması. Proje aktif geliştirme aşamasındadır; bir Spotify/YouTube Music aboneliğinin ya da dünyadaki bütün parçaların tam oynatımının yerine geçtiğini iddia etmez.

## Çalışan yazılım omurgası

- MediaStore üzerinden kullanıcının cihazındaki müzikleri okuma; Media3/MediaSession oynatıcı altyapısı.
- Room ve yerel kullanıcı kütüphanesi üzerinden favoriler, çalma listeleri, geçmiş ve çevrimdışı kaynakları yönetme.
- Jetpack Compose arayüz, Hilt ile enjekte edilen `MainViewModel`, Coroutines/Flow.
- Audius, Deezer (uygun olduğu durumda yalnızca önizleme), Jamendo (kimlik/lisans kapısı), MusicBrainz (metadata) ve YouTube (yapılandırmaya bağlı arama/gömülü oynatıcı) adapterları. Bir kaynağın kodda bulunması canlı çalışmasını veya her eserin oynatma hakkını **garanti etmez**.
- GitHub tabanlı güncelleme ekranı ve SHA-256/imza denetimleri; **üretim OTA kabulü henüz tamamlanmamıştır**.

## Android'de deneme ve geliştirici kurulumu

Gereksinimler: JDK 21, Android SDK, Gradle 9.3.1 ve Android Studio/uyumlu build ortamı. Terminalden `gradle testDebugUnitTest`, `gradle lintDebug` ve `gradle assembleDebug` çalıştırılabilir. Çıktı tipik olarak `app/build/outputs/apk/debug/app-debug.apk` olur.

Son yayımlanan derlemeler: [GitHub Releases](../../releases/latest) · [CI](../../actions/workflows/android.yml). **`app-debug.apk` yalnız önizlemedir.** Rastgele CI/debug imzaları önceki kurulu sürümle eşleşmeyebilir. Var olan uygulamayı kaldırmayın, kullanıcı verilerini riske atacak şekilde üzerine yükleme denemeyin. İmzalı üretim APK'sı için önce cihazdaki mevcut imzanın kalıcı anahtarla eşleşmesi ve silmeden yükseltme testleri gerekir.

## Sağlayıcılar ve telif

- Audius: API ve yayın haklarının izin verdiği gerçek katalog/parçalar; ağ, bölge ve kullanılabilirlik değişebilir.
- Jamendo: `JAMENDO_CLIENT_ID` ve kullanım türüne uygun lisans teyidi olmadan tam yayın açılmaz.
- YouTube: `YOUTUBE_DATA_API_KEY` ve sağlayıcı kısıtlamaları gerekir. YouTube sesini ayrıştırma, gizli arka plan akışı veya DRM aşma yapılmaz.
- MusicBrainz: metadata keşfi; tek başına ses dosyası/lisans sağlamaz.

`.env.example` yapılandırma şablonudur. Gizli anahtarları GitHub'a, hata raporlarına veya uygulama kaynak koduna eklemeyin. Mobil uygulamanın içinde kullanılan servis API anahtarları **gizli sayılamaz**; sağlayıcı tarafında paket/sertifika/API kısıtları gerekir.

## Gizlilik ve hesap

Yerel hesap, cihazda saklanan kimliktir; **uzak sunucuda doğrulanmış bir kullanıcı hesabı değildir**. Gerçek hesap parolalarını yerel/deneme giriş alanlarına yazmayın. Sosyal/arkadaş gösterimi gerçek zamanlı uzak senkronizasyon anlamına gelmez. Cihazdaki müzik ve favoriler korunmalı; otomatik silme/yeniden yükleme kabul edilmez.

## Kod düzeni

- `app/src/main/java/com/example/`: `auth/`, `data/`, `di/`, `model/`, `player/`, `service/`, `ui/`, `update/`.
- `app/src/test/`: JVM/Robolectric testleri; `app/src/androidTest/`: sınırlı instrumentation testi.
- `scripts/`: CI denetimleri ve cihazda **salt okunur** tanılama. `scripts/archive/legacy-code-patches/`: eski, **çalıştırılmaması gereken** tek seferlik kod dönüştürme betikleri.
- `docs/`: mimari, ürün kabul kuralları ve geliştirme notları.

## Üretim kabulü

En güncel doğrulanmış kontrol listesi: [25 Ekim 2026 durum raporu](docs/DELIVERY_STATUS_20261025.md) ve [üretim standardı](docs/LANU_PREMIUM_PRODUCTION_STANDARD.md). Kritik açık kapılar: kalıcı imzalı APK, mevcut kurulumla imza uyumu, veri kayıpsız OTA, gerçek Samsung/Xiaomi/Huawei Android ve GMS'siz cihaz testleri, Crashlytics doğrulaması ve kapsamlı erişilebilirlik kontrolleri. CI başarısı gerçek cihaz kabulü yerine geçmez.

## Lisans

Depoda bir `LICENSE` dosyası bulunmuyor. Kod ve üçüncü taraf müzikler için otomatik olarak açık kaynak veya yeniden dağıtım izni **varsaymayın**; lisans kararı ve içerik hakları depo sahibi tarafından ayrıca netleştirilmelidir.
