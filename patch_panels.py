import re

with open('./app/src/main/java/com/example/dynamicisland/IslandUIPanels.kt', 'r') as f:
    content = f.read()

# Apply Battery Text Size
content = content.replace('Text(text = "${model.level}%", color = color, fontSize = 16.sp', 'Text(text = "${model.level}%", color = color, fontSize = LocalIslandTheme.current.batteryCubeTextSize')
content = content.replace('Text(text = "${model.level}%", color = color, fontSize = 18.sp', 'Text(text = "${model.level}%", color = color, fontSize = LocalIslandTheme.current.batteryCubeTextSize')

# Apply Music Specific
# In MusicMid
content = content.replace('fontSize = theme.primaryTextSize, // 🚀 Dynamic Text Size!', 'fontSize = theme.musicTitleSize,')
content = content.replace('fontSize = theme.secondaryTextSize, // 🚀 Dynamic Subtext Size!', 'fontSize = theme.musicArtistSize,')

# In MusicMax
old_music_max_text = """            Text(text = music.title, color = dynamicTextColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.fillMaxWidth().basicMarquee())
            Text(text = music.artist, color = dynamicTextColor.copy(alpha=0.8f), fontSize = 16.sp, maxLines = 1, modifier = Modifier.fillMaxWidth().basicMarquee())"""

new_music_max_text = """            val theme = LocalIslandTheme.current
            Text(text = music.title, color = dynamicTextColor, fontSize = theme.musicTitleSize * 1.25f, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.fillMaxWidth().basicMarquee())
            Text(text = music.artist, color = dynamicTextColor.copy(alpha=0.8f), fontSize = theme.musicArtistSize * 1.15f, maxLines = 1, modifier = Modifier.fillMaxWidth().basicMarquee())"""
content = content.replace(old_music_max_text, new_music_max_text)

# Update AppTimerWarningMid
old_timer = """                 Text(text = "Time Limit Reached", color = Color.Red, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.basicMarquee())
                 Text(text = "${model.appName} closing in ${remainingSeconds}s", color = Color.White, fontSize = 14.sp, maxLines = 1, modifier = Modifier.basicMarquee())"""

new_timer = """                 val theme = LocalIslandTheme.current
                 Text(text = "Time Limit Reached", color = Color.Red, fontSize = theme.alertTitleSize, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.basicMarquee())
                 Text(text = "${model.appName} closing in ${remainingSeconds}s", color = Color.White, fontSize = theme.alertMessageSize, maxLines = 1, modifier = Modifier.basicMarquee())"""
content = content.replace(old_timer, new_timer)

with open('./app/src/main/java/com/example/dynamicisland/IslandUIPanels.kt', 'w') as f:
    f.write(content)
