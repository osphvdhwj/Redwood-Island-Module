package com.example.dynamicisland.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object ProgrammaticIcons {

    // === MUSIC ICON VARIANTS ===

    val CupertinoMusic = ImageVector.Builder(
        name = "CupertinoMusic",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White), pathFillType = PathFillType.EvenOdd) {
        // Thick, deeply rounded double note (SF Style)
        moveTo(18f, 3f)
        curveTo(18f, 2.45f, 17.55f, 2f, 17f, 2f)
        curveTo(16.71f, 2f, 16.45f, 2.12f, 16.26f, 2.32f)
        lineTo(7.26f, 12.32f)
        curveTo(7.09f, 12.51f, 7f, 12.75f, 7f, 13f)
        verticalLineTo(18f)
        curveTo(7f, 19.66f, 5.66f, 21f, 4f, 21f)
        curveTo(2.34f, 21f, 1f, 19.66f, 1f, 18f)
        curveTo(1f, 16.34f, 2.34f, 15f, 4f, 15f)
        curveTo(4.35f, 15f, 4.69f, 15.06f, 5f, 15.18f)
        verticalLineTo(13f)
        curveTo(5f, 11.99f, 5.38f, 11.03f, 6.05f, 10.28f)
        lineTo(15.05f, 0.28f)
        curveTo(15.54f, -0.26f, 16.4f, -0.31f, 16.96f, 0.18f)
        curveTo(17.62f, 0.76f, 17.7f, 1.75f, 17.16f, 2.43f)
        lineTo(16f, 3.87f)
        verticalLineTo(16f)
        curveTo(16f, 17.66f, 14.66f, 19f, 13f, 19f)
        curveTo(11.34f, 19f, 10f, 17.66f, 10f, 16f)
        curveTo(10f, 14.34f, 11.34f, 13f, 13f, 13f)
        curveTo(13.35f, 13f, 13.69f, 13.06f, 14f, 13.18f)
        verticalLineTo(4.38f)
        lineTo(18f, 3f)
        close()
    }.build()

    val MaterialMusic = ImageVector.Builder(
        name = "MaterialMusic",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        // Geometric M3 Style
        moveTo(12f, 3f)
        verticalLineTo(13.55f)
        curveTo(11.41f, 13.21f, 10.73f, 13f, 10f, 13f)
        curveTo(7.79f, 13f, 6f, 14.79f, 6f, 17f)
        curveTo(6f, 19.21f, 7.79f, 21f, 10f, 21f)
        curveTo(12.21f, 21f, 14f, 19.21f, 14f, 17f)
        verticalLineTo(7f)
        horizontalLineTo(18f)
        verticalLineTo(3f)
        horizontalLineTo(12f)
        close()
    }.build()

    val CyberpunkMusic = ImageVector.Builder(
        name = "CyberpunkMusic",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        // Fractured, aggressive paths
        // Top bar fragment
        moveTo(10f, 2f)
        lineTo(22f, 4f)
        lineTo(22f, 6f)
        lineTo(12f, 4.5f)
        close()
        // Stem fragment with gap
        moveTo(10f, 4f)
        lineTo(10f, 16f)
        lineTo(8f, 16f)
        lineTo(8f, 5f)
        close()
        // Sharp triangular note head
        moveTo(8f, 16f)
        lineTo(10f, 22f)
        lineTo(3f, 19f)
        close()
        // Secondary stem fragment
        moveTo(22f, 7f)
        lineTo(22f, 14f)
        lineTo(20f, 14f)
        lineTo(20f, 8f)
        close()
        // Sharp triangular note head 2
        moveTo(20f, 14f)
        lineTo(22f, 19f)
        lineTo(15f, 16f)
        close()
    }.build()

    // === BATTERY (CHARGING) ICON VARIANTS ===

    val CupertinoBattery = ImageVector.Builder(
        name = "CupertinoBattery",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White), pathFillType = PathFillType.EvenOdd) {
        // Deeply rounded pill body (SF Style)
        moveTo(2f, 8f)
        curveTo(2f, 6.34f, 3.34f, 5f, 5f, 5f)
        horizontalLineTo(17f)
        curveTo(18.66f, 5f, 20f, 6.34f, 20f, 8f)
        verticalLineTo(16f)
        curveTo(20f, 17.66f, 18.66f, 19f, 17f, 19f)
        horizontalLineTo(5f)
        curveTo(3.34f, 19f, 2f, 17.66f, 2f, 16f)
        verticalLineTo(8f)
        close()
        // Rounded tip
        moveTo(21f, 10f)
        curveTo(21.55f, 10f, 22f, 10.45f, 22f, 11f)
        verticalLineTo(13f)
        curveTo(22f, 13.55f, 21.55f, 14f, 21f, 14f)
        close()
        // Central bolt
        moveTo(12.5f, 7f)
        lineTo(8.5f, 12f)
        horizontalLineTo(11f)
        lineTo(9.5f, 17f)
        lineTo(13.5f, 12f)
        horizontalLineTo(11f)
        lineTo(12.5f, 7f)
        close()
    }.build()

    val MaterialBattery = ImageVector.Builder(
        name = "MaterialBattery",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        // Geometric shell aligned to grid
        moveTo(15.67f, 4f)
        horizontalLineTo(14f)
        verticalLineTo(2f)
        horizontalLineTo(10f)
        verticalLineTo(4f)
        horizontalLineTo(8.33f)
        curveTo(7.6f, 4f, 7f, 4.6f, 7f, 5.33f)
        verticalLineTo(20.67f)
        curveTo(7f, 21.4f, 7.6f, 22f, 8.33f, 22f)
        horizontalLineTo(15.67f)
        curveTo(16.4f, 22f, 17f, 21.4f, 17f, 20.67f)
        verticalLineTo(5.33f)
        curveTo(17f, 4.6f, 16.4f, 4f, 15.67f, 4f)
        close()
        // Internal Bolt
        moveTo(14.5f, 11f)
        lineTo(12.27f, 11f)
        lineTo(13f, 7f)
        lineTo(9.5f, 13f)
        lineTo(11.73f, 13f)
        lineTo(11f, 19f)
        lineTo(14.5f, 11f)
        close()
    }.build()

    val CyberpunkBattery = ImageVector.Builder(
        name = "CyberpunkBattery",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        // Hexagonal aggressive shell
        moveTo(3f, 6f)
        lineTo(18f, 6f)
        lineTo(20f, 9f)
        lineTo(20f, 15f)
        lineTo(18f, 18f)
        lineTo(3f, 18f)
        lineTo(2f, 15f)
        lineTo(2f, 9f)
        close()
        // Fractured inner gap
        moveTo(4f, 8f)
        lineTo(4f, 16f)
        lineTo(17f, 16f)
        lineTo(18.5f, 14f)
        lineTo(18.5f, 10f)
        lineTo(17f, 8f)
        close()
        // Disconnected neon bolt
        moveTo(12f, 5f)
        lineTo(8f, 13f)
        lineTo(10f, 13f)
        lineTo(9f, 20f)
        lineTo(13f, 12f)
        lineTo(11f, 12f)
        lineTo(12f, 5f)
        close()
    }.build()

    // === NOTIFICATION BELL ICON VARIANTS ===

    val CupertinoBell = ImageVector.Builder(
        name = "CupertinoBell",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White), pathFillType = PathFillType.EvenOdd) {
        // Symmetrical wide bell (SF Style)
        moveTo(12f, 2f)
        curveTo(9.79f, 2f, 8f, 3.79f, 8f, 6f)
        verticalLineTo(11.29f)
        curveTo(8f, 12.33f, 7.57f, 13.33f, 6.81f, 14.05f)
        lineTo(5.41f, 15.36f)
        curveTo(4.55f, 16.17f, 5.12f, 17.6f, 6.31f, 17.6f)
        horizontalLineTo(17.69f)
        curveTo(18.88f, 17.6f, 19.45f, 16.17f, 18.59f, 15.36f)
        lineTo(17.19f, 14.05f)
        curveTo(16.43f, 13.33f, 16f, 12.33f, 16f, 11.29f)
        verticalLineTo(6f)
        curveTo(16f, 3.79f, 14.21f, 2f, 12f, 2f)
        close()
        // Rounded clapper
        moveTo(10f, 19f)
        curveTo(10f, 20.1f, 10.9f, 21f, 12f, 21f)
        curveTo(13.1f, 21f, 14f, 20.1f, 14f, 19f)
        close()
    }.build()

    val MaterialBell = ImageVector.Builder(
        name = "MaterialBell",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        // M3 Bell with sharp clapper separation
        moveTo(12f, 22f)
        curveTo(13.1f, 22f, 14f, 21.1f, 14f, 20f)
        horizontalLineTo(10f)
        curveTo(10f, 21.1f, 10.9f, 22f, 12f, 22f)
        close()
        moveTo(18f, 16f)
        verticalLineTo(11f)
        curveTo(18f, 7.93f, 16.37f, 5.36f, 13.5f, 4.68f)
        verticalLineTo(4f)
        curveTo(13.5f, 3.17f, 12.83f, 2.5f, 12f, 2.5f)
        curveTo(11.17f, 2.5f, 10.5f, 3.17f, 10.5f, 4f)
        verticalLineTo(4.68f)
        curveTo(7.63f, 5.36f, 6f, 7.92f, 6f, 11f)
        verticalLineTo(16f)
        lineTo(4f, 18f)
        verticalLineTo(19f)
        horizontalLineTo(20f)
        verticalLineTo(18f)
        lineTo(18f, 16f)
        close()
    }.build()

    val CyberpunkBell = ImageVector.Builder(
        name = "CyberpunkBell",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        // Trapezoidal aggressive bell
        moveTo(12f, 2f)
        lineTo(16f, 6f)
        lineTo(16f, 14f)
        lineTo(19f, 17f)
        lineTo(19f, 19f)
        lineTo(5f, 19f)
        lineTo(5f, 17f)
        lineTo(8f, 14f)
        lineTo(8f, 6f)
        close()
        // Fractured inner gap
        moveTo(9.5f, 7f)
        lineTo(14.5f, 7f)
        lineTo(14.5f, 13f)
        lineTo(9.5f, 13f)
        close()
        // Triangular clapper
        moveTo(10f, 20f)
        lineTo(14f, 20f)
        lineTo(12f, 23f)
        close()
    }.build()
}
