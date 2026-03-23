package com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_4

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import kotlin.math.sqrt

internal data class DashboardType4Geometry(
    val unit: Float,
    val center: Offset,
    val width: Float,
    val height: Float,
    val margin: Float,
    val gapScale: Float,
    val maxRadius: Float,
    val outerStrokeWidth: Float,
    val outerRingRadius: Float,
    val scaleRadius: Float,
    val tickSmall: Float,
    val tickMedium: Float,
    val tickLarge: Float,
    val tickSmallWidth: Float,
    val tickMediumWidth: Float,
    val tickLargeWidth: Float,
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
    val tripArcRadius: Float
) {
    companion object {
        fun fromSize(
            size: Size,
            density: Density,
            ringColor: Color = Color(0xFFFC4903),
            logTag: String = "DashboardType4"
        ): DashboardType4Geometry {
            val canvasHeight = size.minDimension
            val unit = canvasHeight / 288f
            val margin = 3f * unit
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerStrokeWidth = 2f * unit
            val gapScale = 8f * unit
            val tickSmall = 6f * unit
            val tickMedium = 10f * unit
            val tickLarge = 14f * unit
            val tickSmallWidth = 1f * unit
            val tickMediumWidth = 1.5f * unit
            val tickLargeWidth = 2f * unit
            val textOffsetFromTick = 12f * unit
            val textSizePx = 18f * unit 
            val glowGap = 10f * unit
            val glowWidth = 21.66f * unit
            val blackStrokeWidth = 8f * unit
            val maxRadius = canvasHeight / 2f - margin
            val outerRingRadius = maxRadius - outerStrokeWidth / 2f
            val scaleRadius = outerRingRadius - outerStrokeWidth / 2f - gapScale
            val textRadius = scaleRadius - tickLarge - textOffsetFromTick
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

            val dx = margin - size.width / 2f
            val dy = size.height / 2f
            val tripArcRadius = sqrt(dx * dx + dy * dy)

            return DashboardType4Geometry(
                unit = unit, center = center, width = size.width, height = size.height,
                margin = margin, gapScale = gapScale, maxRadius = maxRadius,
                outerStrokeWidth = outerStrokeWidth, outerRingRadius = outerRingRadius,
                scaleRadius = scaleRadius, tickSmall = tickSmall, tickMedium = tickMedium,
                tickLarge = tickLarge, tickSmallWidth = tickSmallWidth,
                tickMediumWidth = tickMediumWidth, tickLargeWidth = tickLargeWidth,
                textOffsetFromTick = textOffsetFromTick, textSizePx = textSizePx,
                textRadius = textRadius, glowGap = glowGap, glowWidth = glowWidth,
                ringRadius = ringRadius, blackStrokeWidth = blackStrokeWidth,
                blackRadius = blackRadius, startAngle = startAngle,
                fullSweep = fullSweep, maxSpeed = maxSpeed, density = density,
                ringColor = ringColor, logTag = logTag, trailAlphaStops = trailAlphaStops,
                tripArcRadius = tripArcRadius
            )
        }
    }
}
