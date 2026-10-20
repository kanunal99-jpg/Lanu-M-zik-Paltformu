# LANU Firebase Crashlytics — Güvenli, İsteğe Bağlı Entegrasyon

**20.10.2026 • PR #65 — hazırlık, üretim onayı değildir**

- LANU, **Firebase/GMS olmadan derlenir ve yerel müzik, Room, Media3, kimlik ve favoriler için açılabilir**. Android APK uyumlu Samsung, Xiaomi, Huawei Android/EMUI ve diğer üreticilere GMS zorunlu kılınmaz.
- Yetkili geliştirici Firebase Android uygulamasını **com.aistudio.lanumusic.kpxzqa** package ID'si ile Firebase Console'da kurduğunda, doğru proje `google-services.json` dosyasını **repo geçmişine koymadan**, güvenli CI aşamasında `app/google-services.json` konumuna verir.
- `google-services.json` mevcutsa Gradle, Google Services 4.5.0 + Crashlytics Gradle 3.0.8 plugin'ini ve Firebase BoM üzerinden Crashlytics SDK'yı ekler. Dosya yoksa bu plugin'lerin işlevleri aktif olmaz; hiçbir sahte Firebase bağlantısı raporlanmaz.
- `app/src/main/res/values/firebase_diagnostics.xml` üzerinde `firebase_crashlytics_collection_enabled=false` varsayılan **gizlilik kapısı** vardır. Kullanıcının açık izni, Settings UI, geri alma ve KVKK/GDPR politikası tamamlanmadan Crashlytics gönderimi açılmayacaktır.
- Firebase Console tarafında gerçek test crash ve ANR, Analytics isteğe bağlı, mapping / sürüm çözümlemesi ve API24/29/33/35 emülatör ve GMS'siz fiziksel cihaz smoke testleri yapılmadan **"Crashlytics hazır" denmez**.
- R8 `mapping.txt` artefaktı üretme ve kaynak küçültme CI tarafında zaten var. Release mapping'in Firebase'e gerçekten yüklendiğini doğrulamak için **gerçek proje yetkisi ve CI credentials** gerekir; plugin tek başına kanıt oluşturmaz.
- Firebase yapılandırma dosyası veya servis kimliği, signing keystore, özel anahtarlar, Crashlytics kullanıcı PII değerleri proje issue'larına veya sohbet içine paylaşılmaz. Sır kalıcı depoda korunmalı.
- Kritik üretim engelleri: canlı Firebase erişimi, kullanıcı tanılama tercihi UI'si, test crash'in dashboard'a düşmesi ve R8 mapping doğrulaması.

**Resmî kaynak:** https://firebase.google.com/docs/crashlytics/android/get-started
