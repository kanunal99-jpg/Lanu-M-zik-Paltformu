# LANU Music — Product Hardening 2026-09-16

Bu değişiklik, uygulamayı son kullanıcı kalite kapısına yaklaştırmak için gerçek ürün akışlarını sertleştirir.

## Kapsam
- Kullanıcı playlist'leri için rename ve deterministic song reorder.
- Playlist detay ekranında düzenle, yukarı/aşağı taşı ve sil kontrolleri.
- Media3 player hazır değilken gelen oynatma komutunu kaybetmeme.
- Shuffle/repeat tercihlerinin cihazda kalıcı tutulması.
- Media3 controller state ile UI state yeniden eşitleme.
- Geçiş yapılan parçaların gerçek history kaydı.
- Offline download tamamlandığında boş/eksik dosyanın başarı olarak işaretlenmemesi.
- Paylaşım metinlerinde doğrulanmamış web adresleri yerine gerçek yerel LANU bağlamı kullanılması.

## Bilinçli sınır
Remote katalog, remote auth veya remote sync için gerçek LANU sağlayıcısı doğrulanmadan sahte servis eklenmez.
