// Файл: app/src/main/java/com/alexbar3073/bluetoothcar/ui/screens/home/widgets/dashboards/dashboard_type_4/DashboardType4Speedometer.kt
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

/**
 * ТЕГ: Спидометр Дашборда 4
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Отрисовка спидометра и вольтметра для четвертого типа дашборда.
 * Включает в себя основную шкалу скорости, стрелку, текстовое значение скорости
 * и дуговой вольтметр в нижней части прибора с индикацией заряда АКБ.
 *
 * СВЯЗЬ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Вызывается из DashboardType4.kt.
 * 2. Использует DashboardType4Geometry.kt для получения параметров отрисовки.
 * 3. Использует CarData.kt для получения текущих значений скорости и напряжения.
 */

private const val BASE_MIN_VOLTAGE = 12.0f
private const val BASE_MAX_VOLTAGE = 12.7f
private const val BASE_START_ANGLE = 140f
private const val BASE_SWEEP_ANGLE = -100f
private const val EXTENSION_DEGREES = 130f

private val BURGUNDY = Color(0xFF800000)
private val DARK_GREEN = Color(0xFF1B5E20)
private val DARK_BLUE = Color(0xFF0D47A1)
private val BRIGHT_RED = Color(0xFFFF0000) // Ярко-алый для критических состояний

/**
 * Вспомогательный класс для хранения углов поворота вольтметра.
 */
private data class VoltageAngles(val needleAngle: Float, val ringRotation: Float)

/**
 * Основной компонент спидометра Type 4.
 * Отрисовывает шкалу, стрелку и вольтметр.
 * Вызывается из: DashboardType4.kt
 */
@Composable
internal fun DashboardType4Speedometer(
    modifier: Modifier = Modifier,
    carData: CarData,
    geometry: DashboardType4Geometry
) {
    val maxSpeed = 220f
    val maxVoltage = 16f
    val startupRiseDuration = 1300
    val startupFallDuration = 5000
    val transitionDuration = 1500

    // Состояние анимации запуска (тест стрелок)
    var startupFinished by rememberSaveable { mutableStateOf(false) }
    val startupAnim = remember { Animatable(0f) }
    val transitionAnim = remember { Animatable(0f) }

    // Запуск анимации при первом появлении
    LaunchedEffect(startupFinished) {
        if (!startupFinished) {
            startupAnim.animateTo(maxSpeed, animationSpec = tween(startupRiseDuration, easing = FastOutSlowInEasing))
            startupAnim.animateTo(0f, animationSpec = tween(startupFallDuration, easing = LinearOutSlowInEasing))
            transitionAnim.animateTo(1f, animationSpec = tween(transitionDuration, easing = FastOutSlowInEasing))
            startupFinished = true
        }
    }

    // Выбор значения для отображения: анимация или реальные данные
    val displaySpeed = if (!startupFinished) {
        if (transitionAnim.value > 0f) (max(0f, carData.speed) * transitionAnim.value) else startupAnim.value
    } else carData.speed

    val displayVoltage = if (!startupFinished) {
        if (transitionAnim.value > 0f) (max(0f, carData.voltage) * transitionAnim.value) else (startupAnim.value / maxSpeed * maxVoltage)
    } else carData.voltage

    // Плавная анимация стрелок
    val animatedSpeed by animateFloatAsState(targetValue = displaySpeed.coerceIn(0f, maxSpeed), animationSpec = spring(0.82f, 90f), label = "needle")
    val animatedVoltage by animateFloatAsState(targetValue = displayVoltage.coerceIn(0f, maxVoltage), animationSpec = spring(0.82f, 90f), label = "voltage")

    // Логика иконок аккумулятора. Используем battery_charging_50 для режима зарядки.
    val isLowVoltage = animatedVoltage <= 11.8f
    val isCharging = animatedVoltage > 13.0f
    val idleIcon = painterResource(R.drawable.ic_battery_full_50)
    val chargingIcon = painterResource(R.drawable.battery_charging_50)
    val currentIcon = if (isCharging) chargingIcon else idleIcon

    // Анимация мигания при низком напряжении (alpha от 0.4 до 1.0 для заметности)
    val blinkAlpha by animateFloatAsState(targetValue = if (isLowVoltage) 0.4f else 1f, animationSpec = if (isLowVoltage) infiniteRepeatable(tween(500)) else tween(0), label = "blink")

    // Настройка кистей для текста
    val speedPaint = remember { android.graphics.Paint().apply { color = Color.White.toArgb(); textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true; isAntiAlias = true } }
    val unitPaint = remember { android.graphics.Paint().apply { color = Color.Gray.toArgb(); textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true } }

    // Расчет вращения шкалы вольтметра
    var ringRotation by remember { mutableFloatStateOf(0f) }
    val anglesState = remember(animatedVoltage, ringRotation) { derivedStateOf { calculateAngles(animatedVoltage, ringRotation) } }
    val angles by anglesState

    LaunchedEffect(angles.ringRotation) { if (angles.ringRotation != ringRotation) ringRotation = angles.ringRotation }

    // Кэширование битмапов для оптимизации
    val bitmapKey = remember(geometry.width, geometry.height) { Pair(geometry.width, geometry.height) }
    var scaleBitmap by remember(bitmapKey) { mutableStateOf<ImageBitmap?>(null) }
    var needleBitmap by remember(bitmapKey) { mutableStateOf<ImageBitmap?>(null) }
    var ringBitmap by remember(bitmapKey) { mutableStateOf<ImageBitmap?>(null) }

    val trailCache = rememberTrailCache(geometry)
    val windowPath = rememberVoltmeterWindow(geometry)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Рисуем вольтметр (нижняя дуга)
            drawVoltmeter(geometry, animatedVoltage, ringBitmap ?: buildVoltageRingBitmap(geometry).also { ringBitmap = it }, ringRotation, currentIcon, if (isLowVoltage) blinkAlpha else 1f, angles.needleAngle, windowPath)
            // Рисуем основной спидометр
            drawSpeedometer(geometry, animatedSpeed, scaleBitmap ?: buildScaleBitmap(geometry, geometry.ringColor).also { scaleBitmap = it }, needleBitmap ?: buildNeedleBitmap(geometry, geometry.ringColor).also { needleBitmap = it }, geometry.ringColor, speedPaint, unitPaint, trailCache)
        }
    }
}

/**
 * Отрисовка основного блока спидометра (шкала, свечение, стрелка, текст).
 * Вызывается из: DashboardType4Speedometer
 */
private fun DrawScope.drawSpeedometer(geometry: DashboardType4Geometry, speed: Float, scaleBitmap: ImageBitmap, needleBitmap: ImageBitmap, ringColor: Color, speedPaint: android.graphics.Paint, unitPaint: android.graphics.Paint, trailCache: TrailCache) {
    val needleAngle = geometry.startAngle + geometry.fullSweep * (speed / geometry.maxSpeed)
    drawImage(scaleBitmap, Offset.Zero)
    drawDualRadialGlow(geometry, speed, ringColor)
    rotate(needleAngle, pivot = geometry.center) { drawImage(needleBitmap, Offset.Zero) }
    drawSpeedText(speed, geometry, speedPaint, unitPaint)
    drawSpeedometerTrail(geometry, speed, trailCache)
}

/**
 * Отрисовка текста текущей скорости в центре прибора.
 * Вызывается из: drawSpeedometer
 */
private fun DrawScope.drawSpeedText(speed: Float, geometry: DashboardType4Geometry, speedPaint: android.graphics.Paint, unitPaint: android.graphics.Paint) {
    speedPaint.textSize = 42f * geometry.unit
    unitPaint.textSize = 16f * geometry.unit
    drawContext.canvas.nativeCanvas.apply {
        val speedStr = speed.toInt().toString()
        val fm = speedPaint.fontMetrics
        val baseline = geometry.center.y - (fm.ascent + fm.descent) / 2f
        drawText(speedStr, geometry.center.x, baseline, speedPaint)
        drawText("км/ч", geometry.center.x, baseline + 32f * geometry.unit, unitPaint)
    }
}

/**
 * Отрисовка компонентов вольтметра (вращающаяся шкала, иконка, текст, мини-стрелка).
 * Вызывается из: DashboardType4Speedometer
 */
private fun DrawScope.drawVoltmeter(geometry: DashboardType4Geometry, voltage: Float, ringBitmap: ImageBitmap, ringRotation: Float, iconPainter: Painter, alpha: Float, needleAngle: Float, windowPath: Path) {
    // Ограничиваем область рисования шкалы "окном" вольтметра
    clipPath(windowPath) {
        rotate(ringRotation, pivot = geometry.center) {
            drawImage(ringBitmap, Offset(geometry.center.x - ringBitmap.width / 2f, geometry.center.y - ringBitmap.height / 2f))
        }
    }
    drawBatteryIconAndText(geometry, voltage, iconPainter, alpha)
    drawVoltageNeedle(geometry, needleAngle, voltageToColor(voltage))
}

/**
 * Отрисовка иконки батареи и текстового значения напряжения в одну горизонтальную линию.
 * Размер иконки зафиксирован для обеспечения единообразия.
 * Вызывается из: drawVoltmeter
 */
private fun DrawScope.drawBatteryIconAndText(geometry: DashboardType4Geometry, voltage: Float, iconPainter: Painter, alpha: Float = 1f) {
    // Используем фиксированный размер иконки (квадрат) на основе textSizePx из геометрии
    val iconSize = geometry.textSizePx
    
    val voltageText = String.format(Locale.US, "%.1fV", voltage)
    val textPaint = android.graphics.Paint().apply { color = Color.Gray.toArgb(); textSize = geometry.textSizePx * 0.8f; isAntiAlias = true }
    
    // Расчитываем общую ширину блока для центрирования
    val spacing = 4f * geometry.unit
    val totalWidth = iconSize + spacing + textPaint.measureText(voltageText)
    
    // Позиционируем блок в нижней части прибора
    val blockCenterX = geometry.center.x
    val blockCenterY = geometry.center.y + geometry.textRadius - 5f * geometry.unit
    val blockLeft = blockCenterX - totalWidth / 2f
    
    // Рисуем иконку (используем фиксированный квадратный размер для единообразия)
    translate(blockLeft, blockCenterY - iconSize / 2f) {
        with(iconPainter) {
            draw(Size(iconSize, iconSize), alpha = alpha, colorFilter = ColorFilter.tint(voltageToColor(voltage)))
        }
    }
    
    // Рисуем текст значения напряжения
    drawContext.canvas.nativeCanvas.drawText(
        voltageText,
        blockLeft + iconSize + spacing,
        blockCenterY - (textPaint.ascent() + textPaint.descent()) / 2f,
        textPaint
    )
}

/**
 * Отрисовка тонкой светящейся стрелки вольтметра.
 * Вызывается из: drawVoltmeter
 */
private fun DrawScope.drawVoltageNeedle(geometry: DashboardType4Geometry, needleAngle: Float, color: Color) {
    val angleRad = Math.toRadians(needleAngle.toDouble()).toFloat()
    val cos = cos(angleRad); val sin = sin(angleRad)
    val innerGlowRadius = geometry.ringRadius - geometry.glowWidth
    val start = Offset(geometry.center.x + cos * innerGlowRadius, geometry.center.y + sin * innerGlowRadius)
    val tip = Offset(geometry.center.x + cos * (geometry.scaleRadius - 2f * geometry.unit), geometry.center.y + sin * (geometry.scaleRadius - 2f * geometry.unit))
    drawLine(Brush.linearGradient(listOf(color.copy(alpha = 0f), color.copy(alpha = 0.5f)), start, tip), start, tip, 5f * geometry.unit, StrokeCap.Round)
    drawLine(Brush.linearGradient(listOf(Color.Transparent, Color.White), start, tip), start, tip, 1.5f * geometry.unit, StrokeCap.Round)
}

/**
 * Преобразование напряжения в цвет для индикации состояния.
 */
private fun voltageToColor(v: Float): Color = when {
    v <= 11.8f -> BRIGHT_RED // Используем ярко-алый для критического уровня
    v <= 12.0f -> lerp(BURGUNDY, Color.Red, ((v - 11.8f) / 0.2f).coerceIn(0f, 1f))
    v <= 12.2f -> lerp(Color.Red, Color.Yellow, ((v - 12.0f) / 0.2f).coerceIn(0f, 1f))
    v <= 12.5f -> lerp(Color.Yellow, Color.Green, ((v - 12.2f) / 0.3f).coerceIn(0f, 1f))
    v <= 12.75f -> lerp(Color.Green, DARK_GREEN, ((v - 12.5f) / 0.25f).coerceIn(0f, 1f))
    v <= 13.1f -> lerp(DARK_GREEN, DARK_BLUE, ((v - 12.75f) / 0.35f).coerceIn(0f, 1f))
    else -> Color(0xFF3378D8)
}

/**
 * Кэширование параметров шлейфа спидометра.
 */
private data class TrailCache(val innerRadius: Float, val radiusClipPath: Path)

/**
 * Создание и запоминание параметров шлейфа.
 */
@Composable
private fun rememberTrailCache(geometry: DashboardType4Geometry): TrailCache = remember(geometry.width, geometry.height) {
    val outerRadius = geometry.scaleRadius
    val path = Path().apply { addOval(Rect(geometry.center.x - outerRadius, geometry.center.y - outerRadius, geometry.center.x + outerRadius, geometry.center.y + outerRadius)) }
    TrailCache(outerRadius - geometry.tickLarge, path)
}

/**
 * Создание пути для ограничения области видимости шкалы вольтметра.
 */
@Composable
private fun rememberVoltmeterWindow(geometry: DashboardType4Geometry): Path = remember(geometry.width, geometry.height) {
    val radius = geometry.scaleRadius
    Path().apply { arcTo(Rect(geometry.center.x - radius, geometry.center.y - radius, geometry.center.x + radius, geometry.center.y + radius), 40f, 100f, true); lineTo(geometry.center.x, geometry.center.y); close() }
}

/**
 * Отрисовка цветного шлейфа за стрелкой спидометра.
 * Вызывается из: drawSpeedometer
 */
private fun DrawScope.drawSpeedometerTrail(geometry: DashboardType4Geometry, speed: Float, trailCache: TrailCache) {
    val sweepAngle = geometry.fullSweep * (speed / geometry.maxSpeed)
    if (sweepAngle <= 0f) return
    val color = getTrailColor(speed)
    val currentAngle = geometry.startAngle + sweepAngle
    val angleRad = currentAngle * PI.toFloat() / 180f
    val cosA = cos(angleRad); val sinA = sin(angleRad)
    val needleEnd = Offset(geometry.center.x + geometry.scaleRadius * cosA, geometry.center.y + geometry.scaleRadius * sinA)
    val bigRadius = geometry.scaleRadius * 1.1f
    val bigCircleCenter = Offset(needleEnd.x - bigRadius * cosA, needleEnd.y - bigRadius * sinA)
    val innerStop = (bigRadius - geometry.tickLarge) / bigRadius
    val stops = geometry.trailAlphaStops.mapIndexed { i, a -> (innerStop + i * (1f - innerStop) / (geometry.trailAlphaStops.size - 1)) to color.copy(alpha = a) }.toTypedArray()
    val radialBrush = Brush.radialGradient(colorStops = stops, center = bigCircleCenter, radius = bigRadius)
    clipPath(trailCache.radiusClipPath) {
        val sector = Path().apply { arcTo(Rect(geometry.center.x - bigRadius, geometry.center.y - bigRadius, geometry.center.x + bigRadius, geometry.center.y + bigRadius), geometry.startAngle, sweepAngle, true); lineTo(geometry.center.x, geometry.center.y); close() }
        clipPath(sector) { drawCircle(brush = radialBrush, center = bigCircleCenter, radius = bigRadius - geometry.tickLarge / 2f, style = Stroke(width = geometry.tickLarge)) }
    }
    drawTrailBorder(geometry, sweepAngle, color)
}

/**
 * Получение цвета шлейфа в зависимости от скорости.
 */
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

/**
 * Отрисовка тонкой внешней границы шлейфа.
 */
private fun DrawScope.drawTrailBorder(geometry: DashboardType4Geometry, sweepAngle: Float, color: Color) {
    val borderRadius = geometry.scaleRadius + 0.5f * geometry.unit
    drawArc(brush = Brush.sweepGradient(colors = listOf(color.copy(alpha = 0f), color.copy(alpha = 1f)), center = geometry.center), startAngle = geometry.startAngle, sweepAngle = sweepAngle, useCenter = false, topLeft = Offset(geometry.center.x - borderRadius, geometry.center.y - borderRadius), size = Size(borderRadius * 2, borderRadius * 2), style = Stroke(width = 1f * geometry.unit))
}

/**
 * Отрисовка двойного радиального свечения вокруг шкалы.
 */
private fun DrawScope.drawDualRadialGlow(geometry: DashboardType4Geometry, speed: Float, baseColor: Color, peakSpread: Float = 0.01f) {
    val innerRadius = geometry.ringRadius - geometry.glowWidth
    val outerRadius = geometry.ringRadius + geometry.glowWidth
    if (innerRadius <= 0f) return
    val glowColor = if (speed <= 120f) baseColor else lerp(baseColor, Color.Red, ((speed - 120f) / 100f).coerceIn(0f, 1f))
    val brush = Brush.radialGradient(colorStops = arrayOf(innerRadius / outerRadius to Color.Transparent, (geometry.ringRadius / outerRadius - peakSpread).coerceIn(0f, 1f) to glowColor.copy(alpha = 0.6f), geometry.ringRadius / outerRadius to glowColor, (geometry.ringRadius / outerRadius + peakSpread).coerceIn(0f, 1f) to glowColor.copy(alpha = 0.6f), 1f to Color.Transparent), center = geometry.center, radius = outerRadius)
    drawCircle(brush = brush, center = geometry.center, radius = (outerRadius + innerRadius) / 2f, style = Stroke(width = outerRadius - innerRadius))
}

/**
 * Расчёт углов для анимации шкалы вольтметра.
 */
private fun calculateAngles(voltage: Float, currentRingRotation: Float): VoltageAngles {
    val degreesPerVolt = abs(BASE_SWEEP_ANGLE) / (BASE_MAX_VOLTAGE - BASE_MIN_VOLTAGE)
    val voltageAngleAbsolute = BASE_START_ANGLE - (voltage - BASE_MIN_VOLTAGE) * degreesPerVolt
    return if (currentRingRotation != 0f) {
        val limiter = if (currentRingRotation > 0f) (BASE_START_ANGLE + BASE_SWEEP_ANGLE) else BASE_START_ANGLE
        var newRotation = (currentRingRotation - (voltageAngleAbsolute + currentRingRotation - limiter)).coerceIn(-EXTENSION_DEGREES, EXTENSION_DEGREES)
        if (abs(newRotation) < 0.0001f) newRotation = 0f
        VoltageAngles(limiter, newRotation)
    } else {
        val clamped = voltageAngleAbsolute.coerceIn(BASE_START_ANGLE + BASE_SWEEP_ANGLE, BASE_START_ANGLE)
        var newRotation = (clamped - voltageAngleAbsolute).coerceIn(-EXTENSION_DEGREES, EXTENSION_DEGREES)
        if (abs(newRotation) < 0.0001f) newRotation = 0f
        VoltageAngles(clamped, newRotation)
    }
}

/**
 * Создание битмапа шкалы вольтметра.
 */
private fun buildVoltageRingBitmap(geometry: DashboardType4Geometry): ImageBitmap {
    val bitmap = ImageBitmap(geometry.width.toInt(), geometry.height.toInt())
    val drawScope = CanvasDrawScope()
    drawScope.draw(geometry.density, LayoutDirection.Ltr, Canvas(bitmap), Size(geometry.width, geometry.height)) {
        val ringOuterRadius = geometry.scaleRadius - geometry.tickLarge / 2f
        rotate(270f) { drawArc(brush = buildSweepBrush(), startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = Offset(geometry.center.x - ringOuterRadius, geometry.center.y - ringOuterRadius), size = Size(ringOuterRadius * 2, ringOuterRadius * 2), style = Stroke(width = geometry.tickLarge)) }
        val radialBrush = Brush.radialGradient(colorStops = arrayOf(0f to Color.Transparent, (ringOuterRadius - geometry.tickLarge / 2f) / (ringOuterRadius + geometry.tickLarge / 2f) to Color.Transparent, (ringOuterRadius - geometry.tickLarge / 2f + geometry.tickLarge * 0.65f) / (ringOuterRadius + geometry.tickLarge / 2f) to Color.Black, 1f to Color.Black), center = geometry.center, radius = ringOuterRadius + geometry.tickLarge / 2f)
        drawArc(brush = radialBrush, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = Offset(geometry.center.x - ringOuterRadius, geometry.center.y - ringOuterRadius), size = Size(ringOuterRadius * 2, ringOuterRadius * 2), style = Stroke(width = geometry.tickLarge), blendMode = BlendMode.DstIn)
    }
    return bitmap
}

/**
 * Создание градиентной кисти для шкалы вольтметра.
 */
private fun buildSweepBrush(): Brush {
    val ext = (BASE_MAX_VOLTAGE - BASE_MIN_VOLTAGE) * (EXTENSION_DEGREES / abs(BASE_SWEEP_ANGLE))
    val min = BASE_MIN_VOLTAGE - ext; val max = BASE_MAX_VOLTAGE + ext
    fun n(v: Float) = 1f - ((v - min) / (max - min)).coerceIn(0f, 1f)
    return Brush.sweepGradient(colorStops = arrayOf(0f to DARK_BLUE, n(13.1f) to DARK_BLUE, n(13f) to DARK_GREEN, n(12.75f) to DARK_GREEN, n(12.5f) to Color.Green, n(12.2f) to Color.Yellow, n(12f) to Color.Red, n(11.8f) to BURGUNDY, 1f to BURGUNDY))
}

/**
 * Создание битмапа основной шкалы спидометра.
 */
private fun buildScaleBitmap(geometry: DashboardType4Geometry, ringColor: Color): ImageBitmap {
    val bitmap = ImageBitmap(geometry.width.toInt(), geometry.height.toInt())
    val drawScope = CanvasDrawScope()
    drawScope.draw(geometry.density, LayoutDirection.Ltr, Canvas(bitmap), Size(geometry.width, geometry.height)) {
        drawCircle(Color.White, geometry.outerRingRadius, geometry.center, style = Stroke(geometry.outerStrokeWidth))
        val textPaint = android.graphics.Paint().apply { color = ringColor.toArgb(); textAlign = android.graphics.Paint.Align.CENTER; textSize = geometry.textSizePx; isAntiAlias = true }
        for (i in 0..geometry.maxSpeed.toInt() step 2) {
            val angle = geometry.startAngle + geometry.fullSweep * (i / geometry.maxSpeed)
            val rad = Math.toRadians(angle.toDouble()).toFloat()
            val cos = cos(rad); val sin = sin(rad)
            val is20 = i % 20 == 0; val tickLen = if (is20) geometry.tickLarge else if (i % 10 == 0) geometry.tickMedium else geometry.tickSmall
            drawLine(ringColor, Offset(geometry.center.x + cos * geometry.scaleRadius, geometry.center.y + sin * geometry.scaleRadius), Offset(geometry.center.x + cos * (geometry.scaleRadius - tickLen), geometry.center.y + sin * (geometry.scaleRadius - tickLen)), if (is20) 2f * geometry.unit else if (i % 10 == 0) 1.5f * geometry.unit else 1f * geometry.unit, StrokeCap.Round)
            if (is20) drawContext.canvas.nativeCanvas.apply { save(); translate(geometry.center.x + cos * geometry.textRadius, geometry.center.y + sin * geometry.textRadius); rotate(angle + 90f); drawText(i.toString(), 0f, -(textPaint.fontMetrics.ascent + textPaint.fontMetrics.descent) / 2f, textPaint); restore() }
        }
    }
    return bitmap
}

/**
 * Создание битмапа стрелки спидометра.
 */
private fun buildNeedleBitmap(geometry: DashboardType4Geometry, ringColor: Color): ImageBitmap {
    val bitmap = ImageBitmap(geometry.width.toInt(), geometry.height.toInt())
    val drawScope = CanvasDrawScope()
    drawScope.draw(geometry.density, LayoutDirection.Ltr, Canvas(bitmap), Size(geometry.width, geometry.height)) {
        drawCircle(Color.Black, geometry.blackRadius, geometry.center, style = Stroke(geometry.blackStrokeWidth))
        drawHeatBloomSegment(radius = geometry.blackRadius, color = ringColor, geometry = geometry, sweepAngle = 45f, startAngle = 0f)
        val tip = Offset(geometry.center.x + geometry.scaleRadius - 1f * geometry.unit, geometry.center.y)
        val start = Offset(geometry.center.x + (geometry.ringRadius - geometry.glowWidth), geometry.center.y)
        drawLine(Brush.linearGradient(listOf(ringColor.copy(alpha = 0f), ringColor.copy(alpha = 0.5f)), start, tip), start, tip, 5f * geometry.unit, StrokeCap.Round)
        drawLine(Brush.linearGradient(listOf(Color.Transparent, Color.White), start, tip), start, tip, 1.5f * geometry.unit, StrokeCap.Round)
    }
    return bitmap
}

/**
 * Отрисовка "эффекта раскаленного сегмента" у основания стрелки.
 */
private fun DrawScope.drawHeatBloomSegment(radius: Float, color: Color, geometry: DashboardType4Geometry, sweepAngle: Float = 45f, gradientWidth: Float = 7f, startAngle: Float = 270f) {
    val scaledGradientWidth = gradientWidth * geometry.unit
    val outerRadius = radius + scaledGradientWidth
    val canvas = drawContext.canvas
    canvas.saveLayer(Rect(Offset.Zero, size), Paint())
    drawCircle(brush = Brush.radialGradient(colorStops = arrayOf(0f to Color.Transparent, (radius - scaledGradientWidth) / outerRadius to Color.Transparent, radius / outerRadius to color, 1f to Color.Transparent), center = geometry.center, radius = outerRadius), radius = outerRadius, center = geometry.center)
    drawCircle(Color.White, radius, geometry.center, style = Stroke(1f * geometry.unit))
    drawIntoCanvas {
        it.nativeCanvas.save(); it.nativeCanvas.rotate(startAngle - 180f, geometry.center.x, geometry.center.y)
        val half = (sweepAngle / 360f) / 2f
        drawCircle(brush = Brush.sweepGradient(colorStops = arrayOf(0f to Color.Transparent, 0.5f - half to Color.Transparent, 0.5f to Color.Black, 0.5f + half to Color.Transparent, 1f to Color.Transparent), center = geometry.center), radius = outerRadius, center = geometry.center, blendMode = BlendMode.DstIn)
        it.nativeCanvas.restore()
    }
    canvas.restore()
}
