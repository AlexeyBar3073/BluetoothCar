package com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_1

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class Geometry(
    // Базовые параметры
    val unit: Float,
    val center: Offset,
    val width: Float,
    val height: Float,
    val margin: Float,                 // НОВОЕ: отступ от края Canvas
    val gapScale: Float,
    val maxRadius: Float,
    val outerStrokeWidth: Float,
    val outerRingRadius: Float,
    val scaleRadius: Float,
    val tickSmall: Float,
    val tickMedium: Float,
    val tickLarge: Float,
    val textOffsetFromTick: Float,
    val textSizePx: Float,
    val textRadius: Float,
    val glowGap: Float,
    val glowWidth: Float,
    val ringRadius: Float,
    val blackStrokeWidth: Float,
    val blackRadius: Float,
    val startAngle: Float,
    val fullSweep: Float,
    val maxSpeed: Float,
    val density: Density,
    val ringColor: Color,
    val logTag: String,
    val trailAlphaStops: List<Float>,

    // Радиусы для большой окружности
    val bigMaxRadius: Float,
    val bigOuterRingRadius: Float,
    val bigScaleRadius: Float,
    val bigTextRadius: Float
) {
    companion object {
        fun fromSize(
            size: Size,
            density: Density,
            ringColor: Color = Color(0xFFFC4903),
            logTag: String = "DashboardType1"
        ): Geometry {
            with(density) {
                val unit = 1.dp.toPx()
                val margin = 3f * unit
                val center = Offset(size.width / 2f, size.height / 2f)

                // Базовые параметры
                val outerStrokeWidth = 2f * unit
                val gapScale = 8f * unit
                val tickSmall = 6f * unit
                val tickMedium = 10f * unit
                val tickLarge = 14f * unit
                val textOffsetFromTick = 12f * unit
                val textSizePx = 18.sp.toPx()
                val glowGap = 10f * unit
                val glowWidth = 65f
                val blackStrokeWidth = 8f * unit

                // Радиусы для малой окружности (спидометр)
                val maxRadius = size.minDimension / 2f - margin  // ← используем margin
                val outerRingRadius = maxRadius - outerStrokeWidth / 2f
                val scaleRadius = outerRingRadius - outerStrokeWidth / 2f - gapScale
                val textRadius = scaleRadius - tickLarge - textOffsetFromTick

                // Радиусы для большой окружности (боковые виджеты)
                val bigMaxRadius = size.maxDimension / 2f - margin  // ← используем margin
                val bigOuterRingRadius = bigMaxRadius - outerStrokeWidth / 2f
                val bigScaleRadius = bigOuterRingRadius - outerStrokeWidth / 2f - gapScale
                val bigTextRadius = bigScaleRadius - tickLarge - textOffsetFromTick

                // Остальные параметры
                val outerGlowRadius = textRadius - glowGap
                val ringRadius = outerGlowRadius - glowWidth
                val innerBlackRadius = ringRadius + 1f * unit
                val blackRadius = innerBlackRadius + blackStrokeWidth / 2f

                val startAngle = 150f
                val fullSweep = 240f
                val maxSpeed = 220f

                val trailAlphaStops = listOf(
                    0.062f, 0.123f, 0.185f, 0.246f, 0.308f, 0.370f,
                    0.431f, 0.493f, 0.554f, 0.616f, 0.678f, 0.739f
                )

                return Geometry(
                    unit = unit,
                    center = center,
                    width = size.width,
                    height = size.height,
                    margin = margin,
                    gapScale = gapScale,
                    maxRadius = maxRadius,
                    outerStrokeWidth = outerStrokeWidth,
                    outerRingRadius = outerRingRadius,
                    scaleRadius = scaleRadius,
                    tickSmall = tickSmall,
                    tickMedium = tickMedium,
                    tickLarge = tickLarge,
                    textOffsetFromTick = textOffsetFromTick,
                    textSizePx = textSizePx,
                    textRadius = textRadius,
                    glowGap = glowGap,
                    glowWidth = glowWidth,
                    ringRadius = ringRadius,
                    blackStrokeWidth = blackStrokeWidth,
                    blackRadius = blackRadius,
                    startAngle = startAngle,
                    fullSweep = fullSweep,
                    maxSpeed = maxSpeed,
                    density = density,
                    ringColor = ringColor,
                    logTag = logTag,
                    trailAlphaStops = trailAlphaStops,
                    bigMaxRadius = bigMaxRadius,
                    bigOuterRingRadius = bigOuterRingRadius,
                    bigScaleRadius = bigScaleRadius,
                    bigTextRadius = bigTextRadius
                )
            }
        }
    }
}