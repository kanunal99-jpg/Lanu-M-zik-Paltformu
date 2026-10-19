# LANU Müzik – YouTube arama ve uygulama içi oynatma

## Uygulanan
- Arama ekranındaki **YouTube** girişinden sanatçı/şarkı araması.
- **YouTube Data API v3 / search.list**: genel erişime açık, gömülebilir video sonuçları; başlık, kanal, kapak görseli ve sayfalama.
- Sonuca dokununca LANU içindeki **resmî, görünür YouTube iframe** oynatıcısı açılır.
- Eski **YouTube video URL/ID** yapıştırma seçeneği korunur ve API anahtarı olmadan kullanılabilir.
- Android WebView oynatıcısında uygulama kimliğini taşıyan Referer, API aramasında Android paket adı ve imza SHA-1 başlıkları gönderilir.
- Arama anahtarı/kota/ağ ve boş sonuç durumları gösterilir. Mevcut LANU ses oynatımı YouTube penceresi açılırken duraklatılır; pencere arka plana düşerse WebView durdurulur.

## API anahtarı nasıl tanımlanır?
1. Google Cloud Console'da YouTube Data API v3'ü etkinleştirin.
2. Uygulama anahtarını oluşturun ve mümkünse **YouTube Data API v3** kullanımıyla sınırlandırın. Android uygulaması sınırlandırması için LANU paket adı **com.aistudio.lanumusic.kpxzqa**, dağıttığınız APK'nın imza SHA-1 parmak izi gereklidir.
3. GitHub deposunda Settings → Secrets and variables → Actions → New repository secret yoluyla **YOUTUBE_DATA_API_KEY** adında secret girin. GitHub Actions derlemesinde bu anahtar okunur; kaynağa yazılmaz.
4. Secret olmadan kurulmuş APK'da arama penceresindeki API anahtarı alanına kendi Google Cloud anahtarınızı **oturumluk** girebilirsiniz. Değer cihazda kalıcı saklanmaz. Bu yol geliştirme/test içindir.
5. GitHub Actions'a ait debug APK'nın imza sertifikası, kullanıcı cihazındaki üretim sertifikasından farklı olabilir. Android kısıtlı anahtarları gerçek APK imzasıyla eşleştirin.
6. Yayımlanan APK içerisinde BuildConfig sabiti olarak anahtar kullanımı **anahtarı gizli tutmaz**; API kısıtlamalarını, kullanım kotalarını ve kötüye kullanım izlemeyi zorunlu kabul edin. Yüksek kullanımlı genel dağıtım için kimlik doğrulamalı, kota kontrollü bir backend önerilir.

## Sınırlar
- İndirme, DRM/reklam atlatma, videodan ses çıkarma, arka plan YouTube dinletimi **yok**.
- YouTube genelinde **arama** sunulur; her video müzik değildir ve hiçbir API YouTube üzerindeki her videoyu tek seferde listelemeyi veya her videonun oynatılmasını garanti etmez.
- YouTube hakları, ülke/yaş sınırlamaları, embed kapatma ve kota ayarları uygulanmaya devam eder.
- API anahtarı tanımlanmayan cihazda YouTube'dan otomatik arama **çalışmaz**; mevcut bağlantı yapıştırma ile gömme yöntemi çalışır.
- Çalışır kabulü için Android fiziksel cihazda video yüklenmesi, ilk/sonraki sayfa araması, kapanınca sesin durması, mevcut kullanıcı verilerinin korunması ve imzalı APK yükseltme testi gerekir.

## Test
- YouTubeSearchClientTest: güvenli URL kodlama, filtreler, sayfalama, bozuk sonuçların ayıklanması, tekrarlı kayıtlar, kota ve anahtar hataları.
- YouTubeVideoLinkTest: yalnız güvenilir YouTube HTTPS adresleri ve video kimliği.
- GitHub Actions Android: Robolectric ve debug APK derlemesi; ana dalda başarılı olduğunda Release alanına APK ekler.

## Resmî belgeler
- https://developers.google.com/youtube/v3/docs/search/list
- https://developers.google.com/youtube/terms/required-minimum-functionality
- https://developers.google.com/youtube/terms/developer-policies
- https://docs.cloud.google.com/docs/authentication/api-keys
