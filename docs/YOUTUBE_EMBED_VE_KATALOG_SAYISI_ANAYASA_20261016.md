# LANU Müzik — YouTube görünür oynatıcı ve katalog sayısı anayasası

## Kullanıcı talebi
"Kaç şarkı var? YouTube'dan çek kendi platformumda dinleyeyim."

## Gerçek katalog sayısı
- LANU, şarkıları Android cihazındaki Room önbelleği ve izinli sağlayıcı sorgularıyla kademeli indeksler. Repo kodundan dünyanın şarkı toplamı veya telefonun anlık gerçek sayısı çıkarılamaz.
- Keşfet'te **`allSongs.size` indekslenmiş kayıt** açıkça yazılır; bunun tüm dünya kataloğu olduğu iddia edilmez.
- Kullanıcının önceki cihaz gözlemi: Ceza aramasında **148 kayıt**. Bu sayı cihazına ve güncel veri sağlayıcı sonuçlarına aittir, bugün yeniden ölçülmedi.

## Yasal YouTube entegrasyonu
- **LANU içinden ayrılmadan** resmî görünür YouTube oynatıcısıyla bağlantısı paylaşılan *gömülebilir* video izlenebilir. İlk aşamada URL/video ID girişi vardır.
- Videonun sesini MP3/akış olarak ayırma, gizli player, arka plan müziği, DRM/reklam/geoblocking atlatma, YouTube içeriğini LANU sunucularına indirme **yok**.
- Oynatıcının markası, kontrolleri, reklamları ve YouTube platform bağlantıları engellenmez. Bazı videolar gömmeyi kapatır; kullanıcıya bunu tam dinleme garantisi gibi gösterme.
- YouTube üzerinden kullanıcı araması ve otomatik katalog eşleştirmesi bu PR içinde yapılmaz; bunun için **resmî YouTube Data API** ve uygun kimlik/yetki, kota, sağlayıcı şartları ve gerekiyorsa sunucu tarafı API anahtarı koruması gerekir.
- YouTube izleme, Audius izinli tam ses ve Deezer 30 sn preview birbirinden ayrı kalır. YouTube video oynatımı LANU'nun Media3 arka plan oynatma motoruna gizli ses kanalı olarak bağlanmaz.
- URL parser yalnız `https` ve resmî izinli YouTube hostlarına izin verir; yetkisiz URL/HTTP reddedilir.
- Pencere kapanınca Android WebView durdurulur/yok edilir; arka planda fark edilmeyen YouTube oynatma sürmez.

## Yayın kabulü
1. URL/ID doğrulama birim testleri ve Android build CI.
2. Android cihazda YouTube ekranı içinde görünür player ile oynatma; Youtube embed engellenmişse yanlış vaat vermeyen hata.
3. Uygulama içine dönüş; video kapanınca sesin durması; mevcut LANU oynatıcısının çakışmaması; favori ve yerel verilerin korunması.
4. İmzalama ve veri kayıpsız yükseltme için GitHub issue #22 açık; key paylaşımı yalnız GitHub Secrets aracılığıyla.
5. Otomatik API key olmadan "tüm YouTube şarkıları otomatik getirildi" beyanı yapılmaz.

## Resmî kaynaklar
- https://developers.google.com/youtube/iframe_api_reference
- https://developers.google.com/youtube/terms/developer-policies
- https://developers.google.com/youtube/v3/docs/search/list

**Durum:** Görünür player ve güvenli URL işleme uygulandı. Kapsamlı YouTube arama veya tüm eserlerin tam uzunlukta oynatılması bu paketle tamamlanmaz.
