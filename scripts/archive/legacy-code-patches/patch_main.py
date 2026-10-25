import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# Remove import
content = content.replace('import com.example.service.MusicPlaybackService\n', '')

# Replace MainActivity block
new_main_activity = """class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen(
    viewModel: MainViewModel = viewModel()
) {"""

content = re.sub(r'class MainActivity : ComponentActivity\(\) \{[\s\S]*?@Composable\nfun MainAppScreen\(\n    playbackService: MusicPlaybackService\?,\n    viewModel: MainViewModel = viewModel\(\)\n\) \{', new_main_activity, content)

# Remove startForegroundWithNotification calls
content = re.sub(r'                playbackService\?\.startForegroundWithNotification.*?isPlaying\)\n', '', content)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)

