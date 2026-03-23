package com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_4

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.alexbar3073.bluetoothcar.R
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.CarData
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun DashboardType4Fuel(
    modifier: Modifier = Modifier,
    carData: CarData,
    appSettings: AppSettings?,
    geometry: DashboardType4Geometry
) {
    val fuelTankCapacity = appSettings?.fuelTankCapacity ?: 60f
    val currentFuel = carData.fuel.coerceIn(0f, fuelTankCapacity)
    val fuelRatio = currentFuel / fuelTankCapacity
    val startAngle = 45f
    val sweepAngle = 135f

    val animatedFuelRatio by animateFloatAsState(targetValue = fuelRatio, animationSpec = spring(dampingRatio = 0.8f, stiffness = 80f))
    val iconPainter = painterResource(R.drawable.fuel_50)

    val bitmapKey = remember(geometry.width, geometry.height) { Pair(geometry.width, geometry.height) }
    var backgroundBitmap by remember(bitmapKey) { mutableStateOf<ImageBitmap?>(null) }
    var needleBitmap by remember(bitmapKey) { mutableStateOf<ImageBitmap?>(null) }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val hubRadius = 20f * geometry.unit
            val needleAngle = startAngle + sweepAngle * animatedFuelRatio
            val startNorm = startAngle / 360f; val endNorm = (startAngle + sweepAngle) / 360f; val needleNorm = needleAngle / 360f
            drawArc(brush = Brush.sweepGradient(colorStops = arrayOf(0f to Color.Transparent, (startNorm - 0.001f) to Color.Transparent, startNorm to Color.White.copy(alpha = 0.1f), needleNorm to Color.White, endNorm to Color.White.copy(alpha = 0.1f), (endNorm + 0.001f) to Color.Transparent, 1f to Color.Transparent), center = geometry.center), startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, topLeft = Offset(geometry.center.x - geometry.outerRingRadius, geometry.center.y - geometry.outerRingRadius), size = Size(geometry.outerRingRadius * 2, geometry.outerRingRadius * 2), style = Stroke(width = geometry.outerStrokeWidth))
            drawImage(image = backgroundBitmap ?: buildBackgroundBitmap(geometry, startAngle, sweepAngle, hubRadius, iconPainter).also { backgroundBitmap = it })
            drawFuelValueText(geometry, currentFuel.toInt(), hubRadius)
            rotate(needleAngle, pivot = geometry.center) { drawImage(image = needleBitmap ?: buildNeedleBitmap(geometry, hubRadius).also { needleBitmap = it }) }
        }
    }
}

private fun DrawScope.drawFuelValueText(geometry: DashboardType4Geometry, fuel: Int, hubRadius: Float) {
    val areaRight = geometry.center.x + geometry.outerRingRadius - 10f * geometry.unit; val valText = fuel.toString(); val unitText = "L"
    drawContext.canvas.nativeCanvas.apply {
        save()
        val valuePaint = android.graphics.Paint().apply { color = Color.White.copy(alpha = 0.8f).toArgb(); textSize = hubRadius * 1.3f; isAntiAlias = true; typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD) }
        val unitPaint = android.graphics.Paint().apply { color = Color.White.copy(alpha = 0.8f).toArgb(); textSize = valuePaint.textSize * 0.8f; isAntiAlias = true }
        val totalWidth = valuePaint.measureText(valText) + unitPaint.measureText(unitText) + 2f * geometry.unit; val startX = areaRight - totalWidth - 8f * geometry.unit; val baselineY = geometry.center.y + hubRadius * 1.1f - (valuePaint.fontMetrics.ascent + valuePaint.fontMetrics.descent) / 2f
        drawText(valText, startX, baselineY, valuePaint); drawText(unitText, startX + valuePaint.measureText(valText) + 2f * geometry.unit, baselineY, unitPaint)
        restore()
    }
}

private fun buildBackgroundBitmap(geometry: DashboardType4Geometry, startAngle: Float, sweepAngle: Float, hubRadius: Float, iconPainter: Painter): ImageBitmap {
    val bitmap = ImageBitmap(geometry.width.toInt(), geometry.height.toInt())
    val drawScope = CanvasDrawScope()
    drawScope.draw(geometry.density, LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(bitmap), Size(geometry.width, geometry.height)) {
        val textPaint = android.graphics.Paint().apply { color = geometry.ringColor.toArgb(); textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true }
        for (i in 0..4) {
            val angle = startAngle + sweepAngle * (i / 4f); val rad = Math.toRadians(angle.toDouble()).toFloat(); val cos = cos(rad); val sin = sin(rad)
            val isLarge = i % 2 == 0; val tickLen = if (isLarge) geometry.tickLarge else geometry.tickMedium
            drawLine(color = geometry.ringColor, start = Offset(geometry.center.x + cos * geometry.scaleRadius, geometry.center.y + sin * geometry.scaleRadius), end = Offset(geometry.center.x + cos * (geometry.scaleRadius - tickLen), geometry.center.y + sin * (geometry.scaleRadius - tickLen)), strokeWidth = if (isLarge) geometry.tickLargeWidth else geometry.tickMediumWidth, cap = StrokeCap.Round)
            if (isLarge) {
                textPaint.textSize = geometry.textSizePx * 0.85f * 1.4f; val label = when(i) { 0 -> "0"; 2 -> "1/2"; 4 -> "1"; else -> "" }
                drawContext.canvas.nativeCanvas.apply { save(); translate(geometry.center.x + cos * (geometry.scaleRadius - tickLen - 12f * geometry.unit), geometry.center.y + sin * (geometry.scaleRadius - tickLen - 12f * geometry.unit)); rotate(angle + 90f); drawText(label, 0f, -(textPaint.fontMetrics.ascent + textPaint.fontMetrics.descent) / 2f, textPaint); restore() }
            }
        }
        val areaLeft = geometry.center.x + hubRadius + 20f * geometry.unit; val areaRight = geometry.center.x + geometry.outerRingRadius - 10f * geometry.unit
        if (areaRight > areaLeft) {
            val areaRect = RoundRect(areaLeft, geometry.center.y + hubRadius * 0.1f, areaRight, geometry.center.y + hubRadius * 2.1f, CornerRadius(6f * geometry.unit))
            drawPath(Path().apply { addRoundRect(areaRect) }, Color(0xFF080808))
            drawPath(path = Path().apply { addRoundRect(areaRect) }, brush = Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.15f)), start = Offset(areaRect.left, areaRect.top), end = Offset(areaRect.right, areaRect.bottom)), style = Stroke(width = 2.2f * geometry.unit))
        }
        val iconSize = 33.6f * geometry.unit; val iconRadius = geometry.scaleRadius / 2f
        translate(geometry.center.x - iconSize / 2f, geometry.center.y + iconRadius - iconSize / 2f) { with(iconPainter) { draw(Size(iconSize, iconSize), colorFilter = ColorFilter.tint(geometry.ringColor)) } }
    }
    return bitmap
}

private fun buildNeedleBitmap(geometry: DashboardType4Geometry, hubRadius: Float): ImageBitmap {
    val bitmap = ImageBitmap(geometry.width.toInt(), geometry.height.toInt())
    val drawScope = CanvasDrawScope()
    drawScope.draw(geometry.density, LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(bitmap), Size(geometry.width, geometry.height)) {
        drawCircle(brush = Brush.radialGradient(colors = listOf(geometry.ringColor.copy(alpha = 0.4f), Color.Transparent), center = geometry.center, radius = hubRadius * 3f), radius = hubRadius * 3f, center = geometry.center)
        val tip = Offset(geometry.center.x + geometry.scaleRadius - 2f * geometry.unit, geometry.center.y)
        drawLine(brush = Brush.linearGradient(colors = listOf(geometry.ringColor.copy(alpha = 0f), geometry.ringColor.copy(alpha = 0.6f)), start = geometry.center, end = tip), start = Offset(geometry.center.x + hubRadius * 0.5f, geometry.center.y), end = tip, strokeWidth = 4.5f * geometry.unit, cap = StrokeCap.Round)
        drawLine(brush = Brush.linearGradient(colors = listOf(Color.Transparent, Color.White), start = Offset(geometry.center.x + hubRadius * 0.5f, geometry.center.y), end = tip), start = Offset(geometry.center.x + hubRadius * 0.5f, geometry.center.y), end = tip, strokeWidth = 1.3f * geometry.unit, cap = StrokeCap.Round)
        drawCircle(brush = Brush.radialGradient(colors = listOf(Color(0xFF2A2A2A), Color(0xFF0F0F0F)), center = geometry.center, radius = hubRadius), radius = hubRadius, center = geometry.center)
        drawCircle(brush = Brush.sweepGradient(colorStops = arrayOf(0.0f to Color.White.copy(alpha = 0.1f), 0.25f to Color.White.copy(alpha = 0.45f), 0.5f to Color.White.copy(alpha = 0.1f), 0.75f to Color.Black.copy(alpha = 0.4f), 1.0f to Color.White.copy(alpha = 0.1f)), center = geometry.center), radius = hubRadius - 0.5f * geometry.unit, center = geometry.center, style = Stroke(width = 1.5f * geometry.unit))
        drawCircle(color = Color.White.copy(alpha = 0.2f), radius = 1.5f * geometry.unit, center = geometry.center)
    }
    return bitmap
}
