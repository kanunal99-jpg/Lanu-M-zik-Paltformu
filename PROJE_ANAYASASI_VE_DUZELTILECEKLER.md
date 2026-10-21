# ZORUNLU CANLI RAPOR FORMATI — 21.10.2026

**Kullanıcı talebi:** LANU Müzik için HER geliştirme/görev yanıtında aşağıdaki başlıklar ve kalem kalem mevcut puan ile 100/100 hedefi gösterilir. Bu biçim ilerleme raporlarında ve saatlik otomatik çalışmanın anlamlı bildirimlerinde de geçerlidir.

1. **Mevcut Durum:** En güncel doğrulanmış `main` commit SHA, tamamlanan/bekleyen PR'lar, son GitHub Release ve APK türü. 'Önceki birleşik main CI başarılı' ile 'yeni main CI bekliyor' asla karıştırılmaz.
2. **Test Sonuçları:** Son main run numarası, durumu ve linki; ilgili PR run numarası, sonucu; canlı sanatçı/şarkı doğrulama, Android Lint, Unit/Robolectric, debug APK, R8, SHA-256, varsa gerçek cihaz, imzalı üretim işinin **gerçekte skipped olup olmadığı** ayrı satırlar. Beklemede/başarısız/iptal ayrı raporlanır.
3. **Puanlama:** Her zaman **kalem/alan**, `mevcut puan / alan azamisi`, `alan hedefi`, `fark`, `kanıt` ve `açık engel` içeren tablo; güncel toplam /100, hedef 100/100, kalan fark, eski→yeni değişim. Tahmini/provizyonel puan kesin CI sonucundan farklı etiketlenir. Üretim kapıları eksikken 100 yazılmaz.
4. **Hedefler:** P0/P1 öncelikli somut eksikler, her biri için doğrulanabilir kabul testi ve puan katkısı, kim/ne dış bağımlılık sağlıyor açıkça belirtilir.
5. **Devam Eden Çalışmalar:** Bu turda gerçekten yazılan dosyalar/PR/commit, doğrulanmış test sonucu, merge ve Release asset bağlantısı; sıradaki güvenli işlem. 'Çalışıyorum' sözü kendi başına test kanıtı değildir.

### Güncel skor kartı (kanıta dayalı editoryal değerlendirme, 21 Ekim 2026)

| Alan | Mevcut / azami | Hedef | Eksik | Gerekçe / açık kabul |
|---|---:|---:|---:|---|
| CI ve otomatik test | 24 / 25 | 25 | 1 | #491 ve #492 PR testleri tamamen başarılı; yeni birleşik main #494 henüz tamamlanmadı |
| Veri güvenliği ve çevrimdışı kullanıcı arşivi | 21 / 25 | 25 | 4 | [#93](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/93) veri göçü güvenlik testi ve merge; gerçek kurulu sürümden no-uninstall veri retention doğrulaması eksik |
| Müzik kataloğu ve işlevler | 15 / 20 | 20 | 5 | CI canlı 10 sanatçı / 20 parça doğrulaması başarılı; tüm katalog lisansı/diskografi/kullanıcı playback/E2E ayrıca gerekli |
| Premium mimari ve UX | 10 / 15 | 15 | 5 | Hilt/Clean, tam TR/EN erişilebilirlik ve opt-in Crashlytics dashboard/R8 mapping üretim kabulü açık |
| APK/OTA üretim ve gerçek cihaz kabulü | 5 / 15 | 15 | 10 | [#91](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/91) kurulu APK sertifika gate'i ve [#94](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/94) OTA kurtarma merge edildi; gerçek kalıcı signing secrets, sertifika eşitliği, signed APK, fiziksel OEM testleri yok |
| **Toplam** | **75 / 100** | **100 / 100** | **25** | CI/PR başarısı gerçek üretim kabulü değildir |

**Son doğrulanmış yayın:** [Build 489 (yalnız debug APK + SHA-256)](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/releases/tag/lanu-music-489-b627c0e69d5fd59cbe5e9b65db070298ff51245f). [main #494](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/39627959922) bitmeden #93 + #94 birleşiminin yayınlandığı iddia edilemez.

**P0/P1 100 puan kabul kapıları:** (i) tam birleşik `main` CI, (ii) eski sürüm + yeni imzalı sürüm **aynı kurulu sertifika** ve artan versionCode ile uygulamayı kaldırmadan yükseltme, kullanıcı ID/Room/favori/listeler/offline dosyalar bire bir karşılaştırmalı koruma, (iii) Samsung A55 ve en az bir Xiaomi ve GMS'siz Huawei/Android OEM, tablet, API seviyesi matrisi, (iv) Firebase Crashlytics opt-in test crash+dashboard+mappings, (v) tam Clean/MVVM/Hilt, TR/EN TalkBack/a11y, (vi) telif/lisans/API yetkisi + gerçek ses kataloğu kabulü. Gizli keystore/secrets sohbet/issue içine yazılmaz; eksik dış bağımlılıklar test edilmiş gibi işaretlenmez.

Tarihsel skorlar (55/70/72 veya önceki 92 hedefi) geçmiş değerlendirmedir; **geçerli yeni hedef 100/100**. Bu skor yalnız kanıt geldikçe, aynı alan ağırlıkları korunarak güncellenir.

---

# CANLI KANIT ÖZETİ — 21.10.2026

**Bu bölüm önceki tarihsel durum tablolarının üstündedir ve 21 Ekim itibarıyla doğrulanmış commit/CI kanıtını gösterir.** CI yeşil olması yalnızca CI'de yürütülen testlerin başarılı olduğuna işaret eder; GMS'siz Huawei, Samsung, Xiaomi, tablet veya fiziksel cihaz kabulünün yerine geçmez.

| İş | Kanıt | 21.10.2026 statüsü |
|---|---|---|
| P1 bozuk oynatıcı tercihlerini güvenli geri yükleme | [PR #79](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/79) | main'de; birleştirildi |
| P0 bozuk yerel arşiv yüklemesinde oturum izolasyonu | [PR #80](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/80) | main'de; birleştirildi |
| P0 çevrimdışı dosya silme/indirme yarışı ve Room kaydı koruması | [PR #82](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/82), [PR CI #462](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/39539651067), commit `8241616` | main'e birleştirildi; PR lint/unit/Robolectric/R8 başarılı; birleşik main CI #465 sonuç bekliyor |
| P0 OTA SharedPreferences tip bozulmasında cache ve kullanıcı verilerini koruma | [PR #81](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/81), [PR CI #461](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/39539141839), commit `eadc129` | main'e birleştirildi; PR kalite adımları başarılı; birleşik main CI bekliyor |
| Android test runtime: Roborazzi güncellemesi / Robolectric 4.16.1 uyumu | [PR #36](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/36), [PR CI #453](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/39537706181), commit `02eb4b2` | main'e birleştirildi; PR'daki Unit/Robolectric adımı başarılı; birleşik main CI bekliyor |
| P0 kullanıcı tarafından istenen silmede, dosyalar zaten yoksa Room'da takılı kalan kayıt | [PR #83](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/83), [PR CI #466](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/39546153802) | ayrı PR açıldı; 3 regresyon testi eklendi, **CI ve merge henüz onaylanmadı** |
| Son önceden doğrulanmış ana dal paketi | [main CI #464](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/39539757254), [GitHub Build 464](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/releases/tag/lanu-music-464-6bc361b8c6729436655f4f4603a95ff745ef6226) | CI başarılı; **indirilebilir app-debug.apk ve SHA-256 var**; imzalı production OTA değildir |
| Kalıcı signing / production APK | [main CI #464 signed job](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/39539757254), [Issue #34](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/issues/34) | Job *success* görünse de secret'lar eksik olduğundan signed build, fingerprint ve upload adımları **SKIPPED**; production release **YOK** |

### Değişmez sonraki işler (paralel kontrol listesi)
1. **P0 testlerin birleşik hali:** main CI #465 ve sonraki birleşik push çalışmaları ile #83 PR lint, bütün Unit/Robolectric, R8 ve APK smoke sonuçlarını doğrula; başarısız olursa logla kök nedeni gider, CI yeşil olmadan #83'ü birleştirme.
2. **P0 imzalı yayımlama:** kurulu cihaz paket/sertifika SHA-256 fingerprint'ini yetkili olarak doğrula, aynı sertifikaya sahip kalıcı signing keystore'u sadece GitHub secrets üzerinden kullan; signature pin, artan versionCode, APK SHA ve uzaktan GitHub release asset digest'i doğrulanmadan production OTA yayınlama. **Kurulu uygulama asla silinmez.**
3. **P0 Android çeşitliliği:** API 24/29/33/35/36 ve mümkünse 37, Samsung/Xiaomi/Huawei APK-uyumlu GMS'siz cihaz, telefon/tablet/katlanır ile yerel kimlik, çalma listesi, favoriler, MediaStore ve Room veri bütünlüğü; salt statik OEM kontrolünü gerçek cihaz testi sanma.
4. **P1 mimari / kalite:** MainViewModel Hilt+Clean Architecture, Crashlytics opt-in ve R8 mapping, TR/EN, TalkBack ve düşük RAM/yavaş ağ testlerini izole PR/test kabulü ile sürdür.
5. **P0 gerçek servis/telif:** Audius canlı smoke kapsamını sürdür, YouTube ve Jamendo erişim/lisans/anahtar kısıtları resmî olarak kanıtlanmadan içeriği açık veya hazır gösterme.

**Puanlama:** Eski 55/100 ihtiyatlı üretim kabul başlangıcı ve 92/100 hedef korunur; yalnızca CI başarısından 90+ üretim puanı türetilmez. Puan değişikliği için yazılım, cihaz, imza, servis ve release kanıtı ayrı ayrı gerekir. İlgili kök takip: [#34](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/issues/34).

---

# PROJE ANAYASASI — 15.10.2026

Bu belge mevcut ANAYASA.md, docs/ANAYASA.md, AGENTS.md ve proje belgelerini **silmez veya geçersiz kılmaz**; çelişkide daha güvenli/katı kural uygulanır, karar ADR'de belgelenir.

## SON KANIT TABLOSU — 20.10.2026, EŞZAMANLI GELİŞTİRMELER

**Bu bölüm eski tarihsel PR listelerine göre önceliklidir; eski tablolar geçmişi kaydetmek için korunur.**

| Çalışma | Kanıt | Gerçek durum |
|---|---|---|
| P0 kullanıcı arşivi/yerel kimlik/veri koruma | [#56](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/56) ve [#57](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/57), başarılı CI #392/#394 | **main'de** |
| P0 başarısız indirmede eski dosya korunumu | [#59](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/59), başarılı CI #399 | **main'de** |
| P1 repository yaşam döngüsü | [#58](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/58), başarılı CI #400 | **main'de** |
| Android API/OEM uyumluluk anayasası ve 24/29/33/35 emülasyon | [#60](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/60), başarılı CI #405 | **main'de**; Samsung/Xiaomi/Huawei fiziksel cihaz onayı YOK |
| Android medya izni, çevrimdışı güvenli dosya adı | [#61](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/61), CI #407; [#62](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/62), CI #408 | **main'de**, CI başarılı |
| P0 startup silme yerine offline audit | [#63](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/63), CI #410 | **main'de**; gerçek cihazda dosya koruma smoke ayrıca bekliyor |
| P1 GMS'siz Android WebView yedeği | [#64](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/64), CI #411 | **main'de**, gerçek Huawei/AOSP test edilmedi |
| P1 Firebase Crashlytics opsiyonel, varsayılan telemetri kapalı | [#65](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/65), CI #413 | **main'de**, **Firebase dashboard/test crash/mapping ve kullanıcı onayı henüz yok** |
| P0 OTA oturum sürüm/link/hash tutarlılığı ve kalıcı kayıt | [#66](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/66), CI #418 | **main'de**, fiziksel cihaz silmeden OTA hâlâ bekliyor |
| P0 aynı şarkı için indirme yarışını önleme | [#67](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/67), CI #420 | **CI/merge kabulü sürüyor**, tamamlandı sayılmaz |
| P1 Android API24+ HTTPS-only | [#68](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/68), [CI #421 başarılı](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/39436396865) | **main'de** |
| P1 farklı OEM MediaStore kolon toleransı | [#69](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/69), [CI #423 başarılı](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/39436646763) | **main'de** |
| P2 ilk açılışta gereksiz bildirim izni istememe | [#70](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/70), [CI #424 başarılı](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/39436931965) | **main'de** |

**Puan kuralı:** Tarihsel 55/100 başlangıç, son **kanıta dayalı yaklaşık 70/100 yazılım/CI mühendislik hazırlığı** ve **92/100 üretim hedefi** farklı ölçümlerdir. Açık PR'lar tamamlanmış kabul edilerek puan yükseltilmez. Üretim alanında hâlâ eksik olanlar: **uyumlu kalıcı signing keystore + kurulu APK SHA-256 sertifika eşleşmesi, signed release APK/hash, farklı Android OEM cihazlarda UYGULAMAYI SİLMEDEN yerel kimlik/Room/favori/playlist/çevrimdışı ses korunumu, gerçek YouTube Data API yetkisi ve Firebase Crashlytics opt-in + dashboard + R8 mapping**. Bu engeller kapanmadan 92/100 veya production-ready etiketi kullanılmaz.

---
## GÜNCEL PLATFORM HEDEFİ — 20.10.2026: TÜM ANDROID/APK UYUMLU OEM'LER

**Tek marka hedefi YOK.** Önceki metinlerdeki Samsung Galaxy A55 adı yalnızca tarihsel/test cihazı örneğidir, ürün kapsamını daraltmaz. LANU Müzik, Android/API uyumlu **Samsung, Xiaomi/Redmi/POCO, Huawei Android/EMUI veya APK çalıştırabilen HarmonyOS, Honor, Oppo, Vivo, Realme, OnePlus, Motorola, Google Pixel, HMD/Nokia ve diğer üreticilerin telefon/tablet/katlanabilirlerini** hedefler. **Android APK çalıştırmayan HarmonyOS NEXT/5+** sürümleri bu APK ürününün hedefi değildir; doğal HarmonyOS app ayrıdır.

- Mevcut `minSdk 24` (Android 7+), `targetSdk 36`, `compileSdk 36.1`. Android 17/API37 ileriye dönük çalışma, tablet 600dp+, ekran döndürme, çok pencere, erişilebilirlik, pil ve GMS olmayan Android testleri **kabul kapısıdır**, henüz hepsinde geçildi denmez.
- Her değişiklik Android platform API ve özellik kontrolü üzerinden tasarlanır; `Build.MANUFACTURER` ile marka ayırarak kullanıcıya daha kötü davranış sunulmaz. Google Mobile Services zorunlu tutulmaz; yerel kullanıcı kimliği/çalma listesi/MediaStore/Media3/Room, GMS bulunmadığında da çalışmalı; Firebase/YouTube erişilemiyorsa güvenli düşüş gerekir. Gerçek cihaz testleri henüz gerekli.
- Üretim veri-kayıpsız OTA, farklı üretici örnekleri + GMS'siz Android + tablet matrisinde sınanır. **Hiçbir cihazda uygulama silerek yükseltme yok**; imza/sertifika uyuşmazsa bloke edilir.
- Kabul matrisi, salt okunur ADB tanılama ve CI üretici bağımsız manifest kontrolü: **[docs/ANDROID_OEM_UYUMLULUK_MATRISI.md](docs/ANDROID_OEM_UYUMLULUK_MATRISI.md)**.
- Test dışı tüm üretici/modeller için **doğrulanmış destek iddiası yoktur**. Tek test cihazıyla 92/100 üretim puanı verilmez.

---

## EK DURUM 20.10.2026 — PR #54, #55 birleştirme ve P0 #56 veri bütünlüğü

Bu bölüm üstündeki eski snapshot tablolarını günceller. **PR açılması ve kod oluşturulması üretim kabulü değildir**; CI, gerçek cihaz ve servis koşulları ayrı ayrı kanıtlanır.

| İş | Otomatik kanıt ve mevcut durum | Hâlâ açık sınırlar |
|---|---|---|
| P0 imza sertifikası parmak izi ekranı | [PR #54](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/54) main'e **birleştirildi**, [CI #387 başarılı](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/39419003322) | SHA-1/SHA-256 görüntülenmesi keystore özel anahtarı değildir; Android OEM test cihazlarında sertifika ve kalıcı signing key eşleştirme gerekli |
| P1 bozuk Room tür alanı | [PR #55](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/55) main'e **birleştirildi**, [CI #388 başarılı](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/39419355673) | Yalnızca okuma sırasındaki tür fallback'i; tablo/veri silinmedi |
| P0 yerel kullanıcı arşivi yarış/veri korunması | [PR #56](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/56): uygulama içindeki farklı LocalUserLibraryBackend örneklerine ortak işlem kilidi, diske yazma onayı, bozuk JSON'u hata göstererek koruma; paralel favori/playlist ve bozuk kayıt regresyonları | **CI ve gerçek cihaz kabulü bekleniyor.** İşlem kilidi tek Android süreç kapsamındadır; bu PR kalıcı çoklu süreç / bulut senkronizasyonu garantisi değildir |
| En son birleşik anayasa güncellemesi | [PR #53](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/53) main'e birleştirildi, [CI #380 başarılı](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/39417286720) | Tarihsel tablolardaki önceki PR açık/CI bekliyor satırları bu güncel kanıtlara göre değerlendirilir |

**Sert üretim engelleri değişmedi:** Mevcut cihazdaki APK **kaldırılmaz**. Aynı paket adı + imza sertifikası, kalıcı yetkili keystore, GitHub signed `app-release.apk` + SHA-256, artan versionCode ve seçilmiş Samsung/Xiaomi/Huawei Android/EMUI dahil fiziksel Android cihazlarda yükleme sırasında yerel kimlik/favori/playlist/Room bütünlüğü doğrulanmadan OTA yayına hazır sayılamaz. Google YouTube Data API gerçek anahtar/kota/gömülü oynatma ve Firebase Crashlytics proje erişimi de doğrulanmalı. Debug Build, imzalı production OTA değildir.

**Puan etiketi:** Önceki **55/100** ihtiyatlı temel notu ayrı bir kanıtlı yeniden değerlendirme yapılıncaya kadar korunur; **92/100** hedef ve 90+ etiketi üretim engelleri kalkana kadar sadece hedef olarak tutulur.

---

## 20 Ekim 2026 — CANLI DURUM, TAMAMLANANLAR, AÇIK İŞLER VE HEDEF PUANLAMA

> Bu tarihli bölüm **güncel durumun tek özetidir**; alttaki 15 Ekim tarihli denetim bulguları tarihsel kayıt olarak korunur. PR açılması = kod yazıldı; CI yeşil = sadece kapsanan kontroller geçti; fiziksel cihaz doğrulaması olmadan üretim kabulü yoktur. Her yeni denetimde bu tablonun tarihini, kanıt URL'lerini ve durumunu güncelleyin.

### Değişmez ürün hedefi ve güvenlik anayasası
- LANU Müzik: Android uyumlu OEM telefon, tablet ve katlanabilirler için güvenli, gerçek katalog/izinli ses kullanan, Compose + MVVM/MVI + Hilt ile geliştirilen, çevrimdışı müziği ve oynatma listelerini koruyan profesyonel müzik uygulaması.
- **Mevcut kurulu APK'yı otomatik veya talimatla kaldırma yok.** Aynı paket adı (`com.aistudio.lanumusic.kpxzqa`), doğrulanmış mevcut imza, artan versionCode, SHA-256 ve Android sistem kurulumu sağlanmadan "veri kayıpsız OTA hazır" demeyin. Debug APK yalnızca test/önizlemedir.
- YouTube: resmî Data API arama + görünür gömülü oynatıcı; indirme, ses ayıklama, DRM veya arka plan yasağını aşmak yasak. Kullanıcıya sunulan API anahtarı gizli kabul edilmez; anahtar sınırı/kota uygulanır.
- İndirme/OTA önbelleği temizliği **hiçbir** Room veritabanını, favoriyi, yerel kimliği, playlisti veya MediaStore dosyasını silemez.
- Üretim güvenliği: sessiz kurulum, sırların repoya yazılması, lisanssız ses ve test sonucu olmadan "tamamlandı" iddiası yasak.
- İş sırası: Araştır → kök neden → küçük dal/PR → otomatik test/lint/R8 → bağımsız regresyon → gerekli dış erişimler → fiziksel cihaz → yayın ve indirilebilirlik → kanıtlı kapatma. P0 tamamlanmadan production-ready etiketi kullanılmaz.

### Yapılanlar (kanıtlı durum)
| Kimlik | Değişiklik | GitHub kanıtı | 20.10.2026 durumu |
|---|---|---|---|
| P0-01 | Android yedek/aktar kuralları için müzik DB, yerel kimlik ve tercihlere izin listesi; Room JSON şema çıktısı ve regresyon testleri | [PR #44](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/44) | MAIN'E BİRLEŞTİ; gerçek cihaz/migration matrisi açık |
| P1-01 | Hilt DI başlangıcı, UpdateViewModel, yaşam döngüsüne duyarlı StateFlow tüketimi | [PR #46](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/46) | MAIN'E BİRLEŞTİ; tam MainViewModel geçişi açık |
| P2-01 | Kitaplığım gerçek arşive bağlı, Arkadaşlar ekranına erişim, küçük ekran ve OTA anonsları | [PR #45](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/45) | MAIN'E BİRLEŞTİ; arkadaşlar henüz yerel önizleme |
| QA-01 | Üç paketin birleşik ana dal CI doğrulaması | [#366 BAŞARILI](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/39411353619) | BAŞARILI CI; fiziksel cihaz sonucu değildir |
| P0-02 | YouTube Data API yanıtı için üst bellek sınırı | [PR #47](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/47), [CI #369](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/39414638340) | PR AÇIK, CI SONUCU HENÜZ DOĞRULANMADI; main'e dahil değil |
| P0-08 | Mevcut Room v3→4→5 SQL yükseltmesinin eski favori, playlist/üyelik, cache ve varsayılan metadata alanlarını koruduğu Robolectric smoke testleri | [PR #49](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/49), [CI #371](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/39414966585) | PR AÇIK; CI BEKLİYOR, tarihsel tam şema fixture ve telefon yükseltmesi ayrıca açık |
| REL-01 | [Build 366](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/releases/tag/lanu-music-366-08589306085723ff731f2110d6f857d681b7590d) GitHub Release'te app-debug.apk ve SHA dosyası | Release asset kaydı | YAYIMLANMIŞ DEBUG; imzalı production OTA değildir |

**Tarihsel düzeltme:** Alttaki 15.10.2026 "82 testten 1 başarısız" bulgusu o tarihin snapshot'ıdır; 20.10.2026 birleşik main #366 CI başarılıdır. "Son Build 348/364" eski gözlemlerini yeni Build 366 doğrulaması geçersiz kılar. Önceki raporlar silinmez.

### Açık kabul kapıları ve hedef işler
| ID | Öncelik | Geliştirme / kök risk | Bitti sayılma kanıtı | Durum |
|---|---|---|---|---|
| P0-03 | P0 | Kurulu APK sertifikası ve kalıcı signing secrets + SHA-256 fingerprint pinleme | Kurulu cihaz fingerprint'iyle eşleşen app-release.apk; GitHub asset/digest; secrets asla repoda yok | BLOKER — cihaz + yetkili anahtar gerekli |
| P0-04 | P0 | Veri kayıpsız OTA ve Room eski DB sürümlerinden geçiş | Aynı cihazda silmeden upgrade, eski playlist/favori/yerel kimlik korunması, migration ve rollback test kayıtları | BLOKER — fiziksel cihaz testi |
| P0-05 | P0 | YouTube Data API anahtarı/Android paket-sertifika yetkisi | Gerçek cihazda arama + sayfalama + kota/izin/offline ve gömülü oynatma smoke | BLOKER — harici Google yetkisi |
| P0-06 | P0 | Firebase Crashlytics, mapping, PII güvenliği | Gerçek Firebase proje erişimi, kimlik bilgisi sızdırmadan Gradle bağlantısı, test crash ve dashboard kanıtı | BLOKER — proje erişimi |
| P0-07 | P0 | PR #47 güvenilirlik + regresyon | 1 MiB sınır / taşma / boş gövde testleri, ilgili CI başarı, kontrollü merge | DEVAM EDİYOR |
| P1-02 | P1 | MainViewModel ve repo/singleton sınırlarının Hilt ve Clean Architecture'a ayrılması | Küçük PR'lar, DI testleri, regressionsız CI | AÇIK |
| P1-03 | P1 | Gerçek hesap + senkronizasyon / kullanıcı izolasyonu | Auth güvenlik modeli, çatışma senaryoları, hesap değişimi ve offline testleri | AÇIK; yerel önizleme gerçek sosyal hesap değildir |
| P2-02 | P2 | Uygulama çapında TR/EN, light/dark, büyük yazı, TalkBack | Resource taraması, Compose UI/accessibility screenshot testleri | AÇIK |
| P2-03 | P2 | Başlatma, RAM, akış, düşük ağ ve Android 24–36 uyumu | Tanımlı performans bütçesi + ölçüm, emülatör ve cihaz smoke | AÇIK |
| REL-02 | P0 | Yayın kabulü: sadece debug değil kalıcı imzalı release | Signed production APK + SHA, indirilebilir GitHub Release, gerçek cihaz yükseltme sonucu | BLOKER |

### İlerleme puanı: kanıta dayalı tahmini mühendislik değerlendirmesi
Bu rakamlar ölçülmüş son-kullanıcı memnuniyeti veya yüzde tamamlanma değildir. **Ağırlıklı risk/üretim hazırlığı puanıdır; kısmi kod ve CI kanıtı vardır, cihaz/servis kanıtı olmayan alt kriterlerde puan kırılır.** Çıktı 20.10.2026 tarihli main #366'ya dayanır; PR #47 henüz sayılmaz.

| Boyut | Azami | Mevcut | 90+ hedefi | Düşük puanın nedeni |
|---|---:|---:|---:|---|
| P0 veri, yedek, OTA güvenliği | 25 | 13 | 24 | Kurulu APK sertifikası ve göç testi kanıtsız |
| P1 mimari ve DI | 20 | 11 | 18 | Hilt başlangıç; tam ayrım yok |
| Özellikler, katalog ve P2 UX | 20 | 12 | 18 | Tam TR/EN, erişilebilirlik ve gerçek servis doğrulaması yok |
| CI, test, dayanıklılık | 20 | 16 | 19 | CI başarılı ama cihaz/migration testleri eksik |
| Yayın, Firebase, gerçek cihaz | 15 | 3 | 13 | Debug-only APK; kalıcı release/Crashlytics/device kabulü yok |
| **Toplam** | **100** | **55/100** | **92/100** | **Hedef şartları yerine gelmeden 90+ denmez** |

**Menzil:** Şimdi 55/100 (ihtiyatlı, tahmini); önce 70+ için test ve yazılım sertleştirme, sonra **90–92/100** için P0 imza+veri+YouTube+Firebase ve gerçek cihaz kabulü. Puan otomatik yükseltilmez; her satırın CI, gerçek cihaz veya servis kanıtlarıyla güncellenmesi zorunludur.

### Sıradaki işlem sırası
1. Bu durum kaydı ayrı PR ile ana dala alınır; CI/inceleme kanıtı toplanır.
2. PR #47'deki YouTube bellek güvenilirliği için ölçülebilir sınır/regresyon testlerini tamamla ve CI başarılıysa birleştir.
3. Room v3→4→5 veri kayıpsız migrasyon + şema fixture testleri; gerçek cihazda mevcut imzayı çıkartma sürecini belgele.
4. Hilt/MainViewModel taşımasını sorumluluk bazlı küçük PR'larla tamamla.
5. TR/EN/açık tema/accessibility/performance ölçümlerini ekran ekran kapat.
6. Yetkili insanın sağlayacağı Google/Firebase ve mevcut imza bilgileri geldikten sonra gerçek cihaz OTA/Crashlytics/YouTube kabulünü tamamla; production release ve smoke kanıtını kaydet.

**Kapanış disiplini:** ID + ilgili dosya + risk + PR/commit + CI linki + cihaz/API kanıtı + yayın linki + tarih olmadan hiç bir satır "BİTTİ" olarak işaretlenmez. Güncel takip: [Premium Issue #34](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/issues/34).

---


## 20.10.2026 — İlerleme ek kaydı: PR #47, #48 ve #49 sonrası

Aşağıdaki kayıt üstteki ilk 20.10.2026 snapshot'ının **sonradan gerçekleşen değişikliklerle düzeltilmesidir**. Tarihsel tablo silinmez; bu bölüm güncel durumu gösterir.

| Kayıt | Son geçerli kanıt | Durum |
|---|---|---|
| P0 YouTube API bellek güvenliği | [PR #47](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/47), [CI #369](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/39414638340), [birleşik main CI #374](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/39416149260) | BİRLEŞTİ, CI BAŞARILI; gerçek cihaz arama/API yetkisi ayrıca açık |
| Anayasa/üretim puan matrisi | [PR #48](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/48), [CI #372](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/39415046897) | BİRLEŞTİ, CI BAŞARILI |
| P0 Room v3→4→5 SQL data-retention smoke | [PR #49](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/49), [CI #371](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/39414966585) | BİRLEŞTİ, CI BAŞARILI; eski gerçek schema fixture ve çok üreticili Android migration smoke AÇIK |
| P1 MainViewModel Hilt constructor injection | [PR #50](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/50), [ilk CI #373](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/39416105739) | İlk CI'de runtime annotation introspection test tasarımı hatalıydı; düzeltme commit'i mevcut, [yeniden CI #378](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/39416992088) henüz kabul edilmedi |
| P2 Kitaplığım deep-link yönlendirmesi | [PR #51](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/51), [CI #375](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/39416301638) | Kod ve test PR'da; CI ve cihaz kabulü açık |
| P2 alt menü TR/EN yerelleştirme | [PR #52](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/pull/52), [CI #377](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/39416875623) | Kod ve iki dil testi PR'da; CI açık |

**Puanlama disiplini:** İlk 55/100, cihaz ve üretim kanıtları tamamlanmadan yapay biçimde 90+ yapılmaz. Hedef 92/100 **koşulludur**. Kod/CI iyileştirmeleri yapılan boyutların kanıt gücünü yükseltir, fakat kalıcı imza, veri kayıpsız OTA, Firebase, gerçek YouTube API ve tam cihaz kabulü eksikliği devam eder.

**Sonraki kabul adımları:** PR #50/#51/#52 CI ve regresyon; gerçek 3/4/5 Room fixture; mevcut kurulu APK imza fingerprint ile sabit release keystore karşılaştırması; silmeden Android OTA; YouTube gerçek anahtar + sertifika; Firebase Crashlytics ve R8 mapping; cihaz performans/erişilebilirlik testleri.

---

## Değişmez çalışma kuralları
1. Her proje için dört faz: **Faz 1 Envanter/Araştırma; Faz 2 Kök neden/Düzeltme; Faz 3 Uygulama/Build/Test; Faz 4 Doğrulama/Yayın/Üretim smoke**. Her fazda Araştır → Düzelt → Uygula → Test/Doğrula → Yayınla kapısı tamamlanır; başarısızsa yayın yapılmaz.
2. Kritik her zincir: **Ana servis → Alternatif → Çalışan fallback → Hata yönetimi → Güvenli varsayılan → Log/izleme → Otomatik smoke testi**. AI, üçüncü taraf API, veritabanı, ödeme/kota, dosya, görsel, kimlik doğrulama, frontend/backend, APK/backend ve üretim özel kapsamdır.
3. Ücretsiz/açık kaynak/yerel çözüm önceliklidir. **Kullanıcı açık onayı olmadan ücretli hizmet veya harcama yoktur.**
4. Girdiler ve yetkiler hem istemci hem sunucuda doğrulanır; sırlar asla repoya/loga yazılmaz; kişisel veri asgari tutulur. Yetkisiz veri toplama, hukuka aykırı entegrasyon veya Android kısıtını dolanma yapılmaz.
5. Modül/menü/buton/ekran/izin/veri/export ve offline senaryoları işlev testiyle kanıtlanır. Görünür ama işlevsiz buton yayınlanamaz. Eksik izin ve offline durumunda anlaşılır uyarı zorunludur.
6. CI kalite kapısı: bağımlılık kilidi → lint/typecheck → birim/integrasyon → build → smoke. Hata halinde fail-closed; test başarısızken başarılı release denmez.
7. Android projesinde yayınlanabilir her sürümün **Releases/Yayınlar** bölümünde APK ve mümkünse SHA-256 vardır; URL ve kurulabilirlik doğrulanır. Workflow artifact tek başına yeterli değildir.
8. Prod değişiklikleri küçük, geri alınabilir ve diff ile incelenebilir olmalı; main korunmalı, rollback/yedek ve migration planı tutulmalı. Gizli bilgileri veya eski bulguları silerek 'temiz' rapor hazırlanmaz.
9. **Ürün tam hazır olana kadar** Araştır → Kök neden → Düzelt → Uygula → Build → Test → Doğrula → Deploy → Production smoke → Kanıtla döngüsü geçerli. Çalışma gerçekleştirilmeden 'tamamlandı' yazılmaz.
10. Her hata kaydı: kimlik, etkilenen dosya/modül, etki, önem(P0/P1/P2/P3), kanıt/link, önerilen düzeltme, test, durum, son doğrulama commit'i içermeli.

## Durum etiketleri
- **DOĞRULANDI:** Günlük, test veya kod satırıyla kanıtlanan sorun.
- **RİSK / TEST GEREK:** Kod/iş akışından doğan ama cihaz/üretim testi isteyen konu.
- **BAŞARILI CI:** Yalnızca ilgili iş akışı başarılı; bütün ürünün kusursuzluğu anlamına gelmez.
- **P0:** yayın engelleyici; **P1:** kritik özellik/ güvenilirlik; **P2:** test/iyileştirme açığı; **P3:** dokümantasyon/kozmetik.

## Düzeltilmesi gerekenler ve takip
Bulguları aşağıdaki bölümde kanıt ve tamamlanma kriteriyle izleyin. Tamamlama için kaynak kod değişikliği, başarılı CI, çalışan ürün smoke ve (Android için) Release APK gerekir.

---
# Lanu-M-zik-Paltformu — DÜZELTİLMESİ GEREKENLER

**Tarama tarihi:** 15.10.2026  
**Depo:** https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu  
**Öncelik:** P0  
**Durum:** DOĞRULANDI

## Kanıtlanan durum / ön bulgu
Son CI: 82 testten 1 başarısız (MusicBrainzTurkishRapDiscoveryTest); release işi atlandı, son listelenen release kayıtları draft ve APK taşımıyor.

## İlgili düzeltme / kabul kriteri
MusicBrainz parseArtists testini düzelt; testleri tekrar çalıştır; başarılı APK'yi yayınlanmış Release'e ekle.

## Kontrol adımları
- [ ] README, ağaç, iş akışı, bağımlılık ve gerekiyorsa Android manifest bütünlüğü
- [ ] Kod içinde her görünür özelliğin olay işleyicisi ve veri akışı
- [ ] CI tüm kontroller yeşil, başarısız log kök nedeni giderilmiş
- [ ] Alternatif/fallback, offline, boş/bozuk veri, iptal/izin ret ve güvenlik kontrolleri
- [ ] Android ise gerçek cihaz testi, sağ/sol kulaklık varsa ayrı test, Release APK bağlantı+SHA kontrolü
- [ ] Üretim smoke kanıtı, sürüm/commit ve tarih güncellemesi

## Faz takibi
- **Faz 1 — Araştır:** Repo dosya ağacı ve son workflow/release kayıtları incelendi; tüm fonksiyonların cihazda çalışması henüz kanıtlanmadı.
- **Faz 2 — Düzelt:** Yukarıdaki konu için kod ve konfigürasyonda en küçük güvenli değişiklik gerekli.
- **Faz 3 — Test:** Birim + entegrasyon + cihaz / gerçek servis testi gerekir.
- **Faz 4 — Yayın:** Başarılı ve indirilebilir çıktı ile production smoke yapılmadan kapatılmaz.

---

## Ek Anayasa — Kanıtlı İnceleme ve Düzeltme Takibi (15.10.2026)

- **Kanıtlı araştırma:** [4 bulguluk kaynak-kod/CI/Release incelemesi](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/blob/main/docs/KANITLI_TEKNIK_DENETIM_2026-10-15.md).
- **Özel proje kuralı:** Lisanslı metadata ile gerçek oynatılabilir medya ayrılır; başarısız unit testi release'i bloke eder.
- **Kanıt sınıfları:** Doğrulanmış hata / kaynak kodu ile doğrulanmış eksik / hipotez / gerçek cihaz testi gerekli. Bu statüler birbirine karıştırılamaz.
- **Gerçek işlev zorunluluğu:** Her görünen menü, form, buton, kayıt, harita, oynatıcı ve servis işlev testiyle kanıtlanır; sahte bağlantı ve sahte başarı yasaktır.
- **Veri güvenliği:** Kullanıcı bilgisi uydurulamaz, sızdırılamaz veya sessizce kaybedilemez. Kimlik, senkronizasyon ve import/export yetki testinden geçer.
- **CI/Release kapısı:** Başarısız testte ürün tamamlanmış sayılmaz. Android'de SHA-256 ve indirilebilir Release APK; kritik modülde gerçek cihaz smoke zorunludur.
- **Ücret onayı:** Ücretli API/kota/servis tüketimi kullanıcı onayı olmadan genişletilmez; gerçek alternatif ve fallback test edilir.
- **Kanıtlı kapanış:** Bulgu ID, önem, dosya+satır, CI kanıtı, kök neden, düzeltme commit'i, kabul testi, sürüm, geri dönüş planı ile izlenir.
- **Faz durumu:** Bu ekleme yalnız analiz/dokümantasyon; kaynak kod düzeltmesi veya üretim doğrulaması değildir.


## EK ZORUNLU KURAL: Katalog Bütünlüğü ve Sanatçı Diskografileri (15.10.2026)

- Kullanıcı kabul kriteri: **Ceza örneği dahil, tüm sanatçı ve şarkı aramalarında ilk 9 / 40 / 50 sonuçla kalınmayacak.** Sanatçı kimliğiyle doğrulanmış diskografi ve provider sayfalaması aranacak; sonuçlar kademeli yüklenecek.
- Arama kutusundaki sonuç sayısı yalnızca **şu ana kadar indekslenen/erişilebilen kayıtları** gösterir; gerçek dünya diskografisinin tümü olduğu iddia edilmez.
- Birden çok sağlayıcıdan gelen sonuçlar gerçek provider kimliğine göre birleştirilir; kayıt kaynağı ve lisans korunur; olmayan veya oynatma hakkı bulunmayan dosya uydurulmaz.
- Şarkı filtresi sanatçı adına göre de çalışmalı; sanatçıya özgü aramada diskografisi adıyla doğrulanmış kaynaktan taranır.
- API başarısızlığında mevcut sonuçlar korunur, hata gösterilir, tekrar deneme mümkün olur; keşif durumu sonuçlardan ayrı gösterilir.
- **Regresyon:** Ceza ve diğer popüler Türkçe sanatçılar, aynı isimli farklı sanatçılar, ilk 50 üzerindeki şarkılar, albüm sayfalama, düşük ağ kalitesi ve kaynak kesintisi test edilir.
- **Tam katalog vaadi yok:** Üçüncü taraf katalog kapsamı ve lisansların dışındaki kayıtlar gerçek ses olarak sağlanamaz; eksiklikler açık belirtilir.

## EK ZORUNLU KURAL: Premium Keşif + Kanıtlı Genre (15 Ekim 2026)

- Kullanıcı talebi ve mobil ekran kanıtı: Sosyal ve Arşiv alt navigasyondan çıkartılır, **yerel kayıt/çalma listeleri silinmez**. Dört ana sekme: Ana Sayfa, Keşfet, Ekolayzır, Hesabım.
- Her katalog verisi için gerçek kaynak ID'si, erişilebilirlik, oynatma lisansı ve metadata vs. ses ayrımı; türü kanıtlanmayan kayda **Global Pop değil Türü Belirsiz** etiketi verilir.
- Türkçe Pop, Türkçe Rap, Anadolu Rock ve diğer kategorilerde gerçek indeks sayısı gösterilir; boş kategori sessiz beyaz/siyah ekran değildir.
- 'Tüm dünyadaki bütün şarkılar' garantisi verilemez: metadata kapsamı ve full-stream hakları ayrıdır. YouTube videolarının sesini ayrıştırmak veya görünmez arka planda oynatmak yasaktır; resmî görünür gömülü oynatıcı veya izinli tam ses kaynakları kullanılır.
- Ayrıntılı kanıt, düzeltme kriterleri ve dört faz: [LANU_KATALOG_VE_PREMIUM_ANAYASA_2026-10-15.md](docs/LANU_KATALOG_VE_PREMIUM_ANAYASA_2026-10-15.md).
- **Yayın kapısı:** PR CI, APK imza/sürüm ve gerçek cihazda ceza/genre/oynatma/arama testleri ayrı aşamalardır. CI yeşil = kullanıcı arızaları düzeldi anlamına gelmez.
