# LANU Müzik — Katalog devamlılığı ve güvenli APK yayını (16.10.2026)

## Kanıta dayalı kök neden / düzeltme
- `MusicRepository.expandHistoricalTurkishRapArtists()` ilk 25 MusicBrainz sanatçısından sadece 12'sini işliyordu, ancak kalıcı arama konumunu **25** ilerletiyordu. Böylece 13 sanatçı adayı kalıcı olarak atlanabiliyordu. Yeni kod konumu yalnız işlenen aday sayısı kadar ilerletir; bu davranış birim testinde doğrulanır.
- `collectVerifiedCatalogPages()` üst üste boş sayfa sayacını her çağrıda sıfırlıyordu. Şimdi bu sayaç SharedPreferences içinde saklanarak sonraki genişletme turunda sürdürülür. Mevcut indirilen içerik, favoriler ve oynatma listeleri değiştirilmez.
- Bu düzeltmeler eksik kayıtlar için *nedenlerden ikisini* giderir. MusicBrainz metadata listesinin her sanatçının tüm gerçek kayıtlarını kapsadığı veya Audius/Deezer'ın bunların tam akış haklarını sağladığı ileri sürülemez.

## Sürümleme ve kalıcı imzalama
- Debug APK yoluna dokunulmaz; ana dalda başarıyla build+test edilen `app-debug.apk` test amaçlı Release'e eklenir.
- `.github/workflows/android.yml` yeni `signed_release` job'ını yalnızca **main push + build & normal yayın başarılı** olduktan sonra çalıştırır.
- İmza için dört adet kullanıcıya ait GitHub Actions Repository Secret zorunlu:
  - `LANU_SIGNING_KEYSTORE_BASE64` — güvenli Android release keystore dosyasının Base64 metni
  - `LANU_SIGNING_STORE_PASSWORD` — keystore şifresi
  - `LANU_SIGNING_KEY_PASSWORD` — anahtar şifresi
  - `LANU_SIGNING_KEY_ALIAS` — key alias
- Hiçbiri mevcut değilse signed-release job bildirimi verir; **imzalı APK oluşturulduğunu iddia etmez**. Secret'lar repoya veya ChatGPT sohbetine yazılmaz. Keystore geçici runner yolunda tutulur ve işlem sonunda silinir.
- Dört secret mevcutsa `assembleRelease`, `apksigner verify --print-certs`, `unzip -t` ve SHA-256 sonrası mevcut GitHub Release'e `app-release.apk` eklenir. GitHub Actions başarı sonucu fiziksel Android cihazın eski debug imzadan bu yeni release imzaya veri koruyarak yükseltilebildiği anlamına gelmez.
- Mevcut **debug imzasıyla release imzası farklı olabilir**; üstüne kurulumda imza hatası yaşanır. Yerel arşiv/favori kaybı olmadan yükseltme için cihazda gerçek test, yedek ve güvenli geçiş planı gerekir.

## Kapanış kapısı
1. Birim testleri, mevcut canlı katalog testi, APK derlemesi ve checksum başarılı.
2. Kullanıcı 2 Android sürümünde Ceza/Tarkan/Sezen Aksu, katalog genişletme ve diskografi tarama sonuçlarını paylaşır.
3. Lisans anlaşması olmadan üçüncü taraf **30 saniye önizleme** tam şarkı gibi sunulmaz. YouTube sesini gizli oynatıcıyla ayırmak çözüm değildir.
4. APK, güvenli imza/yükseltme ve dünya müziğinin tam kataloğu ayrı açık iş olarak kalır; tamamlanmadıysa kapatılmaz.

## Bağlantılar
- https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/issues/18 — cihaz regresyonları
- https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/issues/20 — dünya kataloğu ve tam dinleme hakları
- https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/issues/22 — kalıcı imzalama ve yükseltme
- https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/issues/16 — SoundCloud OAuth ve oynatma hakları
