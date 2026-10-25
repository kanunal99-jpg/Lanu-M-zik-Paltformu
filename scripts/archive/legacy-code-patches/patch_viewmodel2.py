import re

with open('app/src/main/java/com/example/MainViewModel.kt', 'r') as f:
    content = f.read()

bad_func = """    fun scanLocalMusic() {
        repository.scanAndSyncLocalMusic()
    }"""

good_func = """    fun scanLocalMusic() {
        viewModelScope.launch {
            repository.scanAndSyncLocalMusic()
        }
    }"""

content = content.replace(bad_func, good_func)

with open('app/src/main/java/com/example/MainViewModel.kt', 'w') as f:
    f.write(content)
