import re

with open('./app/src/main/java/com/example/dynamicisland/DynamicIslandView.kt', 'r') as f:
    content = f.read()

old_theme_parser = """                activeTheme.value = IslandTheme(
                    cornerRadius = intent.getFloatExtra("theme_corner_radius", activeTheme.value.cornerRadius.value).dp,
                    primaryTextSize = intent.getFloatExtra("theme_text_primary", activeTheme.value.primaryTextSize.value).sp,
                    secondaryTextSize = intent.getFloatExtra("theme_text_secondary", activeTheme.value.secondaryTextSize.value).sp,
                    progressBarThickness = intent.getFloatExtra("theme_progress_thick", activeTheme.value.progressBarThickness.value).dp,
                    ringThickness = intent.getFloatExtra("theme_ring_thick", activeTheme.value.ringThickness.value).dp,
                    buttonSize = intent.getFloatExtra("theme_button_size", activeTheme.value.buttonSize.value).dp,
                    elementGap = intent.getFloatExtra("theme_element_gap", activeTheme.value.elementGap.value).dp,

                    musicTitleSize = intent.getFloatExtra("theme_music_title", activeTheme.value.musicTitleSize.value).sp,
                    musicArtistSize = intent.getFloatExtra("theme_music_artist", activeTheme.value.musicArtistSize.value).sp,
                    musicSeekerThickness = intent.getFloatExtra("theme_music_seeker", activeTheme.value.musicSeekerThickness.value).dp,
                    musicButtonSize = intent.getFloatExtra("theme_music_btn", activeTheme.value.musicButtonSize.value).dp,

                    batteryCubeTextSize = intent.getFloatExtra("theme_bat_text", activeTheme.value.batteryCubeTextSize.value).sp,
                    batteryCubeIconSize = intent.getFloatExtra("theme_bat_icon", activeTheme.value.batteryCubeIconSize.value).dp,
                    batteryRingThickness = intent.getFloatExtra("theme_bat_ring", activeTheme.value.batteryRingThickness.value).dp,

                    alertTitleSize = intent.getFloatExtra("theme_alert_title", activeTheme.value.alertTitleSize.value).sp,
                    alertMessageSize = intent.getFloatExtra("theme_alert_msg", activeTheme.value.alertMessageSize.value).sp
                )"""

new_theme_parser = """                val capString = intent.getStringExtra("theme_media_cap") ?: "Round"
                val parsedCap = if (capString == "Square") StrokeCap.Square else StrokeCap.Round

                activeTheme.value = IslandTheme(
                    mediaBarCap = parsedCap,
                    mediaBarThickness = intent.getFloatExtra("theme_media_thick", 4f).dp,
                    titleOffsetX = intent.getFloatExtra("theme_title_x", 0f).dp,
                    titleOffsetY = intent.getFloatExtra("theme_title_y", 0f).dp,
                    titleSize = intent.getFloatExtra("theme_title_size", 16f).sp,
                    timeTextSize = intent.getFloatExtra("theme_time_size", 12f).sp,
                    timeTextOffsetX = intent.getFloatExtra("theme_time_x", 0f).dp,
                    batteryRingThickness = intent.getFloatExtra("theme_bat_ring", 12f).dp,
                    cornerRadius = intent.getFloatExtra("theme_corner_radius", 50f).dp,
                    albumArtSize = intent.getFloatExtra("theme_album_art_size", 44f).dp,
                    buttonSize = intent.getFloatExtra("theme_button_size", 48f).dp
                )"""

content = content.replace(old_theme_parser, new_theme_parser)

with open('./app/src/main/java/com/example/dynamicisland/DynamicIslandView.kt', 'w') as f:
    f.write(content)
