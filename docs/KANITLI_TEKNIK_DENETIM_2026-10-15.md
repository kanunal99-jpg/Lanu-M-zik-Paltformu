# Lanu-M-zik-Paltformu — KANITLI TEKNİK DENETİM

**Tarih:** 15.10.2026  
**Kapsam:** GitHub dosya ağacı, seçili kaynak kodu, CI logları, Releases. Gerçek cihaz ve dış servislerin tam E2E testi yapılmadı.  
**Dosya sayısı (denetim anında):** 142  
**CI gözlemi:** 15.10.2026 Unit & Robolectric test aşamasında CI başarısız; Release skipped. Önceki CI: 82 testte 1 başarısız.  
**Anayasa:** [PROJE_ANAYASASI_VE_DUZELTILECEKLER.md](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/blob/main/PROJE_ANAYASASI_VE_DUZELTILECEKLER.md)

## Kanıtlarla Düzeltilmesi Gerekenler

### 1. MusicBrainz kimlik ayrıştırma testi başarısız — P0 (DOĞRULANMIŞ CI HATASI)
- **Kanıt:** [app/src/test/java/com/example/data/catalog/MusicBrainzTurkishRapDiscoveryTest.kt#L1-L27](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/blob/main/app/src/test/java/com/example/data/catalog/MusicBrainzTurkishRapDiscoveryTest.kt#L1-L27); [CI koşusu](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/38673161474)
- **Gözlem:** parseArtists_keeps_real_identity_fields_and_drops_invalid_rows birim testi RuntimeException ile durdu; uygulama CI'da test kapısını geçemedi.
- **Somut örnek / etkisi:** Geçerli MBID içeren JSON iki sanatçıya ayrıştırılmalı; boş ID kayıt atılmalı.
- **Düzeltme kabul testi:** Tam stack trace ve Gradle JUnit XML analiz edilsin; org.json Android/JVM uyumluluğu araştırılsın; gerçek hata giderildikten sonra 82+ test yeşil.
- **Durum:** ARAŞTIRILDI, DÜZELTME HENÜZ BAŞLAMADI.

### 2. Başarısız test APK yayınını engelliyor — P0 (DOĞRULANMIŞ RELEASE BLOKAJI)
- **Kanıt:** [.github/workflows/android.yml#L87-L115](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/blob/main/.github/workflows/android.yml#L87-L115); [CI koşusu](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/38673161474)
- **Gözlem:** Build/test işindeki hata nedeniyle Publish GitHub Release skipped. Son listelenen draft yayınlar APK içermiyor.
- **Somut örnek / etkisi:** Kullanıcı 'son sürüm' istediğinde git release draft dosyasından kurulabilir APK alamayabilir.
- **Düzeltme kabul testi:** Test→APK→SHA→Release asset→URL indirme zincirini doğrula.
- **Durum:** ARAŞTIRILDI, DÜZELTME HENÜZ BAŞLAMADI.

### 3. Müzik sağlayıcılarının oynatma ve lisans ayrımı — P2 (LİSANS / KAYNAK TESTİ)
- **Kanıt:** [app/src/main/java/com/example/data/catalog/MusicBrainzTurkishRapDiscovery.kt#L16-L20](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/blob/main/app/src/main/java/com/example/data/catalog/MusicBrainzTurkishRapDiscovery.kt#L16-L20); [CI koşusu](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/38673161474)
- **Gözlem:** MusicBrainz sadece metadata/discovery kaynağı. Jamendo ticari lisans geçidi CI ile kontrol ediliyor.
- **Somut örnek / etkisi:** Sanatçı bulunması, yasal tam şarkı oynatma hakkı olduğu anlamına gelmez.
- **Düzeltme kabul testi:** Audius/Jamendo/Deezer kaynakları, lisans, oynatılabilir URL, bozuk internet ve yasal güvenli fallback testleri.
- **Durum:** ARAŞTIRILDI, DÜZELTME HENÜZ BAŞLAMADI.

### 4. Kökte çok sayıda patch/yeniden yazma scripti — P2 (KOD HİJYENİ)
- **Kanıt:** [tree/main](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/tree/main); [CI koşusu](https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/actions/runs/38673161474)
- **Gözlem:** patch_main.py, patch_repo.py, patch_test.py, rewrite_services.py gibi bakım scriptleri aktif kaynakla birlikte duruyor.
- **Somut örnek / etkisi:** Yanlışlıkla bakım scripti üretim dosyasını yeniden yazabilir.
- **Düzeltme kabul testi:** Üretim için gerekli olanları test altında tut; tek seferlik scriptleri tarih/amaçla arşivle, silmeden önce kullanım kanıtı al.
- **Durum:** ARAŞTIRILDI, DÜZELTME HENÜZ BAŞLAMADI.

## Zorunlu 4 Faz
1. Araştır/Envanter: kapsam, kanıt ve etki; bu rapor başlangıç seviyesinde seçili dosya/CI incelemesidir.
2. Kök neden ve düzeltme: P0/P1 önce; en küçük geri alınabilir kod değişimi; gizli veriyi koru; ücret için onay al.
3. Build + test: lint, unit/integrasyon/UI/E2E, offline/kesinti/fallback, gerekirse gerçek cihaz.
4. Doğrula ve yayınla: tüm testler yeşil; Android APK Releases altında SHA-256 ile doğrudan indirilebilir; smoke ve bağlantı doğrulaması.

**Bulgu kapatma:** Kod satırı + kök neden + düzeltme commit'i + test + CI + sürüm/ürün smoke kanıtı olmadan hiçbir madde bitmiş sayılmaz.
