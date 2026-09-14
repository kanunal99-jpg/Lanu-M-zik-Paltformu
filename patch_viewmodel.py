import re

with open('app/src/main/java/com/example/MainViewModel.kt', 'r') as f:
    content = f.read()

method = """
    fun scanLocalMusic() {
        repository.scanAndSyncLocalMusic()
    }
"""

content = content.replace("fun toggleFavorite(", method + "\n\n    fun toggleFavorite(")

with open('app/src/main/java/com/example/MainViewModel.kt', 'w') as f:
    f.write(content)
