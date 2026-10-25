# Tarihsel kod değiştirme betikleri

Bu klasördeki `patch_*.py` ve `rewrite_services.py` dosyaları geçmişte kullanılan, güncel Android koduyla uyumluluğu **doğrulanmamış** tek seferlik dönüşümlerdir.

**Çalıştırmayın.** Bazıları mevcut Kotlin servislerini silebilir veya düzenleyebilir. CI bu dosyaları kullanmaz; yalnız tarihsel inceleme ve git geçmişinin okunabilirliği için korunurlar. Güncel değişiklikleri normal kaynak kodda, feature branch + PR + test süreciyle yapın.

Bu dosyaların taşınması çalışma zamanı uygulama davranışını değiştirmez.
