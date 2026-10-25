import re

with open('app/src/test/java/com/example/AudioPlayerServiceTest.kt', 'r') as f:
    content = f.read()

# Robolectric idle is needed after setPlaylist/play
import_stmt = 'import org.robolectric.shadows.ShadowLooper\nimport org.junit.Test'
content = content.replace('import org.junit.Test', import_stmt)

def replace_with_idle(match):
    return match.group(0) + '\n        ShadowLooper.idleMainLooper()'

content = re.sub(r'service\.setPlaylist\([^\)]+\)', replace_with_idle, content)
content = re.sub(r'service\.play\(\)', replace_with_idle, content)
content = re.sub(r'service\.pause\(\)', replace_with_idle, content)
content = re.sub(r'service\.togglePlayPause\(\)', replace_with_idle, content)
content = re.sub(r'service\.skipToNext\(\)', replace_with_idle, content)
content = re.sub(r'service\.skipToPrevious\(\)', replace_with_idle, content)

with open('app/src/test/java/com/example/AudioPlayerServiceTest.kt', 'w') as f:
    f.write(content)
