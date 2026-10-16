# LANU Müzik — Dağıtım ve kritik hata durumları

## 16.10.2026 düzeltmeleri
- Audius aramasının ilk sayfası bütün sanatçı diskografisini beklemez. Sanatçı kimliği doğrulanmış diskografi yüklemesi aramadan bağımsız yapılır.
- Audius bir sayfayı tekrar tekrar gönderdiğinde tarama yeni şarkı ID'si kalmadığında durur. Maksimum 100 sayfa koruması uygulanır; bu sınır tüm dünya kataloğunu kapsamayı **garanti etmez**.
- İptal edilen ağ/arama isteği genel sağlayıcı hatası diye yutulmaz.
- Android versionCode CI run numarasından monoton artar; bu **debug APK'nın imza farklılığını tek başına çözmez**.
- Mevcut `MusicRepository.getArtistDiscography` yöntemi başarılı sonuçları zaten önbelleğe ekliyor. Önceki "hiç eklenmiyor" teşhisi genelleme olarak doğru değildi; uzun sorgu/timeout ve sağlayıcı kapsamı hâlen ayrı sorunlar.

## Üretime geçişin zorunlu koşulları
1. Sürüm imzalama: Aynı release keystore ile imzalanan paket ve sertifika parmak izi kontrollü olmalı. Şu an GitHub Actions **debug APK** üretir; cihazda önceki debug sürümün üzerine sorunsuz kurulum garanti değil. Mevcut APK'yı/saklanan verileri yedeklemeden kaldırma.
2. Gerçek cihaz: Android sürümü, alt menü, tür filtreleri, Ceza/Tarkan/Sezen diskografisi, Wi-Fi/5G, oynatma, arka plan, izin retleri ve mevcut kurulumun üzerine yükseltme ayrı test edilmeli.
3. Lisans: MusicBrainz katalog metadata sağlar, ses vermez. YouTube video sesini görünmez arka plan kaynağına dönüştürme yapılmaz. İzinli full-stream ve resmî önizleme UI'da ayrı işaretlenir.
4. İçerik kapsamı: 500/600 gibi değerler yalnız bu cihazın indeksindeki kayıt sayısıdır; dünyadaki toplam şarkı sayısı değildir. Bütün müzik kayıtlarına sınırsız erişim için yetkili kaynak ve uygulanabilir lisanslar gerekir; bunlar henüz sağlanmadı.
5. Yayın kapısı: CI success + artifact SHA + GitHub Release + fiziksel cihaz smoke tamamlanmadan ürün "tamamen hatasız" kabul edilmez.

## GitHub işler
- [#18 kullanıcıya görünen regresyonlar](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/issues/18)
- [#20 dünya diskografisi ve lisanslı ses](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/issues/20)

**Durum:** İlk kod ve CI düzeltmesi. Lisanslı dünya çapı tam müzik kataloğu ile güvenli, kalıcı imzalı Android dağıtımı **açık**.
