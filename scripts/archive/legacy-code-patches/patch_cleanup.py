import re

with open('app/src/main/java/com/example/service/AudioPlayerController.kt', 'r') as f:
    content = f.read()

# Add imports for Equalizer
if 'import com.example.model.EqualizerState' not in content:
    content = content.replace('import com.example.model.Song\n', 'import com.example.model.Song\nimport com.example.model.EqualizerState\nimport com.example.model.EqualizerPreset\n')

# Remove duplicate Equalizer classes at the end
content = re.sub(r'enum class EqualizerPreset.*\}', '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/service/AudioPlayerController.kt', 'w') as f:
    f.write(content)
