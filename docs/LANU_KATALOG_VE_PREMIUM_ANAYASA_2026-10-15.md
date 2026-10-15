# LANU Müzik — Katalog, Tür ve Yayın Anayasası (15.10.2026)

## Müşteri kabul kriterleri
1. **Alt menüde Sosyal ve Arşiv yok.** Kullanıcı verileri/ses dosyaları, eski arşiv/çalma listesi kayıtları silinmez. Geriye dönük deep-link güvenli ana sayfaya yönlenir.
2. **Premium keşif:** sade dört sekme (Ana Sayfa, Keşfet, Ekolayzır, Hesabım); geniş dokunma alanları, açık sonuç sayıları, sessiz boş sayfa yok, yükleme ve API hata durumları gösterilir.
3. **Doğru kategori:** Türkçe Rap/Türkçe Pop/Türkçe Rock/Anadolu Rock vb. doğrulanmış türden veya konservatif tanınan sanatçı listesinden çıkarılır. Hiçbir kanıt yoksa **Türü Belirsiz**, otomatik Global Pop değil. Var olan Room verileri korunur.
4. **Başka bir dünya şarkı kitabı değil:** uygulamada gösterilen sayı yalnızca yerel/önbelleğe alınmış/doğrulanmış kaynaklardan indekslenen sayıdır. 600 kayıt, dünyada 600 şarkı olduğu anlamına gelmez. Arama sayfalı ve kademeli, kullanıcıya 'kataloğu genişlet' eylemi görünür.
5. **İzinli ses:** YouTube görüntüsünden sesi ayrıştırmak, videoyu görünmez arka planda oynatmak, reklâm/oynatıcıyı kaldırmak veya DRM atlatmak yapılmaz. Resmî gömülü YouTube oynatıcı mümkünse **görünür**, lisanslı ve kurallara uygun olacaktır. Uygulama içinde arka planda tam müzik dinlemesi ancak bu işleme **açık izin veren** kaynaklardan sağlanır.
6. **Metadata ≠ ses akışı:** MusicBrainz (çekirdek CC0) metadata keşfi ile izinli Audius/Jamendo akışı ve Deezer resmî preview kesinlikle ayrılır. Preview tam şarkı sayılmaz. Metadata kaydına hayalî çal butonu eklenmez.
7. **Kaynak çoğaltma/deduplikasyon:** ID + sağlayıcı + eser/recording kimlikleri; aynı başlıkla farklı kayıtlar (remaster/live/feat) yanlışlıkla silinmez. Yasal oynatılabilir kaynak yoksa açık gösterilir.
8. **Ücretsiz ve onay:** Yeni ücretli API, streaming lisansı, yüksek trafik/depolama harcaması kullanıcı onayı olmadan kurulmaz.
9. **Platform sınırlaması:** Google/YouTube ve diğer sağlayıcıların resmî kullanım, telif, rate-limit, API koşulları takip edilir.

## Önce/sonra örnekleri ve doğrulama
| Kanıt/gözlem | Kaynak | Düzeltme | Kabul |
|---|---|---|---|
| 'Ceza' 139 indekslenmiş parça; gerçek kayıt sayısı farklı | 15 Ekim 22:18 kullanıcı ekranı | Gösterilen sayı 'erişilebilir kayıt' olarak etiketlenir; arama diskografileri kademeli indekslenir | Ceza ve örnek 5 sanatçı; doğru kimlik, eksik sayfa ve izinli çalınabilirlik raporu |
| Global Pop altında Türkçe şarkılar | Kullanıcı ekranı; `AudiusSongMapper.inferCategory` fallback | `MusicGenreResolver` konservatif doğrulama + bilinmeyen kategorisi | Ceza→Türkçe Rap; bilinmeyen→Türü Belirsiz; Türkçe Rock etiketi doğru |
| Türkçe Pop/Anadolu Rock için 0 kayıt | Kullanıcı ekranı | Gerçek kategori sayısı, boş ise 'henüz indekslenmedi' | 0 kayıt nedenini açık göster, veri uydurma |
| Sosyal ve Arşiv gereksiz | Kullanıcı ekranları | Alt gezintiden çıkar, kayıtları silme | Dört sekme; eski Playlists verisi Room'da korunur |
| Room 601 / Genel katalog 583 | Kullanıcı ekranları | İki örtüşen arama modundan tek keşif aramasına geç | Çift arama kutusu yok, indekslenen sayı açık |
| YouTube'dan tam ses | Kullanıcı talebi | Sadece yasal/izinli oyuncu entegrasyonu; görünmeyen ses çekme yok | Sağlayıcı sözleşmesi + gerçek cihaz testini geçmeyen stream oynatılmaz |

## Sonraki faz — kapsamlı dünya diskografi
- **Faz 1:** Katalog metadata (sanatçı, albüm, single, feat, eser/recording), MusicBrainz/ISBN olmayan ses kaynaklarını güvenilir biçimde ayır; metadata sınırsız sayfalama ama kota kontrollü. Kaynak ve telif kaydı.
- **Faz 2:** Lisanslı full-stream sağlayıcılarının keşfi; erişilebilir değilse demo/full şarkı gibi göstermeden resmî preview.
- **Faz 3:** Sunucu tarafı indeks/önbellek, oran sınırı, gerçek geri dönüş ve 2 ayrı cihazda playback/arka plan, Android yükseltme, ücretsiz plan maliyet testi.
- **Faz 4:** CI + cihaz + hak doğrulamasından sonra yeni Android Release APK/SHA-256; önceki sürüm üzerine imza/sürüm kodu uyumlu yükseltme.

## Değişiklik durumu
Bu dosya ve kategoriler ayrı bir geliştirme dalında; CI/test sonucu ve APK teslimi doğrulanana kadar **tamamlandı** denmez.
