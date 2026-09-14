with open('app/src/main/java/com/example/model/Song.kt', 'r') as f:
    content = f.read()

content = content.replace('    LOCAL("Yerel Müzik"),\n', '')
content = content.replace('    CHILL_LOFI("chill_lofi", "Akustik & Chill", "Acoustic & Chill", Color(0xFF56AB2F), Color(0xFFA8E063), "☕");', '    CHILL_LOFI("chill_lofi", "Akustik & Chill", "Acoustic & Chill", Color(0xFF56AB2F), Color(0xFFA8E063), "☕"),\n    LOCAL("local", "Yerel Müzik", "Local Music", Color(0xFF2C3E50), Color(0xFF3498DB), "📁");')

with open('app/src/main/java/com/example/model/Song.kt', 'w') as f:
    f.write(content)
