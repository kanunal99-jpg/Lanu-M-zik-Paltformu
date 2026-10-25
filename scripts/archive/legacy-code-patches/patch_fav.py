import re

with open('app/src/main/java/com/example/data/MusicRepository.kt', 'r') as f:
    content = f.read()

bad_func = """    fun toggleFavorite(songId: String) {
        val favs = favoriteSongIds.first()
        if (favs.contains(songId)) {
            dao.removeFavorite(songId)
        } else {
            dao.addFavorite(FavoriteSongEntity(songId))
        }
    }"""

good_func = """    fun toggleFavorite(songId: String) {
        scope.launch {
            val favs = favoriteSongIds.first()
            if (favs.contains(songId)) {
                dao.removeFavorite(songId)
            } else {
                dao.addFavorite(FavoriteSongEntity(songId))
            }
        }
    }"""

content = content.replace(bad_func, good_func)

with open('app/src/main/java/com/example/data/MusicRepository.kt', 'w') as f:
    f.write(content)
