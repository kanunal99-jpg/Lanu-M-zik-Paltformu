# LANU Music — Konsolide Ürün Çatısı

## Amaç
LANU Music'i lisanslı katalog gerektirmeden, gerçek yerel içerik ve doğrulanabilir metadata üzerine kurulu profesyonel bir müzik uygulamasına dönüştürmek.

## Tek mimari omurga

`UI → ViewModel → Domain/Repository → Provider Chain → Local Source / Optional Remote Provider → Cache → Safe Local Fallback → Logging/Telemetry → Tests`

Hiçbir kritik kullanıcı akışı tek bir uzak servise bağımlı değildir. Uzak sağlayıcı yoksa uygulama yerel modda çalışmaya devam eder.

## 1. Kimlik ve oturum
- Provider-agnostic `AuthBackend` korunur.
- Mevcut yerel hesap gerçek bir yerel/offline hesap olarak gösterilir.
- Bulut hesabı varmış gibi davranılmaz.
- Remote auth ileride ayrı bir adapter olarak eklenir; LANU'ya ait sağlayıcı/proje doğrulanmadan bağlanmaz.
- Oturum yoksa güvenli yerel kullanıcı kapsamına düşülür.

## 2. Kullanıcı kütüphanesi
Tek `UserLibraryService` üzerinden:
- favoriler
- çalma listeleri
- playlist öğeleri
- dinleme geçmişi
- indirmeler/çevrimdışı içerik referansları

Tüm kayıtlar `userId` kapsamındadır. Eski kullanıcı verileri için migration/fallback korunur.

## 3. Arşiv UX
Arşiv tek merkezdir:
- Çalma Listeleri
- Beğenilenler
- Çevrimdışı
- Geçmiş

Her sekme gerçek repository state'inden beslenir. İşlemler uygulama yeniden açıldığında korunur.

## 4. Dinleme geçmişi
Bir parça gerçekten oynatılmaya başladığında history kaydı oluşturulur.
- son dinlenenler
- tekrar oynat
- geçmişi temizle
- yinelenen kayıtları kontrollü tutma
- bozuk/eksik şarkıda güvenli atlama

History öneri motorunun temel sinyallerinden biridir.

## 5. Player entegrasyonu
Player, kütüphane işlemlerinden bağımsız ikinci bir veri kaynağı oluşturmaz.
- Play → history
- Favorite → user library
- Add to playlist → user library
- Download → local catalog/download state

Media3/MediaSession, audio focus, background playback, headset ve lock-screen davranışı korunur.

## 6. Home / Discover
Sadece doğrulanabilir içerik gösterilir.
- sahte yeni sürüm yok
- sahte oynanma sayısı yok
- uydurma katalog yok
- yerel katalog boşsa açık ve güvenli empty state

Discover, ileride gerçek provider adapter'larından gelen veriyi kullanabilir.

## 7. Öneri motoru
İlk aşamada cihaz üzerinde çalışır:
`history + favorites + playlists + tekrar dinleme + metadata similarity → ranking`

Harici AI kritik yol değildir. Gemini varsa yalnızca kontrollü araç katmanı olarak kullanılabilir; katalog/veritabanına doğrudan erişemez.

## 8. Senkronizasyon
Remote sync geldiğinde zincir:
`Remote Sync → Alternative Provider → Local Queue/Cache → Local Source → Safe Default`

Çakışmalar deterministik çözülür. Offline değişiklikler kaybolmaz.

## 9. Platform özellikleri
Temel ürün oturduktan sonra:
- Android Auto
- widget
- deep links
- paylaşım
- bildirim/MediaSession kontrolleri
- kapsamlı accessibility ve state restoration

## 10. Kalite kapısı
Her değişiklik için:
1. gerçek `main` incelenir
2. feature branch açılır
3. unit + Robolectric testleri eklenir
4. CI çalıştırılır
5. PR incelenir ve merge edilir
6. post-merge Main CI doğrulanır
7. debug APK artifact alınır
8. APK SHA-256 bağımsız doğrulanır

CI veya SHA doğrulanmadan özellik tamamlanmış sayılmaz.

## Lisans sınırı
Bu çatının dışında bırakılan konu müzik lisanslama/rights acquisition'dır. Uygulama lisanslı katalog varmış gibi davranmaz; yalnızca kullanıcının cihazındaki gerçek içerik ve hukuken kullanılabilir/izinli kaynaklar üzerinden çalışır.
