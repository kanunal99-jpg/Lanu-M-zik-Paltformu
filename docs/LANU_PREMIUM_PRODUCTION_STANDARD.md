# LANU Premium Professional — Android Üretim Standardı

Bu dosya kullanıcı tarafından 20.10.2026 tarihinde paylaşılan **LANU PREMIUM PROFESSIONAL — ANDROID YAZILIM GELİŞTİRME MASTER PROMPT** belgesindeki kabul kriterlerini bu repo için uygulanabilir hale getirir. Kaynak belgenin tüm iddialarının kodda tamamlandığı anlamına gelmez.

## Zorunlu prensipler

1. **Mimari:** MVVM veya MVI, Compose/Material 3, ayrıştırılmış UI–domain–data, gerektiğinde Repository Pattern ve Hilt; kontrollü ve geriye uyumlu refactoring.
2. **Tasarım:** Türkçe–İngilizce Android string kaynakları, dark/light, erişilebilirlik, uyarlanan ekran boyutları, loading/error/empty/success durumları.
3. **İş parçacığı:** Coroutine, Flow, lifecycle-aware collect; ağ/dosya/DB işlemleri ana thread'i bloke edemez; uygun yerlerde WorkManager.
4. **Hata güvenliği:** timeout, retry, fallback, güvenli varsayılan, log ve toparlama; hassas bilgi loglanamaz.
5. **Üretim güvenliği:** HTTPS, minimum izin, Android Keystore ile hassas veriler, dependency audit, R8/ProGuard. APK içinde saklanan herhangi bir API anahtarı **gizli anahtar sayılmaz**; kullanıcıya sunulan API anahtarları sağlayıcıda Android paket+sertifika+API kapsamına sınırlandırılmalıdır.
6. **Veri:** Room migrations, offline-first uygun alanlarda, favori/müzik/playlist verileri korunur. Otomatik uygulama kaldırma yasaktır.
7. **OTA:** Resmî sürüm seçimi, versionCode, signed APK+checksum, SHA-256, paket adı ve kurulu imza uyumu, kullanıcı onaylı Android installer; kesinti/iptal/kurtarma, debug-only OTA yasak.
8. **Yayın:** Build+test+lint+R8 smoke+imzalı release, GitHub asset SHA-256 geri okuma ve release indirme doğrulaması; gerçek cihaza kurulabilirlik ayrı bir kabul kapısıdır.
9. **Google Play:** Yayına çıkış istenene kadar yayın yok; geleceğe dönük AAB/izin/target SDK uyumluluğu değerlendirilir.
10. **İzleme:** Firebase Crashlytics için sağlayıcı konfigürasyonu, sürüm haritaları ve gerçeğe dayalı durum raporları gerekir.
11. **Test:** Unit, Robolectric, Compose, DB migration, network/offline, security, regressions ve gerçek cihaz smoke testleri risklere uygun oranlarda.
12. **Geliştirme döngüsü:** Araştır → analiz → kök neden → plan → kod → build → test → doğrula → release → smoke test → rapor.

## Üretim durum matrisi (2026-10-20)

| Kontrol | Durum | Kabul kanıtı |
|---|---|---|
| Compose ve MVVM | Kısmen mevcut | Compose UI, MainViewModel var; tam clean architecture ve Hilt geçişi denetlenmeli |
| Hilt DI | Kısmen entegre | PR #46 ile AGP9 uyumlu Hilt application/activity/OTA ViewModel ve temel provider kayıtları eklendi; tam MainViewModel constructor ve repository ayrımı açık |
| OTA signed release doğrulaması | Kod var; üretim engelli | GitHub API + APK SHA-256 ve imza kontrolü var; son Release Build 366 yalnız app-debug.apk. Kurulu cihaz fingerprint, kalıcı signing secrets ve silmeden yükseltme kabulü eksik |
| R8 + kaynak küçültme | Bu pakette etkin | CI release smoke ve mapping çıktısı başarılı olmadan kabul yok |
| TR/EN OTA metinleri | OTA ekranında var, uygulamada kısmi | PR #45 OTA erişilebilirliği ve gezinme iyileştirmeleri; tam UI çevirisi ayrıca yapılmalı |
| Sürüm notları | Bu pakette eklendi | GitHub release body üzerinden gösterilir |
| Lint + birim testleri | Birleşik main CI #366 başarılı | https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/39411353619; fiziksel cihaz ve canlı YouTube yetkisi kanıtı değildir |
| Crashlytics | Eksik entegrasyon/kurulum incelemesi | google-services.json ve güvenli Firebase setup gerekir |
| Android fiziksel cihaz testi | Bekliyor | Debug ile release imzalı APK geçişi, yükleme ve uygulama verisi gözlenmeli |
| Play/AAB uyumluluğu | Yayından önce doğrulanmalı | Şimdi Play'de yayın yapılmaz |

## Sert durdurma kuralları

- Android'e sessiz APK yükleme, DRM aşma, uygulamayı kaldırarak kullanıcı verisini riske atma ve farklı imzalı APK'yı yükseltme olarak sunma yapılmaz.
- Build/test yapılmadıysa **başarılı** yazılmaz. Debug release, OTA uyumlu imzalı production release sayılmaz.
- Firebase hesabı, imza keystore'u veya gerçek cihaz erişimi yoksa ilgili kalem **tamamlanmadı** kalır.
- Release var ama kullanıcı telefonuna mevcut uygulama üzerine kurulabilirliği doğrulanmadıysa üretim kabulü tamamlanmış sayılmaz.

## Sıradaki proje-geneli geçişler

* Hilt + repository ayrıştırmasını risk bazlı modüllere bölerek gerçekleştirmek.
* Crashlytics ve Firebase kaynaklarını güvenle bağlamak; release mapping upload.
* Tüm UI ekranlarının TR/EN kaynak dosyalarına taşınması ve accessibility taraması.
* Room migration/device instrumentation/Compose UI testleri.
* Sabit signing key ve önceki kurulu APK sertifika uyumluluğu: tamamlanıncaya kadar OTA production bloke kalır.

## Güncel ilerleme defteri

P0/P1/P2 PR ve release durumları, açık kabul engelleri ve 55/100 mevcut → 92/100 koşullu hedef puanlaması **[PROJE_ANAYASASI_VE_DUZELTILECEKLER.md](../PROJE_ANAYASASI_VE_DUZELTILECEKLER.md)** içinde 20.10.2026 tarihli bölümde takip edilir. Tarihsel tablolar tamamlanma kanıtı değildir.
