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
