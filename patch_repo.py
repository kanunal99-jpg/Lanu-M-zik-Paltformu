import re

with open('app/src/main/java/com/example/data/MusicRepository.kt', 'r') as f:
    content = f.read()

scanner_field = """
    private val scope = CoroutineScope(Dispatchers.IO)
    private val localMusicScanner = LocalMusicScanner(context)
"""

content = content.replace("private val scope = CoroutineScope(Dispatchers.IO)", scanner_field.strip())

scan_method = """
    fun scanAndSyncLocalMusic() {
        scope.launch {
            val localSongs = localMusicScanner.scanLocalMusic()
            if (localSongs.isNotEmpty()) {
                // Combine with existing songs
                val current = _songs.value.toMutableList()
                val existingIds = current.map { it.id }.toSet()
                val newSongs = localSongs.filter { !existingIds.contains(it.id) }
                
                if (newSongs.isNotEmpty()) {
                    current.addAll(newSongs)
                    _songs.value = current
                    
                    // Save to Room
                    dao.insertLocalSongs(newSongs.map { it.toLocalEntity(isOffline = true) })
                }
            }
        }
    }
"""

content = content.replace("fun toggleFavorite(", scan_method + "\n\n    fun toggleFavorite(")

with open('app/src/main/java/com/example/data/MusicRepository.kt', 'w') as f:
    f.write(content)
