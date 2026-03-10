import re

with open('./app/src/main/java/com/example/dynamicisland/IslandUIPanels.kt', 'r') as f:
    content = f.read()

# MusicMid UI adjustments
old_mid = """            // Text Column
            Column(modifier = Modifier.weight(1f, fill=false)) {
                Text(
                    text = music.title,
                    color = dynamicTextColor,
                    fontSize = theme.musicTitleSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1, modifier = Modifier.basicMarquee()
                )
                Text(
                    text = music.artist,
                    color = dynamicTextColor.copy(alpha = 0.7f),
                    fontSize = theme.musicArtistSize,
                    maxLines = 1, modifier = Modifier.basicMarquee()
                )
            }"""

new_mid = """            // Text Column
            Column(modifier = Modifier.weight(1f, fill=false).offset(x = theme.titleOffsetX, y = theme.titleOffsetY)) {
                Text(
                    text = music.title,
                    color = dynamicTextColor,
                    fontSize = theme.titleSize,
                    fontFamily = theme.titleFont, // 🚀 Custom Font!
                    fontWeight = FontWeight.Bold,
                    maxLines = 1, modifier = Modifier.basicMarquee()
                )
                Text(
                    text = music.artist,
                    color = dynamicTextColor.copy(alpha = 0.7f),
                    fontSize = theme.titleSize * 0.85f, // Usually artist is a bit smaller than title
                    fontFamily = theme.titleFont,
                    maxLines = 1, modifier = Modifier.basicMarquee()
                )
            }"""

content = content.replace(old_mid, new_mid)

# Progress Bar
old_lin = """        androidx.compose.foundation.Canvas(modifier = modifier.height(theme.progressBarThickness)) {
            drawLine(
                color = trackColor,
                start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
                strokeWidth = theme.progressBarThickness.toPx(), // 🚀 Dynamic Thickness!
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                end = androidx.compose.ui.geometry.Offset(size.width * progress, size.height / 2),
                strokeWidth = theme.progressBarThickness.toPx(), // 🚀 Dynamic Thickness!
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }"""

new_lin = """        androidx.compose.foundation.Canvas(modifier = modifier.height(theme.mediaBarThickness)) {
            drawLine(
                color = trackColor,
                start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
                strokeWidth = theme.mediaBarThickness.toPx(),
                cap = theme.mediaBarCap // 🚀 Custom Shape (Round, Square, Butt)
            )
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                end = androidx.compose.ui.geometry.Offset(size.width * progress, size.height / 2),
                strokeWidth = theme.mediaBarThickness.toPx(),
                cap = theme.mediaBarCap
            )
        }"""

content = content.replace(old_lin, new_lin)

with open('./app/src/main/java/com/example/dynamicisland/IslandUIPanels.kt', 'w') as f:
    f.write(content)
