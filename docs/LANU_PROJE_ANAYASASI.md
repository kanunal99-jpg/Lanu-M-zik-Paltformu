# LANU Müzik — Proje Anayasası ve Teslimat Sözleşmesi

> 21 Ekim 2026. Bu belge tüm gelecek geliştirme turlarının zorunlu çalışma kurallarını tanımlar. Kanıtlanmamış başarı, cihaz uyumluluğu veya kusursuzluk iddia edilmez. Kaynak üretim standardı: [LANU_PREMIUM_PRODUCTION_STANDARD.md](LANU_PREMIUM_PRODUCTION_STANDARD.md).

## 1. Nihai amaç ve değişmez ilkeler

- Hedef **100/100 kanıtlı premium ürün kalitesi**; puan tek başına teslim kanıtı değildir.
- Proje koordinatörü / kıdemli mühendis disiplini: kök neden araştır → küçük güvenli değişiklik → test → APK build → doğrula → yayın → smoke → kanıtla.
- Hız, kullanıcı verisinin, imzanın, güvenliğin ve doğruluğun önüne geçmez. Bağımsız işler paralel ele alınabilir; aynı dosyaya rakip PR açılmaz.
- Kullanıcı istemese bile gerekli ürün ve güvenlik gereksinimleri tespit edilir, gerekçelendirilir, uygulanır ve test edilir; belirsiz veya riskli üretim işlemleri izinsiz yapılmaz.
- **Sıfır hata garantisi verilmez**; riskler, açık kabul kapıları ve gerçek test sonuçları açıkça bildirilir.
- Mevcut kullanıcı uygulaması kaldırılmaz; hesap, çalma listesi, favori, geçmiş, Room veya indirilen medya verileri temizlenmez. Veri kaybı yaratabilecek işlem açık izin ve yedekleme/geri dönüş planı gerektirir.
- Ücretli hizmet, lisans, hesap veya dış sistem kaydı açık onay olmadan açılmaz. Keystore, API anahtarı ve kişisel bilgiler sohbete, issue'ya veya commit'e yazılmaz.

## 2. Her tur zorunlu rapor: Mevcut Durum → Test Sonrası → Hedef

Her **Çalış / Devam** turunda alan bazında **Mevcut | Test Sonrası | Hedef** puanları 0–100 olarak kalem kalem raporlanır. Başlıklar zorunludur: **Mevcut Durum**, **Test Sonuçları / Test Sonrası**, **Hedefler**, **Yapılan İşlemler ve Kanıt**, **Engeller / Sonraki Kapı**.

Ayrı takip alanları: mimari/Clean/MVVM/Hilt; Compose UI/UX/TR-EN/a11y; müzik katalog/lisans/oynatma; offline/Room/migrasyon; kimlik/oturum; ağ/timeout/retry/fallback; performans; unit/instrumentation/Robolectric; lint/R8/CVE; güvenlik; CI/CD; APK/release/SHA256; kalıcı imza; OTA; Samsung A55/Xiaomi/Huawei GMS-free/tablet; Crashlytics opt-in; telif/API kısıtları.

**Genel kalite** ve **üretim hazırlığı** ayrı puanlanır. CI bekleniyorsa test sonrası puanı artırılmaz. Tahmini puanlar ölçüm gibi sunulmaz; her artış kanıta dayanır. Eski statüden yeni başarı varsayılmaz.

## 3. Her turda canlı GitHub doğrulama

1. Canlı main SHA, açık PR numarası/head SHA, mergeability ve merge durumunu yeniden al.
2. GitHub Actions workflow **run/job/step** durumlarını ve gerektiğinde hata loglarını oku; başarısızlıkta önce kök nedeni belirle.
3. Değişiklikler küçük, bağımsız, geri alınabilir PR'larla ve uygun regresyon testleriyle yapılır.
4. PR merge yalnızca ilgili HEAD için gerekli CI **success**, mergeable true ve değişiklik incelemesi sonrası; mümkünse squash + expected head SHA.
5. Merge sonrası main SHA, yeni main CI ve yayın job'larını yeniden doğrula. PR CI başarısı, main yayın başarısı değildir.
6. Çalışan testler sırasında çakışmayan başka iyileştirmeler hazırlanabilir; tamamlanmamış iş başarı diye raporlanmaz.

## 4. APK üretimi, yayın ve OTA: P0 kabul sözleşmesi

- Her önemli başarılı yayınlanabilir main sürümünde mümkün olduğunda GitHub Releases altında cihazdan indirilebilir APK ve `.sha256` bulunur; dosyaların **gerçekten indirilmesi**, byte karşılaştırması ve yeniden hesaplanan SHA-256 doğrulanır.
- Debug APK açıkça **önizleme/test** olarak etiketlenir. Geçici CI R8 smoke anahtarı, üretim imzası sayılmaz.
- Signed production APK yalnızca kalıcı keystore secrets, sabit yayın sertifikası pin'i ve **kurulu cihaz sertifikasıyla eşleşme** doğrulandığında güvenli OTA olarak sunulur. Eksik/mismatched fingerprint **fail closed**.
- Paket kimliği, monoton versionCode, APK imzası, indirme SHA-256, R8 mapping ve kurulum güvenliği doğrulanır.
- OTA akışı: güncelleme kontrolü → güvenli indirme → SHA-256 ve sertifika kontrolü → Android installer → uygulamayı kaldırmadan yükseltme → Room/favoriler/çalma listeleri/geçmiş/indirilen medya bütünlüğü.
- Var olan kullanıcı cihazına test APK'sı zorla kurulmaz; ADB smoke varsayılanı read-only; özel test cihazı ve açık onay olmadan install yapılmaz.
- Keystore veya fiziksel cihaz yoksa **blokaj** olarak raporlanır; imzalı OTA başarısı iddia edilmez.

## 5. Savunma ve kullanıcı güvenliği

- Minimum izin, dışa açık bileşen/intent doğrulaması, güvenli deep link, ağ TLS/host kontrolü, kullanıcı girdisi ve dosya/URI doğrulaması, path traversal/ZIP bomb sınırları, replay/rollback/version downgrade önleme.
- Kimlik ve oturum izolasyonu, hassas verilerin güvenli saklanması, log/Crashlytics PII temizliği, güvenli hata mesajı ve kullanıcı dostu fallback.
- Bağımlılık ve supply-chain güvenliği: güncel CVE değerlendirmesi, Gradle/Actions izinleri, release provenance, checksum/signature doğrulaması ve least-privilege GitHub token.
- Üçüncü taraf katalog/streaming yalnızca lisans ve sağlayıcı koşulları uygunsa; YouTube resmi görünür oynatıcı ve API kısıtları; izinsiz ses indirme veya yanıltıcı tam akış yok.
- Kritik düzeltmeler saldırı senaryosu ve negatif testle doğrulanır; dış saldırılara karşı mutlak güvenlik garantisi verilmez.

## 6. Ürün kalitesi ve platform matrisi

- Compose/Material 3, MVVM/MVI/Clean, Hilt, Coroutines/Flow, lifecycle, offline-first, Room migrasyonları, Media3 playback, ağ timeout/retry/circuit-breaker/fallback, hata/boş/yükleniyor durumları.
- TR/EN, dark/light, ekran okuyucu/Compose semantics, büyük font, tablet/katlanabilir düzen; gerçek kullanım performansı, soğuk açılış, ANR, bellek ve pil ölçümleri.
- Samsung Galaxy A55 öncelikli; Xiaomi/Redmi/POCO, Huawei GMS-free ve diğer Android üreticileri ile tabletler. GMS yokken temel işlevler çalışmalı; HarmonyOS NEXT APK uyumluluğu varsayılmaz.
- Firebase Crashlytics yalnızca güvenli yapılandırma, opt-in ve gizlilik kabulüyle; servis yoksa uygulama çalışmaya devam eder.
- Room eski verileri ve modern silme/tombstone semantiği korunur; geriye dönük ayırt edilemeyen eski silme izlerine sahte garanti verilmez.

## 7. Yayın kabul ve iletişim

- Kritik P0 açıkken veya production signing/gerçek cihaz kanıtı eksikken **100/100** ve 'kusursuz teslim' iddiası yasaktır.
- Her test/PR/CI/release sonucunda kanıt bağlantısı, SHA ve açık engeller kaydedilir; indirilebilir APK bağlantısı mümkün olduğunda verilir.
- Yalnızca somut CI başarısızlığı, merge, yayın/APK/checksum, güvenlik veya üretim engeli değişikliğinde kısa bildirim; ayrıntılı puanlı rapor kullanıcı istediğinde her zaman sunulur.
- Otomatik izleme, otomatik kod değiştirme veya deploy yetkisi anlamına gelmez. Gelecek çalışmalar yalnızca mevcut yetkili araçlar/çalışma oturumlarıyla yapılır.
- İlgili açık kabul kapıları: [#29](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/issues/29), [#34](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/issues/34), [#89](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/issues/89).
