import re

with open('app/src/main/AndroidManifest.xml', 'r') as f:
    content = f.read()

# Remove old services
content = re.sub(r'<service\s+android:name="\.service\.MusicPlaybackService".*?/>', '', content, flags=re.DOTALL)
content = re.sub(r'<service\s+android:name="\.service\.AudioPlayerService".*?/>', '', content, flags=re.DOTALL)

# Add new service
new_service = """
        <service
            android:name=".service.LanuMediaSessionService"
            android:foregroundServiceType="mediaPlayback"
            android:exported="true">
            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService" />
            </intent-filter>
        </service>
"""

content = content.replace('</application>', new_service + '\n    </application>')

with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(content)
