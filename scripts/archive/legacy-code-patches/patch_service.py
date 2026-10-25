import re

with open('app/src/main/java/com/example/service/LanuMediaSessionService.kt', 'r') as f:
    content = f.read()

# Remove the setCallback block
content = re.sub(r'\.setCallback\(object : MediaSession\.Callback \{[\s\S]*?\}\)\n', '', content)

with open('app/src/main/java/com/example/service/LanuMediaSessionService.kt', 'w') as f:
    f.write(content)

