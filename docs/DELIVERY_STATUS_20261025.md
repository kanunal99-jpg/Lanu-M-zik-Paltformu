# LANU Müzik — Doğrulanabilir teslim durumu (25 Ekim 2026)

Bu belge GitHub'ın **main** dalı ve yayın envanterinden yapılan salt okunur denetimi ve repo-hijyen bakım paketinin kapsamını ayırır. CI'de geçmeyen cihaz, servis veya üretim kabulü **tamamlanmış sayılmaz**.

## Ana dal kanıtı (bu denetimden önce)

- **HEAD:** `57d067151e769d93a2b3867a4cd30c25b5637cf6` (21 Ekim 2026).
- **Son main CI:** [run 39662278022](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/39662278022) — başarılı.
- **Son yayın:** [LANU Music Build 510](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/releases/tag/lanu-music-510-57d067151e769d93a2b3867a4cd30c25b5637cf6) — `app-debug.apk` (31.228.247 bayt) ve `app-debug.apk.sha256`; **app-release.apk yok**.
- **Üretim CI:** Debug derleme ve R8 test sürümü üretilebilir; kalıcı production signing, kurulu sertifika eşleşmesi ve cihaz üstünde veri koruyan yükseltme kanıtı yok. Secrets değerleri repo üzerinden okunamadığı için var/yok şeklinde uydurma bildirim verilmez.

## Claude analizindeki yanlış ve doğru sınıflandırmalar

| İddia | Koddan denetlenen durum |
|---|---|
| `app/src` erişilemez | **Yanlış**. GitHub kaynak ağacı ve `MainViewModel.kt` dahil dosyalar okunabildi. |
| Hilt yalnız `UpdateViewModel` içinde | **Eski bilgi**. `MainViewModel` üzerinde `@HiltViewModel`, `@Inject`; `di/LanuViewModelModule.kt` var. **Tam Clean Architecture** yine ayrı konu. |
| Katalog provider ayrımı yok | **Yanlış**. `data/catalog/` altında Audius, Deezer, Jamendo, MusicBrainz ve diğer adapterlar var; canlı kullanılabilirliği ayrıca test edilmelidir. |
| Android instrumentation hiç yok | **Yanlış**. Bir temel `ExampleInstrumentedTest.kt` var; gerçek cihaz smoke **yok**. |
| README / lisans yok | **Doğru**; README bakım paketinde eklenir. Lisans seçimi bakım çalışması sırasında varsayılmaz. |
| `patch_*.py` ve `rewrite_services.py` kökte | **Doğru**. 12 `patch_*.py` + 1 `rewrite_services.py`; sürüm geçmişini korumak için arşive taşınır. |
| Üretim APK/OTA kanıtlanmış değil | **Doğru**. Debug artifact'i farklı imzalı kurulu APK'ya yükseltme olarak kullanılmaz. |
| Sosyal/arkadaşlar yerel önizleme | **Doğru**. Uzak hesap ve senkronizasyon olmadığı açıkça gösterilmeli. |

## Önceki editoryal puan kartı (21 Ekim'deki 75/100; yeni cihaz kabulü eklenmedi)

| Alan | Önceki kanıtlı puan | Hedef | Temel kabul eksiği |
|---|---:|---:|---|
| CI + otomatik test | 24/25 | 25 | Cihaz ve CI kapsam genişliği |
| Veri + çevrimdışı arşiv | 21/25 | 25 | Gerçek kurulu uygulamada veri korumalı yükseltme |
| Katalog + işlevler | 15/20 | 20 | Yasal ses erişimi, diskografi kapsamı, gerçek playback |
| Premium mimari + UX | 10/15 | 15 | Domain ayrımı, tam TR/EN, TalkBack, Crashlytics kabulü |
| APK/OTA + cihaz | 5/15 | 15 | Kalıcı imza, signed release, çoklu OEM smoke |
| **Toplam** | **75/100** | **100/100** | **25 puan**; yeni puan ancak yeni kanıtla |

## Öncelikli teslim kapıları

1. **P0 üretim imzası** — Kullanıcı cihazındaki mevcut uygulamanın sertifika SHA-256 değeri, yetkili kalıcı yayın keystore'u ile eşleşmeli. Anahtar hiçbir şekilde issue/PR'ye yazılmaz. [Takip #29](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/issues/29).
2. **P0 veri koruyan OTA** — Aynı package/signature, daha yüksek versionCode, SHA-256, korunan Room/kimlik/favori/playlist/çevrimdışı içerik; gerçek fiziksel cihazda kaldırmadan yükseltme testi.
3. **P0 cihaz regresyonu** — Samsung A55 + bir Xiaomi/Redmi + GMS'siz Android/Huawei, farklı API seviyeleri ve offline/4G-Wi-Fi geçişi. [Takip #18](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/issues/18).
4. **P1 test/altyapı** — Temel instrumentation'dan gerçek Compose/Room migration testlerine geçiş; tam Hilt sınırları ve yaşam döngüsü; güvenli Android güncellemeleri.
5. **P1 servis/ürün** — YouTube yetkisi/kota ve oynatıcı, Jamendo lisansı, telife uygun açık streaming/preview ayrımı; gerçek olmayan sosyal veriyle kullanıcıyı yanıltmama. [Takip #27](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/issues/27).
6. **P1 üretim UX** — TR/EN kaynakları, büyük font/TalkBack, Crashlytics opt-in ve dashboard/R8 mapping doğrulaması. [Takip #34](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/issues/34).

## Bu bakım PR'ının kapsamı

Kökten arşive taşınan tarihsel kod yeniden yazma betikleri, doğru kapsamı açıklayan README, dürüst ürün metadata açıklaması ve CI'ya eklenen repository hygiene doğrulaması. **Android çalışma zamanı, Room şeması, paket kimliği, imza ayarları, kullanıcı dosyaları değiştirilmez**. Bu bakımın CI sonucunu yalnız PR koşusu kesinleştirir; ana dal yayınına geçtiği ayrıca doğrulanır.
