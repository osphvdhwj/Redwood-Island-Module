import re

with open("app/src/main/java/com/example/dynamicisland/IslandUIPanels.kt", "r") as f:
    content = f.read()

old_code = """        // Audio Manager
        val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager }
        val maxVolume = remember { audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).toFloat() }
        var volume by remember { mutableFloatStateOf(audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) / maxVolume) }
        var ringerState by remember { mutableIntStateOf(audioManager.ringerMode) }"""

new_code = """        // Audio Manager
        val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager }"""

content = content.replace(old_code, new_code)

with open("app/src/main/java/com/example/dynamicisland/IslandUIPanels.kt", "w") as f:
    f.write(content)
