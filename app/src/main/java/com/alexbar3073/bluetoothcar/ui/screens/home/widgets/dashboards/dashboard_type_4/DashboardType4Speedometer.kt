package com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_4

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import com.alexbar3073.bluetoothcar.R
import com.alexbar3073.bluetoothcar.data.models.CarData
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
internal fun DashboardType4Speedometer(
    modifier: Modifier = Modifier,
    carData: CarData,
    geometry: DashboardType4Geometry
) {
    val maxSpeed = 220f
    val maxVoltage = 16f
    var startupFinished by rememberSaveable { mutableStateOf(false) }
    val startupAnim = remember { Animatable(0f) }
    val transitionAnim = remember { Animatable(0f) }

    LaunchedEffect(startupFinished) {
        if (!startupFinished) {
            startupAnim.animateTo(maxSpeed, animationSpec = tween(1300, easing = FastOutSlowInEasing))
            startupAnim.animateTo(0f, animationSpec = tween(5000, easing = LinearOutSlowInEasing))
            transitionAnim.animateTo(1f, animationSpec = tween(1500, easing = FastOutSlowInEasing))
            startupFinished = true
        }
    }

    val displaySpeed = if (!startupFinished) {
        if (transitionAnim.value > 0f) max(0f, carData.speed) * transitionAnim.value else startupAnim.value
    } else carData.speed

    val displayVoltage = if (!startupFinished) {
        if (transitionAnim.value > 0f) max(0f, carData.voltage) * transitionAnim.value else startupAnim.value / maxSpeed * maxVoltage
    } else carData.voltage

    val animatedSpeed by animateFloatAsState(targetValue = displaySpeed.coerceIn(0f, maxSpeed), animationSpec = spring(dampingRatio = 0.82f, stiffness = 90f))
    val animatedVoltage by animateFloatAsState(targetValue = displayVoltage.coerceIn(0f, maxVoltage), animationSpec = spring(dampingRatio = 0.82f, stiffness = 90f))

    val isLowVoltage = animatedVoltage <= 11.8f
    val isCharging = animatedVoltage > 13.0f
    val currentIcon = if (isCharging) painterResource(R.drawable.battery_charging_50) else if (isLowVoltage) painterResource(R.drawable.battery_full_48) else painterResource(R.drawable.battery_50)

    val blinkAlpha by animateFloatAsState(targetValue = if (isLowVoltage) 0.3f else 1f, animationSpec = if (isLowVoltage) infiniteRepeatable(animation = tween(500)) else tween(0))

    val speedPaint = remember { android.graphics.Paint().apply { color = Color.White.toArgb(); textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true; isAntiAlias = true } }
    val unitPaint = remember { android.graphics.Paint().apply { color = Color.Gray.toArgb(); textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true } }

    var ringRotation by remember { mutableFloatStateOf(0f) }
    val anglesState = remember(animatedVoltage, ringRotation) { derivedStateOf { calculateAngles(animatedVoltage, ringRotation) } }
    val angles by anglesState
    LaunchedEffect(angles.ringRotation) { if (angles.ringRotation != ringRotation) ringRotation = angles.ringRotation }

    val bitmapKey = remember(geometry.width, geometry.height) { Pair(geometry.width, geometry.height) }
    var scaleBitmap by remember(bitmapKey) { mutableStateOf<ImageBitmap?>(null) }
    var needleBitmap by remember(bitmapKey) { mutableStateOf<ImageBitmap?>(null) }
    var ringBitmap by remember(bitmapKey) { mutableStateOf<ImageBitmap?>(null) }

    val trailCache = rememberTrailCache(geometry)
    val windowPath = rememberVoltmeterWindow(geometry)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawVoltmeter(geometry, animatedVoltage, ringBitmap ?: buildVoltageRingBitmap(geometry).also { ringBitmap = it }, ringRotation, currentIcon, if (isLowVoltage) blinkAlpha else 1f, angles.needleAngle, windowPath)
            drawSpeedometer(geometry, animatedSpeed, scaleBitmap ?: buildScaleBitmap(geometry, geometry.ringColor).also { scaleBitmap = it }, needleBitmap ?: buildNeedleBitmap(geometry, geometry.ringColor).also { needleBitmap = it }, geometry.ringColor, speedPaint, unitPaint, trailCache)
        }
    }
}

private fun DrawScope.drawSpeedometer(geometry: DashboardType4Geometry, speed: Float, scaleBitmap: ImageBitmap, needleBitmap: ImageBitmap, ringColor: Color, speedPaint: android.graphics.Paint, unitPaint: android.graphics.Paint, trailCache: TrailCache) {
    val needleAngle = geometry.startAngle + geometry.fullSweep * (speed / geometry.maxSpeed)
    drawImage(scaleBitmap, Offset.Zero)
    drawDualRadialGlow(geometry, speed, ringColor)
    rotate(needleAngle, pivot = geometry.center) { drawImage(needleBitmap, Offset.Zero) }
    drawSpeedText(speed, geometry, speedPaint, unitPaint)
    drawSpeedometerTrail(geometry, speed, trailCache)
}

private data class TrailCache(val innerRadius: Float, val radiusClipPath: Path)

@Composable
private fun rememberTrailCache(geometry: DashboardType4Geometry): TrailCache {
    return remember(geometry.width, geometry.height) {
        val outerRadius = geometry.scaleRadius
        val radiusClipPath = Path().apply { addOval(Rect(geometry.center.x - outerRadius, geometry.center.y - outerRadius, geometry.center.x + outerRadius, geometry.center.y + outerRadius)) }
        TrailCache(outerRadius - geometry.tickLarge, radiusClipPath)
    }
}

@Composable
private fun rememberVoltmeterWindow(geometry: DashboardType4Geometry): Path {
    return remember(geometry.width, geometry.height) {
        val radius = geometry.scaleRadius
        Path().apply { arcTo(Rect(geometry.center.x - radius, geometry.center.y - radius, geometry.center.x + radius, geometry.center.y + radius), 40f, 100f, true); lineTo(geometry.center.x, geometry.center.y); close() }
    }
}

private fun DrawScope.drawSpeedometerTrail(geometry: DashboardType4Geometry, speed: Float, trailCache: TrailCache) {
    val sweepAngle = geometry.fullSweep * (speed / geometry.maxSpeed)
    if (sweepAngle <= 0f) return
    val color = getTrailColor(speed)
    val currentAngle = geometry.startAngle + sweepAngle
    val angleRad = currentAngle * PI.toFloat() / 180f
    val cosA = cos(angleRad)
    val sinA = sin(angleRad)
    val needleEnd = Offset(geometry.center.x + geometry.scaleRadius * cosA, geometry.center.y + geometry.scaleRadius * sinA)
    val bigRadius = geometry.scaleRadius * 1.1f
    val bigInnerRadius = bigRadius - geometry.tickLarge
    val bigCircleCenter = Offset(needleEnd.x - bigRadius * cosA, needleEnd.y - bigRadius * sinA)
    val innerStop = bigInnerRadius / bigRadius
    val colorStops = geometry.trailAlphaStops.mapIndexed { i, alpha -> (innerStop + i * (1f - innerStop) / (geometry.trailAlphaStops.size - 1)) to color.copy(alpha = alpha) }.toTypedArray()
    val radialBrush = Brush.radialGradient(colorStops = colorStops, center = bigCircleCenter, radius = bigRadius)
    clipPath(trailCache.radiusClipPath) {
        val sectorClipPath = Path().apply { arcTo(Rect(geometry.center.x - bigRadius, geometry.center.y - bigRadius, geometry.center.x + bigRadius, geometry.center.y + bigRadius), geometry.startAngle, sweepAngle, true); lineTo(geometry.center.x, geometry.center.y); close() }
        clipPath(sectorClipPath) { drawCircle(brush = radialBrush, center = bigCircleCenter, radius = bigRadius - geometry.tickLarge / 2f, style = Stroke(width = geometry.tickLarge)) }
    }
    drawTrailBorder(geometry, sweepAngle, color)
}

private fun getTrailColor(speed: Float): Color {
    val white = Color.White; val green = Color.Green; val yellow = Color.Yellow; val orange = Color(0xFFFFA500); val red = Color.Red; val burgundy = Color(0xFF800000)
    return when {
        speed < 30f -> white
        speed in 30f..50f -> lerp(white, green, (speed - 30f) / 20f)
        speed < 60f -> green
        speed in 60f..80f -> lerp(green, yellow, (speed - 60f) / 20f)
        speed < 90f -> yellow
        speed in 90f..110f -> lerp(yellow, orange, (speed - 90f) / 20f)
        speed < 130f -> orange
        speed in 130f..150f -> lerp(orange, red, (speed - 130f) / 20f)
        speed < 170f -> red
        speed in 170f..190f -> lerp(red, burgundy, (speed - 170f) / 20f)
        else -> burgundy
    }
}

private fun DrawScope.drawTrailBorder(geometry: DashboardType4Geometry, sweepAngle: Float, color: Color) {
    val borderWidth = 1f * geometry.unit; val borderRadius = geometry.scaleRadius + borderWidth / 2f
    drawArc(brush = Brush.sweepGradient(colors = listOf(color.copy(alpha = 0f), color.copy(alpha = 1f)), center = geometry.center), startAngle = geometry.startAngle, sweepAngle = sweepAngle, useCenter = false, topLeft = Offset(geometry.center.x - borderRadius, geometry.center.y - borderRadius), size = Size(borderRadius * 2, borderRadius * 2), style = Stroke(width = borderWidth))
}

private fun DrawScope.drawSpeedText(speed: Float, geometry: DashboardType4Geometry, speedPaint: android.graphics.Paint, unitPaint: android.graphics.Paint) {
    speedPaint.textSize = 42f * geometry.unit; unitPaint.textSize = 16f * geometry.unit
    drawContext.canvas.nativeCanvas.apply {
        val speedStr = speed.toInt().toString(); val fm = speedPaint.fontMetrics; val baseline = geometry.center.y - (fm.ascent + fm.descent) / 2f
        drawText(speedStr, geometry.center.x, baseline, speedPaint); drawText("км/ч", geometry.center.x, baseline + 32f * geometry.unit, unitPaint)
    }
}

private fun DrawScope.drawDualRadialGlow(geometry: DashboardType4Geometry, speed: Float, baseColor: Color) {
    val innerRadius = geometry.ringRadius - geometry.glowWidth; val outerRadius = geometry.ringRadius + geometry.glowWidth
    if (innerRadius <= 0f) return
    val glowColor = if (speed <= 120f) baseColor else lerp(baseColor, Color.Red, ((speed - 120f) / 100f).coerceIn(0f, 1f))
    val innerStop = innerRadius / outerRadius; val peakStop = geometry.ringRadius / outerRadius
    val brush = Brush.radialGradient(colorStops = arrayOf(innerStop to Color.Transparent, (peakStop - 0.01f) to glowColor.copy(alpha = 0.6f), peakStop to glowColor, (peakStop + 0.01f) to glowColor.copy(alpha = 0.6f), 1f to Color.Transparent), center = geometry.center, radius = outerRadius)
    drawCircle(brush = brush, center = geometry.center, radius = (outerRadius + innerRadius) / 2f, style = Stroke(width = outerRadius - innerRadius))
}

private const val BASE_MIN_VOLTAGE = 12.0f
private const val BASE_MAX_VOLTAGE = 12.7f
private const val BASE_START_ANGLE = 140f
private const val BASE_SWEEP_ANGLE = -100f
private const val EXTENSION_DEGREES = 130f

private data class VoltageAngles(val needleAngle: Float, val ringRotation: Float)

private fun DrawScope.drawVoltmeter(geometry: DashboardType4Geometry, voltage: Float, ringBitmap: ImageBitmap, ringRotation: Float, iconPainter: Painter, alpha: Float, needleAngle: Float, windowPath: Path) {
    clipPath(windowPath) { rotate(ringRotation, pivot = geometry.center) { drawImage(ringBitmap, Offset(geometry.center.x - ringBitmap.width / 2f, geometry.center.y - ringBitmap.height / 2f)) } }
    drawBatteryIconAndText(geometry, voltage, iconPainter, alpha)
    drawVoltageNeedle(geometry, needleAngle, voltageToColor(voltage))
}

private fun calculateAngles(voltage: Float, currentRingRotation: Float): VoltageAngles {
    val baseMinAngle = BASE_START_ANGLE; val baseMaxAngle = BASE_START_ANGLE + BASE_SWEEP_ANGLE
    val degreesPerVolt = abs(BASE_SWEEP_ANGLE) / (BASE_MAX_VOLTAGE - BASE_MIN_VOLTAGE)
    val voltageAngleAbsolute = BASE_START_ANGLE - (voltage - BASE_MIN_VOLTAGE) * degreesPerVolt
    return if (currentRingRotation != 0f) {
        val limiter = if (currentRingRotation > 0f) baseMaxAngle else baseMinAngle
        var newRotation = (currentRingRotation - (voltageAngleAbsolute + currentRingRotation - limiter)).coerceIn(-EXTENSION_DEGREES, EXTENSION_DEGREES)
        if (abs(newRotation) < 0.0001f) newRotation = 0f
        VoltageAngles(limiter, newRotation)
    } else {
        val clampedAngle = voltageAngleAbsolute.coerceIn(baseMaxAngle, baseMinAngle)
        var newRotation = (clampedAngle - voltageAngleAbsolute).coerceIn(-EXTENSION_DEGREES, EXTENSION_DEGREES)
        if (abs(newRotation) < 0.0001f) newRotation = 0f
        VoltageAngles(clampedAngle, newRotation)
    }
}

private fun DrawScope.drawVoltageNeedle(geometry: DashboardType4Geometry, needleAngle: Float, ringColor: Color) {
    val angleRad = Math.toRadians(needleAngle.toDouble()).toFloat()
    val cos = cos(angleRad); val sin = sin(angleRad)
    val start = Offset(geometry.center.x + cos * geometry.textRadius, geometry.center.y + sin * geometry.textRadius)
    val tip = Offset(geometry.center.x + cos * (geometry.scaleRadius - 2f * geometry.unit), geometry.center.y + sin * (geometry.scaleRadius - 2f * geometry.unit))
    drawLine(brush = Brush.linearGradient(colors = listOf(ringColor.copy(alpha = 0f), ringColor.copy(alpha = 0.5f)), start = start, end = tip), start = start, end = tip, strokeWidth = 5f * geometry.unit, cap = StrokeCap.Round)
    drawLine(brush = Brush.linearGradient(colors = listOf(Color.Transparent, Color.White), start = start, end = tip), start = start, end = tip, strokeWidth = 1.5f * geometry.unit, cap = StrokeCap.Round)
}

private fun voltageToColor(v: Float): Color = when {
    v <= 11.8f -> Color.Red
    v <= 12.0f -> lerp(Color(0xFF800000), Color.Red, ((v - 11.8f) / 0.2f).coerceIn(0f, 1f))
    v <= 12.2f -> lerp(Color.Red, Color.Yellow, ((v - 12.0f) / 0.2f).coerceIn(0f, 1f))
    v <= 12.5f -> lerp(Color.Yellow, Color.Green, ((v - 12.2f) / 0.3f).coerceIn(0f, 1f))
    v <= 12.75f -> lerp(Color.Green, Color(0xFF1B5E20), ((v - 12.5f) / 0.25f).coerceIn(0f, 1f))
    v <= 13.1f -> lerp(Color(0xFF1B5E20), Color(0xFF0D47A1), ((v - 12.75f) / 0.35f).coerceIn(0f, 1f))
    else -> Color(0xFF3378D8)
}

private fun buildVoltageRingBitmap(geometry: DashboardType4Geometry): ImageBitmap {
    val bitmap = ImageBitmap(geometry.width.toInt(), geometry.height.toInt())
    val drawScope = CanvasDrawScope()
    drawScope.draw(geometry.density, LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(bitmap), Size(geometry.width, geometry.height)) {
        val ringWidth = geometry.tickLarge; val ringOuterRadius = geometry.scaleRadius - ringWidth / 2f
        rotate(270f) { drawArc(brush = Brush.sweepGradient(colorStops = arrayOf(0f to Color(0xFF0D47A1), 0.5f to Color.Green, 1f to Color(0xFF800000))), startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = Offset(geometry.center.x - ringOuterRadius, geometry.center.y - ringOuterRadius), size = Size(ringOuterRadius * 2, ringOuterRadius * 2), style = Stroke(width = ringWidth)) }
    }
    return bitmap
}

private fun buildScaleBitmap(geometry: DashboardType4Geometry, ringColor: Color): ImageBitmap {
    val bitmap = ImageBitmap(geometry.width.toInt(), geometry.height.toInt())
    val drawScope = CanvasDrawScope()
    drawScope.draw(geometry.density, LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(bitmap), Size(geometry.width, geometry.height)) {
        drawCircle(Color.White, geometry.outerRingRadius, geometry.center, style = Stroke(geometry.outerStrokeWidth))
        val textPaint = android.graphics.Paint().apply { color = ringColor.toArgb(); textAlign = android.graphics.Paint.Align.CENTER; textSize = geometry.textSizePx; isAntiAlias = true }
        for (i in 0..geometry.maxSpeed.toInt() step 2) {
            val angle = geometry.startAngle + geometry.fullSweep * (i / geometry.maxSpeed)
            val rad = Math.toRadians(angle.toDouble()).toFloat()
            val cos = cos(rad); val sin = sin(rad)
            val is20 = i % 20 == 0; val tickLen = if (is20) geometry.tickLarge else if (i % 10 == 0) geometry.tickMedium else geometry.tickSmall
            drawLine(ringColor, Offset(geometry.center.x + cos * geometry.scaleRadius, geometry.center.y + sin * geometry.scaleRadius), Offset(geometry.center.x + cos * (geometry.scaleRadius - tickLen), geometry.center.y + sin * (geometry.scaleRadius - tickLen)), if (is20) geometry.tickLargeWidth else geometry.tickSmallWidth, StrokeCap.Round)
            if (is20) drawContext.canvas.nativeCanvas.apply { save(); translate(geometry.center.x + cos * geometry.textRadius, geometry.center.y + sin * geometry.textRadius); rotate(angle + 90f); drawText(i.toString(), 0f, -(textPaint.fontMetrics.ascent + textPaint.fontMetrics.descent) / 2f, textPaint); restore() }
        }
    }
    return bitmap
}

private fun buildNeedleBitmap(geometry: DashboardType4Geometry, ringColor: Color): ImageBitmap {
    val bitmap = ImageBitmap(geometry.width.toInt(), geometry.height.toInt())
    val drawScope = CanvasDrawScope()
    drawScope.draw(geometry.density, LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(bitmap), Size(geometry.width, geometry.height)) {
        drawCircle(Color.Black, geometry.blackRadius, geometry.center, style = Stroke(geometry.blackStrokeWidth))
        val tip = Offset(geometry.center.x + geometry.scaleRadius - geometry.unit, geometry.center.y)
        drawLine(Brush.linearGradient(listOf(ringColor.copy(alpha = 0f), ringColor.copy(alpha = 0.5f)), geometry.center, tip), geometry.center, tip, 5f * geometry.unit, StrokeCap.Round)
        drawLine(Brush.linearGradient(listOf(Color.Transparent, Color.White), geometry.center, tip), geometry.center, tip, 1.5f * geometry.unit, StrokeCap.Round)
    }
    return bitmap
}

private fun DrawScope.drawBatteryIconAndText(geometry: DashboardType4Geometry, voltage: Float, iconPainter: Painter, alpha: Float = 1f) {
    val iconSize = geometry.textSizePx; val voltageText = String.format(Locale.US, "%.1fV", voltage)
    val textPaint = android.graphics.Paint().apply { color = Color.Gray.toArgb(); textSize = geometry.textSizePx * 0.8f; isAntiAlias = true }
    val totalWidth = iconSize + 4f * geometry.unit + textPaint.measureText(voltageText)
    val blockCenterX = geometry.center.x; val blockCenterY = geometry.center.y + geometry.textRadius - 5f * geometry.unit
    val blockLeft = blockCenterX - totalWidth / 2f
    translate(blockLeft, blockCenterY - iconSize / 2f) { with(iconPainter) { draw(Size(iconSize, iconSize), alpha = alpha, colorFilter = ColorFilter.tint(voltageToColor(voltage))) } }
    drawContext.canvas.nativeCanvas.drawText(voltageText, blockLeft + iconSize + 4f * geometry.unit, blockCenterY - (textPaint.ascent() + textPaint.descent()) / 2f, textPaint)
}
