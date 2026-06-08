package com.example.dynamicisland.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.shared.settings.AestheticStyle
import com.example.dynamicisland.shared.settings.IconPack
import com.example.dynamicisland.shared.settings.DesignLanguage
import com.example.dynamicisland.shared.settings.PhysicsStyle
import com.example.dynamicisland.shared.settings.ContentTransitionStyle
import com.example.dynamicisland.shared.model.IslandState
import com.example.dynamicisland.shared.model.LiveActivityModel
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.shared.model.LocalIslandTheme
import com.example.dynamicisland.shared.model.IslandTheme
import com.example.dynamicisland.core.ui.design.RedwoodTheme
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*

/**
 * ELITE PROGRAMMATIC ASSETS
 * Custom vector paths for industry-standard icon styles.
 */
object ProgrammaticIcons {

    // === MUSIC ICON VARIANTS ===

    val CupertinoMusic = ImageVector.Builder(
        name = "CupertinoMusic",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White), pathFillType = PathFillType.EvenOdd) {
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
        moveTo(10f, 2f)
        lineTo(22f, 4f)
        lineTo(22f, 6f)
        lineTo(12f, 4.5f)
        close()
        moveTo(10f, 4f)
        lineTo(10f, 16f)
        lineTo(8f, 16f)
        lineTo(8f, 5f)
        close()
        moveTo(8f, 16f)
        lineTo(10f, 22f)
        lineTo(3f, 19f)
        close()
    }.build()

    // === BATTERY ICON VARIANTS ===

    val CupertinoBattery = ImageVector.Builder(
        name = "CupertinoBattery",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White), pathFillType = PathFillType.EvenOdd) {
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
        moveTo(21f, 10f)
        curveTo(21.55f, 10f, 22f, 10.45f, 22f, 11f)
        verticalLineTo(13f)
        curveTo(22f, 13.55f, 21.55f, 14f, 21f, 14f)
        close()
    }.build()

    val MaterialBattery = ImageVector.Builder(
        name = "MaterialBattery",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
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
    }.build()

    val CyberpunkBattery = ImageVector.Builder(
        name = "CyberpunkBattery",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(3f, 6f)
        lineTo(18f, 6f)
        lineTo(20f, 9f)
        lineTo(20f, 15f)
        lineTo(18f, 18f)
        lineTo(3f, 18f)
        lineTo(2f, 15f)
        lineTo(2f, 9f)
        close()
    }.build()

    // === SYSTEM BELL VARIANTS ===

    val CupertinoBell = ImageVector.Builder(
        name = "CupertinoBell",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White), pathFillType = PathFillType.EvenOdd) {
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
    }.build()

    val MaterialBell = ImageVector.Builder(
        name = "MaterialBell",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
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
        close()
    }.build()

    val CyberpunkBell = ImageVector.Builder(
        name = "CyberpunkBell",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
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
    }.build()

    // === SYSTEM CONNECTIVITY VARIANTS ===

    val OxygenWifi = ImageVector.Builder(
        name = "OxygenWifi",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(stroke = SolidColor(Color.White), strokeLineWidth = 1.5f) {
        moveTo(12f, 18f)
        lineTo(12.01f, 18.01f)
        moveTo(8.5f, 14.5f)
        curveTo(10.43f, 12.57f, 13.57f, 12.57f, 15.5f, 14.5f)
        moveTo(5f, 11f)
        curveTo(8.87f, 7.13f, 15.13f, 7.13f, 19f, 11f)
        moveTo(1.5f, 7.5f)
        curveTo(7.3f, 1.7f, 16.7f, 1.7f, 22.5f, 7.5f)
    }.build()

    val CyberpunkWifi = ImageVector.Builder(
        name = "CyberpunkWifi",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(12f, 21f)
        lineTo(9f, 18f)
        lineTo(12f, 15f)
        lineTo(15f, 18f)
        close()
        moveTo(6f, 15f)
        lineTo(3f, 12f)
        lineTo(21f, 12f)
        lineTo(18f, 15f)
        close()
        moveTo(0f, 9f)
        lineTo(24f, 9f)
        lineTo(22f, 6f)
        lineTo(2f, 6f)
        close()
    }.build()

    // === PERFORMANCE VARIANTS ===

    val FuturisticRAM = ImageVector.Builder(
        name = "FuturisticRAM",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(stroke = SolidColor(Color.White), strokeLineWidth = 2f) {
        moveTo(2f, 6f)
        horizontalLineTo(22f)
        verticalLineTo(18f)
        horizontalLineTo(2f)
        close()
    }.path(fill = SolidColor(Color.White)) {
        moveTo(4f, 18f)
        verticalLineTo(21f)
        horizontalLineTo(6f)
        verticalLineTo(18f)
        close()
        moveTo(9f, 18f)
        verticalLineTo(21f)
        horizontalLineTo(11f)
        verticalLineTo(18f)
        close()
        moveTo(14f, 18f)
        verticalLineTo(21f)
        horizontalLineTo(16f)
        verticalLineTo(18f)
        close()
        moveTo(19f, 18f)
        verticalLineTo(21f)
        horizontalLineTo(21f)
        verticalLineTo(18f)
        close()
    }.build()

    val FuturisticCPU = ImageVector.Builder(
        name = "FuturisticCPU",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(stroke = SolidColor(Color.White), strokeLineWidth = 2f) {
        moveTo(5f, 5f)
        horizontalLineTo(19f)
        verticalLineTo(19f)
        horizontalLineTo(5f)
        close()
    }.path(fill = SolidColor(Color.White)) {
        // Top pins
        moveTo(8f, 2f)
        verticalLineTo(5f)
        horizontalLineTo(9f)
        verticalLineTo(2f)
        close()
        moveTo(15f, 2f)
        verticalLineTo(5f)
        horizontalLineTo(16f)
        verticalLineTo(2f)
        close()
        // Right pins
        moveTo(19f, 8f)
        horizontalLineTo(22f)
        verticalLineTo(9f)
        horizontalLineTo(19f)
        close()
        moveTo(19f, 15f)
        horizontalLineTo(22f)
        verticalLineTo(16f)
        horizontalLineTo(19f)
        close()
        // Bottom pins
        moveTo(8f, 19f)
        verticalLineTo(22f)
        horizontalLineTo(9f)
        verticalLineTo(19f)
        close()
        moveTo(15f, 19f)
        verticalLineTo(22f)
        horizontalLineTo(16f)
        verticalLineTo(19f)
        close()
        // Left pins
        moveTo(2f, 8f)
        horizontalLineTo(5f)
        verticalLineTo(9f)
        horizontalLineTo(2f)
        close()
        moveTo(2f, 15f)
        horizontalLineTo(5f)
        verticalLineTo(16f)
        horizontalLineTo(2f)
        close()
    }.build()
}
