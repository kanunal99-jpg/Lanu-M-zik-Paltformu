# LANU Music — Konsolide Tamamlama Çerçevesi

Bu belge, kalan ürün işlerinin tek feature hattında tamamlanması için kabul kriterlerini sabitler. Kodda gerçek olmayan katalog, oynanma sayısı, yayın veya uzak hesap simüle edilmez.

## Tamamlanacak ürün omurgası

1. **Kullanıcı kütüphanesi**
   - Favori, playlist, playlist öğesi, indirme ve geçmiş aynı kullanıcı kimliği kapsamında.
   - Oturum değişiminde önceki kullanıcının verisi görünmez.
   - Yerel hesap açıkça yerel/çevrimdışı olarak etiketlenir.

2. **Geçmiş**
   - Yalnızca gerçek oynatma başlangıcında kayıt.
   - Gerçek şarkı metadata'sı üzerinden gösterim.
   - Tekrar oynatma.
   - Temizleme.
   - Bozuk/kayıp içerikte güvenli atlama.

3. **Playlist**
   - Oluşturma, silme, parçaya ekleme/çıkarma.
   - Kullanıcı kapsamı.
   - Uygulama yeniden açıldığında kalıcılık.

4. **Home / Discover**
   - Yerel katalog boşsa açık empty-state.
   - Doğrulanmamış yeni sürüm, popülerlik veya katalog gösterilmez.
   - Uzak provider yalnızca doğrulanmış adapter olarak eklenebilir.

5. **Öneriler**
   - İlk sürüm cihaz üzerinde: history + favorites + playlists + tekrar + metadata similarity.
   - AI kritik yol değildir.
   - Öneri motoru katalog üretmez; yalnızca mevcut doğrulanabilir parçaları sıralar.

6. **Player**
   - Media3 Player/MediaSession tek oynatma otoritesi.
   - Queue, shuffle, repeat, seek ve audio focus.
   - Arka plan/lock-screen/Bluetooth kontrolleri.
   - Oynatma devamlılığı için yerel state.

7. **Offline**
   - Yerel kaynak öncelikli.
   - Cache/download durumu gerçek dosya veya doğrulanmış yerel kayıtla eşleşir.
   - Ağ yokken kritik kullanıcı akışı çalışmaya devam eder.

8. **Platform yüzeyleri**
   - Android Auto için MediaLibraryService/MediaSession uyumluluğu.
   - Widget için Jetpack Glance.
   - Deep link/share/accessibility/state restoration.

9. **Remote sync**
   - LANU'ya ait gerçek provider belirlenmeden cloud backend bağlanmaz.
   - Zincir: remote → alternative → local queue/cache → local source → safe default.
   - Offline değişiklikler kaybolmaz.

## Kalite kapısı

Her kod değişikliği:

`feature branch → PR → unit/Robolectric → CI → merge → Main CI → debug APK → bağımsız SHA-256`

şeklinde doğrulanır. Bir adım başarısızsa sonraki adıma geçilmez.

## Gerçek veri kuralı

`Song.playCount` gibi doğrulanmamış metrikler varsayılan olarak sıfır/bilinmiyor kabul edilir. Uydurma popülerlik sayıları gerçek veri gibi sunulmaz.

## Harici teknik dayanaklar

- Android Media3, arka plan oynatma için `MediaSessionService`/`MediaLibraryService` modelini önerir.
- Offline-first mimaride yerel veri kaynağı standart doğruluk kaynağı olmalıdır.
- Jetpack Glance, Compose tabanlı Android widget'ları için resmi Jetpack yaklaşımıdır.

Bu belge ürün lisanslama/rights acquisition işini kapsamaz.
