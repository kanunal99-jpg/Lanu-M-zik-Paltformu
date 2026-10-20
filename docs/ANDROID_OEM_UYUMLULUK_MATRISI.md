# LANU Müzik — Android Üretici Bağımsız Uyumluluk Anayasası

**Başlangıç:** 20.10.2026 • **Kapsam:** Android işletim sistemi / APK uyumlu telefon, tablet ve katlanabilir cihazlar

> **Önemli düzeltme:** LANU Müzik yalnızca Samsung Galaxy A55'e yapılmıyor. A55, varsa fiziksel regresyon cihazlarından sadece biridir. Bir üreticiye özel fonksiyon, ekran, imza veya Google hesabı ön şartı konulmayacak.

## Desteklenen platform ve gerçekte kanıtlanmış durum

- **Android 7.0+ (API 24+)** uygulamanın `minSdk` gereksinimidir. Mevcut `targetSdk` **36**, `compileSdk` **36.1**. Bu değerler her cihazda test yapıldığını **kanıtlamaz**. Android 17 / API **37** geriye dönük çalışma ve büyük ekran davranışı **test planında** yer alır; API 37 hedefleme ayrıca değerlendirilir.
- **Hedef OEM örnekleri:** Samsung (One UI), Xiaomi/Redmi/POCO (HyperOS/MIUI), Huawei **Android/EMUI veya APK uyumlu HarmonyOS sürümleri**, Honor (MagicOS), Oppo (ColorOS), Vivo (Funtouch OS), Realme, OnePlus, Motorola, Google Pixel, Nokia/HMD ve diğer AOSP/Android uyumlu üreticiler.
- **Huawei ayrımı:** Google servisleri bulunmayan fakat Android APK çalıştıran modeller hedef kapsamındadır. **HarmonyOS NEXT / HarmonyOS 5+ gibi Android APK çalıştırmayan sürümler, bir Android APK ile destekleniyor sayılmaz**; bunlar ayrı doğal platform uygulaması gerektirir. Aynı markanın her modeli için uyumluluk iddiası yasaktır.
- **Telefon + tablet + katlanabilir:** kompakt 360dp, dar ekran/200% metin, yatay, 600dp+ tablet, çok pencere, katlanır ve Android 17 API37 büyük ekran davranışları kabul matrisi kapsamındadır.
- **Google Mobile Services (GMS) olmadan:** temel yerel katalog/çalma, favori/playlist, yerel kimlik, Room, Media3 ve GitHub OTA **GMS olmadan açılabilmeli**. Google/Firebase/YouTube özellikleri varsa erişilemediğinde kullanıcı dostu *unavailable/fallback* sunulmalı; yerel veriyi silme veya açılışı bloklama yok. Bu hedefler **cihazda henüz doğrulanmadı**.
- **İndirme / kurulum politikası:** APK kimliği `com.aistudio.lanumusic.kpxzqa` tüm üreticilerde aynı; kurulu imza sertifikası, artan versionCode, verified release hash ve Android'in kullanıcı onaylı yükleyicisi zorunlu. Başarısız yüklemede **asla kaldır/yeni kur** önerisi yok.
- **Mağaza bağımsız dağıtım:** Mevcut GitHub debug APK, production OTA değildir. Kalıcı signing keystore/sertifika eşleştirme, signed release ve Google/Firebase kimlik bilgisi için yetkili kurulum gerekiyor. 2026 sonrası bazı sertifikalı Android cihazlarda üretici mağazalarının kurulum/geliştirici doğrulama politikaları ayrıca kontrol edilecek; hedef dağıtım kanalı üzerinden kontrol yapılmadan yayın hazır denmez.

## Fiziksel cihaz / emülatör kabul matrisi (tamamlanmamış)

| Grup | Örnek üretici / yazılım | Minimum koşul | Test ve kabul kanıtı |
|---|---|---|---|
| Eski Android, API 24–28 | Çeşitli OEM; Android 7–9 | Kurulum, medya izni, yerel dosya, kimlik, oynatma, cache | API 24/26/28 emülatör + en az 1 gerçek cihaz |
| Orta Android, API 29–32 | Samsung, Xiaomi, Motorola vb. | Scoped storage, MediaStore, bildirim ve background ses | API 29/30/32 cihaz/emülatör |
| Android 13–15, API 33–35 | Samsung One UI, Xiaomi HyperOS, Oppo/Vivo/Pixel | READ_MEDIA_AUDIO, foreground playback, OTA FileProvider, izin ret/fallback | API 33/34/35 ve en az 2 farklı OEM |
| Android 16, API 36 | Samsung/Xiaomi/Pixel/Honor | target36, gizlilik/arka plan, R8, sürüm yükseltme | API36 emülatör + gerçek cihaz |
| Android 17, API 37 (ileri uyumluluk) | Android OEM / Pixel emülatörü | çoklu pencere, büyük ekran uyarlama, runtime korumaları | API37 smoke ve ölçüm; mevcut target36 ile de test |
| GMS olmayan Android | Huawei EMUI ve AOSP test image | Yerel arşiv ve medya + OTA, Firebase/YouTube erişilemiyorsa çökmeden güvenli fallback | GMS'siz Android emülatör veya Huawei APK-uyumlu gerçek cihaz |
| Tablet / katlanabilir | Samsung, Lenovo, Xiaomi, Pixel Fold vb. | 600dp+, font ölçekleme, yatay, katlanma, klavye ve TalkBack | 7/10 inç emülatör + fiziksel cihaz |

**Zorunlu çapraz senaryolar:** İlk açılış, oturum/süreklilik, aynı anda favoriler/playlist, uçak modu, yavaş ağ, lisanslı/yerel ses, bluetooth ve kulaklık, oynatma kesintisi, bildirim, yeniden başlatma, ekran döndürme, pil optimizasyonu ve arka plan kısıtları, alt menü TR/EN, TalkBack ve yazı büyütme. Üreticiye özgü pil ayarı davranışı farklı olabilir; otomatik olarak kısıtlamaları atlatma yok.

**Kritik OTA ve veri kaybı testi her ailede:** Mevcut kullanıcı APK'sı korunur; imza + paket kimliği doğrulanır, kurulu kullanıcı kimliği/favori/playlist/Room verisi kaydedilir, **silmeden** daha yeni üretim imzalı APK ile yükseltilir, bilgiler karşılaştırılır. İmza eşleşmiyorsa işlem *bloklanır*, uygulama kaldırılmaz. Gerçek üretim signing credentials olmadan bu satırlar tamamlandı denemez.

## Otomatik ve manuel uygulama

- `python3 scripts/check_android_oem_compatibility.py`: CI üzerinde minSdk, package ID, manifest donanım/üretici kilidi ve ekran yönlendirme katı kısıtları için taşınabilir denetim. Marka bazında fiziksel uyumluluğu tek başına kanıtlamaz.
- `bash scripts/android_device_readonly_smoke.sh`: `adb` bağlı tek Android cihazın marka/model/API, GMS mevcutluk ve LANU paket durumunu **salt okunur** raporlar; kullanıcı verisi, telefon kimliği/seri numarası toplamaz; kurulum/kalıcı cihaz ayarı değiştirmez.
- Her gerçek cihaz sonucu: Android API, üretici ve model, GMS mevcutluğu, cihaz tipi, test sürümü, gözlemler (pass/fail/skipped), imzalı OTA izin/sertifika kanıtı ve test tarihi ile issue #34'e işlenmeli.

## Durum ve kapsam kuralları

- **Doğrulanan:** Gradle minSdk24/target36/compile36.1; Android manifest'te belirli bir üretici gereksinimi görünmüyor. Bu **statik incelemedir**; uygulamanın bütün cihazlarda sorunsuz açıldığı anlamına gelmez.
- **Açık:** Çok üreticili gerçek cihaz smoke, API37, GMS'siz uçtan uca test, tablet/katlanır ekran adaptasyonu, korunan veriyle OTA ve gerçek signed release.
- **Anayasa:** Hedef 92/100 tek marka ile değil, Android API / OEM / form factor / servis var-yok matrisinin kanıtlarıyla değerlendirilir.

## Güncel referanslar

- Android 17 adaptif büyük ekran: https://developer.android.com/blog/posts/android-17-is-here
- Android platform API düzeyleri: https://source.android.com/docs/setup/reference/build-numbers
- Android geliştirici doğrulama süreçleri: https://developer.android.com/developer-verification
- Huawei APK-uyumluluğu cihaz işletim sistemine bağlı olduğundan marka adına göre otomatik garanti verilmez.
