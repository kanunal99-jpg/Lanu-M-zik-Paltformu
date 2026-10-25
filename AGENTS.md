# LANU MUSIC - PROJE ANAYASASI (CONSTITUTION)

Bu belge, uygulamanın geliştirme vizyonunu ve "GERÇEK MÜZİK PLATFORMU" hedefini tanımlar. Ajan (agent), bu projedeki her işleminde bu kurallara kesinlikle uymak zorundadır.

## 1. TEMEL KURAL: UYDURMA VERİ YOK
Gemini hiçbir zaman katalog kaynağı yerine geçmeyecek. Gerçek sanatçı, albüm, şarkı, süre, çıkış tarihi ve streaming bağlantıları kullanılacaktır. Gemini sadece mevcut onaylı katalog üzerinde arama, öneri ve doğal dil işleme (playlist oluşturma vb.) yapacaktır.

## 2. GERÇEK MÜZİK KATALOĞU
Türkçe, Global, Pop, Rock, Rap, Sanat Müziği vb. geniş kapsama sahip, metadata açısından zengin (yıl, tür, süre, ISRC, kapak vb.) gerçek bir katalog kullanılacaktır. Katalogda olmayan bir şey "var" gibi gösterilmeyecektir.

## 3. SES DOSYASI VE TELİF
Sadece yerel müzik dosyaları, lisanslı veya telif şartları uygun (resmi preview) sesler kullanılacaktır. Korsan indirme veya DRM aşma (bypass) yöntemleri uygulanmayacaktır.

## 4. YEREL MÜZİK (MEDIASTORE)
Kullanıcının Android cihazındaki yerel müzikleri (MediaStore) profesyonelce taranıp kütüphaneye dahil edilecektir. Cihaz dosyaları asla silinmeyecektir.

## 5. PROFESYONEL PLAYER
Media3/ExoPlayer kullanılarak gapless playback, lock screen kontrolleri, audio focus, kulaklık kontrolleri, Media Session ve background playback eksiksiz bir şekilde entegre edilecektir. UI ile Player state her an tam senkron olmalıdır.

## 6. SANATÇI VE ALBÜM DENEYİMİ
Sanatçı ve albüm sayfaları Spotify/Apple Music kalitesinde, diskografi odaklı hiyerarşide (Albümler, Single'lar, Popüler Parçalar vb.) tasarlanacaktır.

## 7. ARAMA VE TOLERANS
Katalog içerisinde Türkçe karakter, yazım hatalarına töleranslı gelişmiş arama motoru kurulacaktır.

## 8. GEMINI AI ENTEGRASYONU VE FUNCTION CALLING
Gemini doğrudan veritabanı okumayacak; `searchArtists()`, `createPlaylist()` gibi kontrollü araçlar (functions/tools) üzerinden katalogla konuşacaktır. AI halüsinasyonları playback akışını bozmayacaktır.

## 9. KİŞİSELLEŞTİRME VE PLAYLISTLER
Kullanıcı favorileri, son oynatılanları baz alınarak öneriler sunulacak, Room (SQLite) tabanlı playlist oluşturma, düzenleme mimarisi kurulacaktır.

## 10. RESILIENCE (DAYANIKLILIK) VE OFFLINE ÇALIŞMA
Network hatası, metadata API çökmesi veya Gemini hatası durumlarında uygulama çökmeyecek; offline katalog, cache ve local music fallback ile yoluna devam edecektir. Veri modeli profesyonelce (Room DB kullanılarak) tasarlanacaktır.

## 11. GITHUB & TEST STANDARTLARI
Mevcut çalışan kod gereksiz yere bozulmayacak, her eklenen modül `compile_applet` ile derlenecek ve mümkünse Robolectric ile testleri eklenecektir. "Yapıldı" denmeden önce özelliklerin çalıştığı mutlaka teyit edilecektir.

## 12. LANU PREMIUM PROFESSIONAL STANDARTLARI
Bu repo için ayrıntılı kalite ve üretim kabul listesi **docs/LANU_PREMIUM_PRODUCTION_STANDARD.md** dosyasında tutulur. Mevcut kullanıcı master promptu, üretim güvenliğinde önceliklidir. Yeni modüller için Compose, MVVM/MVI, lifecycle-safe Coroutine/Flow, kaynaklardan yerelleştirme, uygun testler, R8 release ve güvenli OTA doğrulaması esastır. Mevcut fonksiyonları bir seferde riskli büyük refactor ile değiştirmeyin; geriye uyumlu, test edilmiş küçük paketlerle iyileştirin. Kurulu APK sertifikası veya yayın anahtarları olmadan veri kayıpsız OTA hazır raporlanamaz.

## 13. ANDROID ÜRETİCİ BAĞIMSIZ ÜRÜN KAPSAMI (20.10.2026)

**LANU Müzik belirli bir telefon markasına veya modele özel geliştirilmeyecek.** Samsung Galaxy A55 sadece örnek cihazdır. Resmî hedef tüm **Android APK uyumlu** Samsung, Xiaomi/Redmi/POCO, Huawei Android/EMUI veya APK uyumlu eski HarmonyOS, Honor, Oppo, Vivo, Realme, OnePlus, Motorola, Pixel, AOSP ve diğer üreticilerin telefon, tablet ve katlanabilir cihazlarıdır. APK kabul etmeyen HarmonyOS NEXT/5+ gibi sistemler Android APK ile uyumlu sayılmaz.

- Her geliştirme platform API sınırlarına, MediaStore/Media3/Room/Compose ve Android standart kurulum iznine göre yapılır; OEM/Google servisleri zorunlu hale getirilemez. GMS'siz cihazlarda temel yerel müzik ve arşiv açılabilmeli; servis özellikleri güvenli fallback vermelidir.
- `minSdk 24` korunur; Android 17 / API37 dahil sürüm matrisi, yatay/tablet/katlanır, GMS'siz AOSP ve çok üreticili gerçek cihaz smoke ayrı kabul şartıdır.
- Kaynak: [Android OEM Uyumluluk Matrisi](docs/ANDROID_OEM_UYUMLULUK_MATRISI.md). CI statik denetimi ile cihazda çalıştı kanıtı karıştırılmaz. Üretim yayınından önce kalıcı APK signing, **silmeden OTA** ve gerçek cihazlarda kimlik/favori/playlist korunumu şarttır.


## 14. DEĞİŞMEZ SÜREKLİ SAĞLAMLAŞTIRMA DÖNGÜSÜ (25.10.2026)

**Kullanıcı emri: “Çalış; devam et; her zaman sağlamlaştır, yapılan işe dön bak, eksikleri tamamla.”** Her görevde önce `PROJE_ANAYASASI_VE_DUZELTILECEKLER.md` üstündeki güncel protokol gözden geçirilir. Yeni istek, teknik gelişme veya denetimde eksik kural bulunursa **önce anayasa PR'si**, ardından açık P0/P1 regresyonlar, geçmiş PR'lar, testler ve yayın varlıkları tekrar incelenir. “Önceden düzeltildi” sözü, güncel HEAD ve başarısız durum testiyle tekrar teyit edilmeden kabul değildir.

Zorunlu sıra: **Anayasa → öncekileri incele → kök neden → küçük PR → adversarial Unit/Robolectric/Lint/R8 → güvenli merge → yeni main CI → SHA-256'lı indirilebilir Release → gerçek cihaz/veri koruma kabulü → tekrar denetim.** İptal edilmiş/atlanan adım ve debug APK üretim kabulü sayılmaz. P0 gizlilik, kullanıcı verisi, offline URI sahteciliği, harici Media3 kontrolcüsü, OTA imzası, telif ve yanlış kalite vaadi yeni özelliklerden önce gelir.

Tüm kritik zincirler **ana servis → alternatif → gerçek fallback → hata → güvenli varsayılan → log → smoke test** kuralını uygular. Uygulama silinmez, `applicationId` değiştirilmez, eski Room/favori/playlist/kimlik ve offline dosyalar korunur; yeni maliyet yalnız onayla başlatılır. Gerçek cihaz/signed üretim/OTA kanıtı yoksa bu durum açıkça “engelli” gösterilir. Kapanışta alan bazlı kanıtlı puan /100 ve 100/100 hedef açığı ile Issue #34 güncellenir.
