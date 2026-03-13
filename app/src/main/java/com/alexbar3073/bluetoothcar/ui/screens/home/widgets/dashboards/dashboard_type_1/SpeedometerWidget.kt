package com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_1

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
import androidx.compose.ui.graphics.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alexbar3073.bluetoothcar.R
import com.alexbar3073.bluetoothcar.data.models.CarData
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Виджет спидометра с интегрированным вольтметром для дашборда Type 1.
 * Выполняет однократную стартовую анимацию (развертка приборов) при первом запуске.
 *
 * ОПТИМИЗАЦИИ:
 *   1. Мутация состояния вынесена из Canvas (ringRotation обновляется в LaunchedEffect)
 *   2. Bitmap'ы привязаны к размеру Canvas (пересоздаются только при изменении размера)
 *   3. Paint объекты кэшируются (не создаются каждый кадр)
 *   4. Кэширование Path для хвоста и окна вольтметра
 *   5. Вынос размера текста из цикла рисования
 *   6. Трафарет (clipPath) для вольтметра вместо закрашивания фоном
 *
 * @param modifier Модификатор для настройки размера и позиции
 * @param carData Данные автомобиля
 * @param geometry Геометрические параметры дашборда (содержит размеры, цвет и лог-тег)
 */
@Composable
internal fun SpeedometerWidget(
    modifier: Modifier = Modifier,
    carData: CarData,
    geometry: Geometry
) {
    // ========== КОНФИГУРАЦИОННЫЕ ПАРАМЕТРЫ ==========
    val maxSpeed = 220f
    val maxVoltage = 16f
    val startupRiseDuration = 1300
    val startupFallDuration = 5000
    val transitionDuration = 1500

    // ========== УПРАВЛЕНИЕ СТАРТОВОЙ АНИМАЦИЕЙ ==========
    var startupFinished by rememberSaveable { mutableStateOf(false) }
    val startupAnim = remember { Animatable(0f) }
    val transitionAnim = remember { Animatable(0f) }

    LaunchedEffect(startupFinished) {
        if (!startupFinished) {
            startupAnim.animateTo(
                maxSpeed,
                animationSpec = tween(startupRiseDuration, easing = FastOutSlowInEasing)
            )
            startupAnim.animateTo(
                0f,
                animationSpec = tween(startupFallDuration, easing = LinearOutSlowInEasing)
            )
            transitionAnim.animateTo(
                1f,
                animationSpec = tween(transitionDuration, easing = FastOutSlowInEasing)
            )
            startupFinished = true
        }
    }

    // ========== ПОДГОТОВКА ДАННЫХ ==========
    val displaySpeed = if (!startupFinished) {
        if (transitionAnim.value > 0f) {
            val targetSpeed = max(0f, carData.speed)
            targetSpeed * transitionAnim.value
        } else {
            startupAnim.value
        }
    } else {
        carData.speed
    }

    val displayVoltage = if (!startupFinished) {
        if (transitionAnim.value > 0f) {
            val targetVoltage = max(0f, carData.voltage)
            targetVoltage * transitionAnim.value
        } else {
            startupAnim.value / maxSpeed * maxVoltage
        }
    } else {
        carData.voltage
    }

    val animatedSpeed by animateFloatAsState(
        targetValue = displaySpeed.coerceIn(0f, maxSpeed),
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 90f),
        label = "needle"
    )

    val animatedVoltage by animateFloatAsState(
        targetValue = displayVoltage.coerceIn(0f, maxVoltage),
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 90f),
        label = "voltage"
    )

    // ========== ЛОГИКА ВОЛЬТМЕТРА ==========
    val isLowVoltage = animatedVoltage <= 11.8f
    val isCharging = animatedVoltage > 13.0f

    val alarmIcon = painterResource(R.drawable.battery_full_48)
    val normalIcon = painterResource(R.drawable.battery_50)
    val chargingIcon = painterResource(R.drawable.battery_charging_50)
    val currentIcon = if (isCharging) {
        chargingIcon
    } else {
        if (isLowVoltage) alarmIcon else normalIcon
    }

    val blinkAlpha by animateFloatAsState(
        targetValue = if (isLowVoltage) 0.3f else 1f,
        animationSpec = if (isLowVoltage) {
            infiniteRepeatable(animation = tween(500))
        } else {
            tween(0)
        },
        label = "blink_anim"
    )

    // ========== КЭШИРОВАНИЕ PAINT ==========
    val speedPaint = remember {
        android.graphics.Paint().apply {
            color = Color.White.toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
    }

    val unitPaint = remember {
        android.graphics.Paint().apply {
            color = Color.Gray.toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    // ========== РАСЧЕТ УГЛОВ ВОЛЬТМЕТРА ==========
    var ringRotation by remember { mutableFloatStateOf(0f) }

    val anglesState = remember(animatedVoltage, ringRotation) {
        derivedStateOf { calculateAngles(animatedVoltage, ringRotation) }
    }
    val angles by anglesState

    LaunchedEffect(angles.ringRotation) {
        if (angles.ringRotation != ringRotation) {
            ringRotation = angles.ringRotation
        }
    }

    // ========== КЭШИРОВАНИЕ СПЕЦИФИЧНЫХ ДЛЯ ВИДЖЕТА ДАННЫХ =====
    // ИСПРАВЛЕНИЕ: ключ для битмапов — привязка к размеру Canvas
    val bitmapKey = remember(geometry.width, geometry.height) {
        Pair(geometry.width, geometry.height)
    }

    var scaleBitmap by remember(bitmapKey) { mutableStateOf<ImageBitmap?>(null) }
    var needleBitmap by remember(bitmapKey) { mutableStateOf<ImageBitmap?>(null) }
    var ringBitmap by remember(bitmapKey) { mutableStateOf<ImageBitmap?>(null) }

    // ИСПРАВЛЕНИЕ: кэш для хвоста (Path и innerRadius)
    val trailCache = rememberTrailCache(geometry)

    // ИСПРАВЛЕНИЕ: кэш для окна вольтметра
    val windowPath = rememberVoltmeterWindow(geometry)

    // ИСПРАВЛЕНИЕ: размеры текста (вычисляются один раз)
    val speedTextSize = with(geometry.density) { 42.sp.toPx() }
    val unitTextSize = with(geometry.density) { 16.sp.toPx() }

    // ========== ОСНОВНАЯ ОТРИСОВКА ==========
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawVoltmeter(
                geometry = geometry,
                voltage = animatedVoltage,
                ringBitmap = ringBitmap ?: buildVoltageRingBitmap(geometry).also {
                    ringBitmap = it
                },
                ringRotation = ringRotation,
                iconPainter = currentIcon,
                alpha = if (isLowVoltage) blinkAlpha else 1f,
                needleAngle = angles.needleAngle,
                windowPath = windowPath
            )

            drawSpeedometer(
                geometry = geometry,
                speed = animatedSpeed,
                scaleBitmap = scaleBitmap ?: buildScaleBitmap(geometry, geometry.ringColor).also {
                    scaleBitmap = it
                },
                needleBitmap = needleBitmap ?: buildNeedleBitmap(
                    geometry,
                    geometry.ringColor
                ).also {
                    needleBitmap = it
                },
                ringColor = geometry.ringColor,
                speedPaint = speedPaint,
                unitPaint = unitPaint,
                trailCache = trailCache,
                speedTextSize = speedTextSize,
                unitTextSize = unitTextSize
            )
        }
    }
}

/**
 * Отрисовка спидометра.
 *
 * ОПТИМИЗАЦИЯ: добавлены параметры trailCache, speedTextSize, unitTextSize
 *
 * @param geometry Геометрические параметры
 * @param speed Текущая скорость для отображения
 * @param scaleBitmap Битмап со статической шкалой
 * @param needleBitmap Битмап со стрелкой
 * @param ringColor Цвет основных элементов (из геометрии)
 * @param speedPaint Кэшированный Paint для отрисовки числового значения скорости
 * @param unitPaint Кэшированный Paint для отрисовки единиц измерения
 * @param trailCache Кэш с предвычисленными данными хвоста
 * @param speedTextSize Заранее вычисленный размер шрифта для скорости
 * @param unitTextSize Заранее вычисленный размер шрифта для единиц измерения
 */
private fun DrawScope.drawSpeedometer(
    geometry: Geometry,
    speed: Float,
    scaleBitmap: ImageBitmap,
    needleBitmap: ImageBitmap,
    ringColor: Color,
    speedPaint: android.graphics.Paint,
    unitPaint: android.graphics.Paint,
    trailCache: TrailCache,
    speedTextSize: Float,
    unitTextSize: Float
) {
    val needleAngle = geometry.startAngle + geometry.fullSweep * (speed / geometry.maxSpeed)

    drawImage(scaleBitmap, Offset.Zero)
    drawDualRadialGlow(geometry, speed, ringColor)

    rotate(needleAngle, pivot = geometry.center) {
        drawImage(needleBitmap, Offset.Zero)
    }

    drawSpeedText(
        speed = speed,
        geometry = geometry,
        speedPaint = speedPaint,
        unitPaint = unitPaint,
        speedTextSize = speedTextSize,
        unitTextSize = unitTextSize
    )

    drawSpeedometerTrail(
        geometry = geometry,
        speed = speed,
        trailCache = trailCache
    )
}

/**
 * Data class для кэширования неизменяемых данных хвоста
 */
private data class TrailCache(
    val innerRadius: Float,
    val radiusClipPath: Path
)

/**
 * Создает и кэширует данные для хвоста спидометра.
 * Вызывается только при изменении геометрии.
 */
@Composable
private fun rememberTrailCache(
    geometry: Geometry
): TrailCache {
    return remember(geometry.width, geometry.height) {
        val outerRadius = geometry.scaleRadius
        val tickLength = geometry.tickLarge
        val innerRadius = outerRadius - tickLength

        val radiusClipPath = Path().apply {
            addOval(
                Rect(
                    left = geometry.center.x - outerRadius,
                    top = geometry.center.y - outerRadius,
                    right = geometry.center.x + outerRadius,
                    bottom = geometry.center.y + outerRadius
                )
            )
        }

        TrailCache(innerRadius, radiusClipPath)
    }
}

/**
 * Создает и кэширует путь для окна вольтметра.
 * Вызывается только при изменении геометрии.
 */
@Composable
private fun rememberVoltmeterWindow(
    geometry: Geometry
): Path {
    return remember(geometry.width, geometry.height) {
        val radius = geometry.scaleRadius

        Path().apply {
            arcTo(
                rect = Rect(
                    left = geometry.center.x - radius,
                    top = geometry.center.y - radius,
                    right = geometry.center.x + radius,
                    bottom = geometry.center.y + radius
                ),
                startAngleDegrees = 40f,
                sweepAngleDegrees = 100f,
                forceMoveTo = true
            )
            lineTo(geometry.center.x, geometry.center.y)
            close()
        }
    }
}

/**
 * Отрисовка хвоста спидометра с градиентом и тонкой дугой
 *
 * @param geometry Геометрические параметры
 * @param speed Текущая скорость
 * @param trailCache Кэш с предвычисленными неизменяемыми данными
 */
private fun DrawScope.drawSpeedometerTrail(
    geometry: Geometry,
    speed: Float,
    trailCache: TrailCache
) {
    val sweepAngle = geometry.fullSweep * (speed / geometry.maxSpeed)
    if (sweepAngle <= 0f) return

    val color = getTrailColor(speed)

    val currentAngle = geometry.startAngle + sweepAngle
    val angleRad = currentAngle * PI.toFloat() / 180f
    val cosA = cos(angleRad)
    val sinA = sin(angleRad)

    val needleEnd = Offset(
        x = geometry.center.x + geometry.scaleRadius * cosA,
        y = geometry.center.y + geometry.scaleRadius * sinA
    )

    val bigRadius = geometry.scaleRadius * 1.1f
    val bigInnerRadius = bigRadius - geometry.tickLarge

    val bigCircleCenter = Offset(
        x = needleEnd.x - bigRadius * cosA,
        y = needleEnd.y - bigRadius * sinA
    )

    // ИСПРАВЛЕНИЕ: используем trailAlphaStops из geometry
    val radialStops = geometry.trailAlphaStops

    val innerStop = bigInnerRadius / bigRadius
    val colorStops = radialStops.mapIndexed { i, alpha ->
        val position = innerStop + i * (1f - innerStop) / (radialStops.size - 1)
        position to color.copy(alpha = alpha)
    }.toTypedArray()

    val radialBrush = Brush.radialGradient(
        colorStops = colorStops,
        center = bigCircleCenter,
        radius = bigRadius
    )

    // ИСПРАВЛЕНИЕ: используем кэшированный Path
    clipPath(trailCache.radiusClipPath) {
        val sectorClipPath = Path().apply {
            arcTo(
                rect = Rect(
                    left = geometry.center.x - bigRadius,
                    top = geometry.center.y - bigRadius,
                    right = geometry.center.x + bigRadius,
                    bottom = geometry.center.y + bigRadius
                ),
                startAngleDegrees = geometry.startAngle,
                sweepAngleDegrees = sweepAngle,
                forceMoveTo = true
            )
            lineTo(geometry.center.x, geometry.center.y)
            close()
        }

        clipPath(sectorClipPath) {
            drawCircle(
                brush = radialBrush,
                center = bigCircleCenter,
                radius = bigRadius - geometry.tickLarge / 2f,
                style = Stroke(width = geometry.tickLarge)
            )
        }
    }

    drawTrailBorder(geometry, sweepAngle, color)
}

/**
 * Определяет цвет хвоста по скорости
 */
private fun getTrailColor(speed: Float): Color {
    val white = Color.White
    val green = Color.Green
    val yellow = Color.Yellow
    val orange = Color(0xFFFFA500)
    val red = Color.Red
    val burgundy = Color(0xFF800000)

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
 * Рисует тонкую внешнюю дугу хвоста
 */
private fun DrawScope.drawTrailBorder(
    geometry: Geometry,
    sweepAngle: Float,
    color: Color
) {
    val borderWidth = 1.dp.toPx()
    val borderRadius = geometry.scaleRadius + borderWidth / 2f
    val borderRect = Rect(
        left = geometry.center.x - borderRadius,
        top = geometry.center.y - borderRadius,
        right = geometry.center.x + borderRadius,
        bottom = geometry.center.y + borderRadius
    )

    drawArc(
        brush = Brush.sweepGradient(
            colors = listOf(color.copy(alpha = 0f), color.copy(alpha = 1f)),
            center = geometry.center
        ),
        startAngle = geometry.startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = borderRect.topLeft,
        size = borderRect.size,
        style = Stroke(width = borderWidth, cap = StrokeCap.Butt)
    )
}

/**
 * Рисует текст текущей скорости в центре спидометра.
 *
 * ОПТИМИЗАЦИЯ: размер текста передается как параметр (вычислен заранее)
 *
 * @param speed Текущая скорость
 * @param geometry Геометрические параметры
 * @param speedPaint Кэшированный Paint
 * @param unitPaint Кэшированный Paint
 * @param speedTextSize Заранее вычисленный размер шрифта для скорости
 * @param unitTextSize Заранее вычисленный размер шрифта для единиц измерения
 */
private fun DrawScope.drawSpeedText(
    speed: Float,
    geometry: Geometry,
    speedPaint: android.graphics.Paint,
    unitPaint: android.graphics.Paint,
    speedTextSize: Float,
    unitTextSize: Float
) {
    speedPaint.textSize = speedTextSize
    unitPaint.textSize = unitTextSize

    drawContext.canvas.nativeCanvas.apply {
        val speedStr = speed.toInt().toString()
        val fm = speedPaint.fontMetrics
        val baseline = geometry.center.y - (fm.ascent + fm.descent) / 2f

        drawText(speedStr, geometry.center.x, baseline, speedPaint)
        drawText("км/ч", geometry.center.x, baseline + 32f * geometry.unit, unitPaint)
    }
}

/**
 * Рисует двойное радиальное свечение.
 */
private fun DrawScope.drawDualRadialGlow(
    geometry: Geometry,
    speed: Float,
    baseColor: Color,
    peakSpread: Float = 0.01f
) {
    val innerRadius = geometry.ringRadius - geometry.glowWidth
    val outerRadius = geometry.ringRadius + geometry.glowWidth
    if (innerRadius <= 0f) return

    val glowColor = if (speed <= 120f) {
        baseColor
    } else {
        val t = ((speed - 120f) / 100f).coerceIn(0f, 1f)
        lerp(baseColor, Color(0xFFFF0000), t)
    }

    val totalWidth = outerRadius - innerRadius
    val strokeCenter = (outerRadius + innerRadius) / 2f

    val innerStop = innerRadius / outerRadius
    val peakStop = geometry.ringRadius / outerRadius

    val leftPeak = (peakStop - peakSpread).coerceIn(0f, 1f)
    val rightPeak = (peakStop + peakSpread).coerceIn(0f, 1f)

    val brush = Brush.radialGradient(
        colorStops = arrayOf(
            innerStop to Color.Transparent,
            leftPeak to glowColor.copy(alpha = 0.6f),
            peakStop to glowColor,
            rightPeak to glowColor.copy(alpha = 0.6f),
            1f to Color.Transparent
        ),
        center = geometry.center,
        radius = outerRadius
    )

    drawCircle(
        brush = brush,
        center = geometry.center,
        radius = strokeCenter,
        style = Stroke(width = totalWidth)
    )
}

// ==============================
// ВОЛЬТМЕТР
// ==============================

// Константы вольтметра
private const val BASE_MIN_VOLTAGE = 12.0f
private const val BASE_MAX_VOLTAGE = 12.7f
private const val BASE_START_ANGLE = 140f
private const val BASE_SWEEP_ANGLE = -100f
private const val EXTENSION_DEGREES = 130f

private val BURGUNDY = Color(0xFF800000)
private val DARK_GREEN = Color(0xFF1B5E20)
private val DARK_BLUE = Color(0xFF0D47A1)

/**
 * Данные углов для отрисовки вольтметра.
 */
private data class VoltageAngles(
    val needleAngle: Float,
    val ringRotation: Float
)

/**
 * Отрисовка вольтметра.
 *
 * АРХИТЕКТУРА (ИСПРАВЛЕНИЕ):
 *   1. Используем трафарет (windowPath) в форме окна (нижний сегмент)
 *   2. Применяем clipPath — кольцо видно только в окне
 *   3. Поверх рисуем иконку и стрелку (без обрезки)
 *   4. НЕТ закрашивания фоном → нет черного ореола
 *
 * @param geometry Геометрические параметры
 * @param voltage Текущее напряжение
 * @param ringBitmap Битмап с цветным кольцом (полный круг)
 * @param ringRotation Угол поворота кольца
 * @param iconPainter Иконка батареи
 * @param alpha Прозрачность иконки (для мигания)
 * @param needleAngle Угол стрелки вольтметра
 * @param windowPath Кэшированный путь для трафарета окна
 */
private fun DrawScope.drawVoltmeter(
    geometry: Geometry,
    voltage: Float,
    ringBitmap: ImageBitmap,
    ringRotation: Float,
    iconPainter: Painter,
    alpha: Float,
    needleAngle: Float,
    windowPath: Path
) {
    clipPath(windowPath) {
        rotate(ringRotation, pivot = geometry.center) {
            drawImage(
                image = ringBitmap,
                topLeft = Offset(
                    geometry.center.x - ringBitmap.width / 2f,
                    geometry.center.y - ringBitmap.height / 2f
                )
            )
        }
    }

    drawBatteryIconAndText(
        geometry = geometry,
        voltage = voltage,
        iconPainter = iconPainter,
        alpha = alpha
    )

    drawVoltageNeedle(
        geometry = geometry,
        needleAngle = needleAngle,
        ringColor = voltageToColor(voltage)
    )
}

/**
 * Расчет углов для вольтметра.
 */
private fun calculateAngles(
    voltage: Float,
    currentRingRotation: Float
): VoltageAngles {
    val baseMinAngle = BASE_START_ANGLE
    val baseMaxAngle = BASE_START_ANGLE + BASE_SWEEP_ANGLE
    val degreesPerVolt = abs(BASE_SWEEP_ANGLE) / (BASE_MAX_VOLTAGE - BASE_MIN_VOLTAGE)

    val voltageAngleAbsolute = BASE_START_ANGLE - (voltage - BASE_MIN_VOLTAGE) * degreesPerVolt

    return if (currentRingRotation != 0f) {
        val limiter = if (currentRingRotation > 0f) baseMaxAngle else baseMinAngle
        val desired = voltageAngleAbsolute + currentRingRotation
        val overflow = desired - limiter
        var newRotation = currentRingRotation - overflow

        if (currentRingRotation > 0f && newRotation < 0f) newRotation = 0f
        if (currentRingRotation < 0f && newRotation > 0f) newRotation = 0f

        newRotation = newRotation.coerceIn(-EXTENSION_DEGREES, EXTENSION_DEGREES)
        if (abs(newRotation) < 0.0001f) newRotation = 0f

        VoltageAngles(limiter, newRotation)
    } else {
        val clampedAngle = voltageAngleAbsolute.coerceIn(baseMaxAngle, baseMinAngle)
        val overflow = voltageAngleAbsolute - clampedAngle
        var newRotation = -overflow

        newRotation = newRotation.coerceIn(-EXTENSION_DEGREES, EXTENSION_DEGREES)
        if (abs(newRotation) < 0.0001f) newRotation = 0f

        VoltageAngles(clampedAngle, newRotation)
    }
}

/**
 * Рисует стрелку вольтметра.
 */
private fun DrawScope.drawVoltageNeedle(
    geometry: Geometry,
    needleAngle: Float,
    ringColor: Color
) {
    val angleRad = Math.toRadians(needleAngle.toDouble()).toFloat()
    val cos = cos(angleRad)
    val sin = sin(angleRad)

    val start = Offset(
        geometry.center.x + cos * geometry.textRadius,
        geometry.center.y + sin * geometry.textRadius
    )

    val tip = Offset(
        geometry.center.x + cos * (geometry.scaleRadius - 2f),
        geometry.center.y + sin * (geometry.scaleRadius - 2f)
    )

    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(
                ringColor.copy(alpha = 0f),
                ringColor.copy(alpha = 0.5f)
            ),
            start = start,
            end = tip
        ),
        start = start,
        end = tip,
        strokeWidth = 5f * geometry.unit,
        cap = StrokeCap.Round
    )

    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                Color.White
            ),
            start = start,
            end = tip
        ),
        start = start,
        end = tip,
        strokeWidth = 1.5f * geometry.unit,
        cap = StrokeCap.Round
    )
}

/**
 * Создает кисть с круговым градиентом для цветного кольца вольтметра.
 */
private fun buildSweepBrush(): Brush {
    val (extendedMin, extendedMax) = getExtendedVoltageRange()

    fun normalizeInverted(v: Float): Float =
        1.0f - ((v - extendedMin) / (extendedMax - extendedMin)).coerceIn(0f, 1f)

    val stops = arrayOf(
        0.00f to DARK_BLUE,
        normalizeInverted(13.1f) to DARK_BLUE,
        normalizeInverted(13.0f) to DARK_GREEN,
        normalizeInverted(12.75f) to DARK_GREEN,
        normalizeInverted(12.5f) to Color.Green,
        normalizeInverted(12.2f) to Color.Yellow,
        normalizeInverted(12.0f) to Color.Red,
        normalizeInverted(11.8f) to BURGUNDY,
        1.00f to BURGUNDY
    )

    return Brush.sweepGradient(colorStops = stops)
}

/**
 * Определяет цвет иконки батареи по напряжению.
 */
private fun voltageToColor(v: Float): Color = when {
    v <= 11.8f -> Color.Red
    v <= 12.0f -> lerp(BURGUNDY, Color.Red, ((v - 11.8f) / 0.2f).coerceIn(0f, 1f))
    v <= 12.2f -> lerp(Color.Red, Color.Yellow, ((v - 12.0f) / 0.2f).coerceIn(0f, 1f))
    v <= 12.5f -> lerp(Color.Yellow, Color.Green, ((v - 12.2f) / 0.3f).coerceIn(0f, 1f))
    v <= 12.75f -> lerp(Color.Green, DARK_GREEN, ((v - 12.5f) / 0.25f).coerceIn(0f, 1f))
    v <= 13.1f -> lerp(DARK_GREEN, DARK_BLUE, ((v - 12.75f) / 0.35f).coerceIn(0f, 1f))
    else -> Color(0xFF3378D8)
}

/**
 * Рассчитывает расширенный диапазон напряжений.
 */
private fun getExtendedVoltageRange(): Pair<Float, Float> {
    val extendedRange = (BASE_MAX_VOLTAGE - BASE_MIN_VOLTAGE) *
            (EXTENSION_DEGREES / abs(BASE_SWEEP_ANGLE))
    return (BASE_MIN_VOLTAGE - extendedRange) to (BASE_MAX_VOLTAGE + extendedRange)
}

/**
 * Создает bitmap с цветным кольцом вольтметра.
 *
 * ИЗМЕНЕНИЯ:
 *   - Удалена маска с BlendMode.DstIn (больше не нужна)
 *   - Удален backgroundColor (не используется)
 *
 * @param geometry Геометрические параметры
 */
private fun buildVoltageRingBitmap(
    geometry: Geometry
): ImageBitmap {
    val bitmap = ImageBitmap(
        width = geometry.width.toInt(),
        height = geometry.height.toInt()
    )

    val composeCanvas = Canvas(bitmap)
    val drawScope = CanvasDrawScope()
    val sweepBrush = buildSweepBrush()

    drawScope.draw(
        density = geometry.density,
        layoutDirection = LayoutDirection.Ltr,
        canvas = composeCanvas,
        size = Size(geometry.width, geometry.height)
    ) {
        val bitmapCenter = geometry.center
        val ringWidth = geometry.tickLarge
        val ringOuterRadius = geometry.scaleRadius - ringWidth / 2f

        // 1. Цветное кольцо (круговой градиент)
        rotate(270f) {
            drawArc(
                brush = sweepBrush,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(
                    bitmapCenter.x - ringOuterRadius,
                    bitmapCenter.y - ringOuterRadius
                ),
                size = Size(ringOuterRadius * 2, ringOuterRadius * 2),
                style = Stroke(width = ringWidth)
            )
        }

        // 2. Радиальный градиент для глубины (ВОССТАНОВЛЕН)
        val innerRadius = ringOuterRadius - ringWidth / 2f
        val outerRadius = ringOuterRadius + ringWidth / 2f
        val thickness = outerRadius - innerRadius
        val dissolveDepth = thickness * 0.65f
        val fadeEnd = innerRadius + dissolveDepth

        val fadeStartRatio = innerRadius / outerRadius
        val fadeEndRatio = fadeEnd / outerRadius

        val radialBrush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                fadeStartRatio to Color.Transparent,
                fadeEndRatio to Color.Black,
                1f to Color.Black
            ),
            center = bitmapCenter,
            radius = outerRadius
        )

        drawArc(
            brush = radialBrush,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(bitmapCenter.x - ringOuterRadius, bitmapCenter.y - ringOuterRadius),
            size = Size(ringOuterRadius * 2, ringOuterRadius * 2),
            style = Stroke(width = ringWidth),
            blendMode = BlendMode.DstIn
        )

        // 3. Блик
        rotate(270f) {
            drawArc(
                color = Color.White.copy(alpha = 0.02f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(
                    bitmapCenter.x - ringOuterRadius,
                    bitmapCenter.y - ringOuterRadius
                ),
                size = Size(ringOuterRadius * 2, ringOuterRadius * 2),
                style = Stroke(width = ringWidth),
                blendMode = BlendMode.Overlay
            )
        }
    }

    return bitmap
}

/**
 * Создает bitmap со статической шкалой спидометра.
 */
private fun buildScaleBitmap(
    geometry: Geometry,
    ringColor: Color
): ImageBitmap {
    val bitmap = ImageBitmap(
        width = geometry.width.toInt(),
        height = geometry.height.toInt()
    )

    val composeCanvas = Canvas(bitmap)
    val drawScope = CanvasDrawScope()

    drawScope.draw(
        density = geometry.density,
        layoutDirection = LayoutDirection.Ltr,
        canvas = composeCanvas,
        size = Size(geometry.width, geometry.height)
    ) {
        drawCircle(
            color = Color.White,
            radius = geometry.outerRingRadius,
            center = geometry.center,
            style = Stroke(width = geometry.outerStrokeWidth)
        )

        val textPaint = android.graphics.Paint().apply {
            color = ringColor.toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = geometry.textSizePx
            isAntiAlias = true
        }
        val fontMetrics = textPaint.fontMetrics

        for (i in 0..geometry.maxSpeed.toInt() step 2) {
            val angle = geometry.startAngle + geometry.fullSweep * (i / geometry.maxSpeed)
            val rad = Math.toRadians(angle.toDouble()).toFloat()
            val cos = cos(rad)
            val sin = sin(rad)

            val is20 = i % 20 == 0
            val is10 = i % 10 == 0

            val tickLength = when {
                is20 -> geometry.tickLarge
                is10 -> geometry.tickMedium
                else -> geometry.tickSmall
            }

            val tickStroke = when {
                is20 -> 2f * geometry.unit
                is10 -> 1.5f * geometry.unit
                else -> 1f * geometry.unit
            }

            val start = Offset(
                geometry.center.x + cos * geometry.scaleRadius,
                geometry.center.y + sin * geometry.scaleRadius
            )
            val end = Offset(
                geometry.center.x + cos * (geometry.scaleRadius - tickLength),
                geometry.center.y + sin * (geometry.scaleRadius - tickLength)
            )

            drawLine(
                color = ringColor,
                start = start,
                end = end,
                strokeWidth = tickStroke,
                cap = StrokeCap.Round
            )

            if (is20) {
                val textX = geometry.center.x + cos * geometry.textRadius
                val textY = geometry.center.y + sin * geometry.textRadius

                drawContext.canvas.nativeCanvas.apply {
                    save()
                    translate(textX, textY)
                    rotate(angle + 90f)

                    val baselineShift = -(fontMetrics.ascent + fontMetrics.descent) / 2f
                    drawText(i.toString(), 0f, baselineShift, textPaint)
                    restore()
                }
            }
        }
    }

    return bitmap
}

/**
 * Создает bitmap со стрелкой спидометра.
 */
private fun buildNeedleBitmap(
    geometry: Geometry,
    ringColor: Color
): ImageBitmap {
    val bitmap = ImageBitmap(
        width = geometry.width.toInt(),
        height = geometry.height.toInt()
    )

    val composeCanvas = Canvas(bitmap)
    val drawScope = CanvasDrawScope()

    drawScope.draw(
        density = geometry.density,
        layoutDirection = LayoutDirection.Ltr,
        canvas = composeCanvas,
        size = Size(geometry.width, geometry.height)
    ) {
        drawCircle(
            color = Color.Black,
            radius = geometry.blackRadius,
            center = geometry.center,
            style = Stroke(width = geometry.blackStrokeWidth)
        )

        drawHeatBloomSegment(
            radius = geometry.blackRadius,
            color = ringColor,
            sweepAngle = 45f,
            startAngle = 0f
        )

        val needleLength = geometry.scaleRadius - 1f * geometry.unit
        val tip = Offset(geometry.center.x + needleLength, geometry.center.y)

        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(ringColor.copy(alpha = 0f), ringColor.copy(alpha = 0.5f)),
                start = geometry.center,
                end = tip
            ),
            start = geometry.center,
            end = tip,
            strokeWidth = 5f * geometry.unit,
            cap = StrokeCap.Round
        )

        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, Color.White),
                start = geometry.center,
                end = tip
            ),
            start = geometry.center,
            end = tip,
            strokeWidth = 1.5f * geometry.unit,
            cap = StrokeCap.Round
        )
    }

    return bitmap
}

/**
 * Рисует эффект теплового свечения под стрелкой.
 */
private fun DrawScope.drawHeatBloomSegment(
    radius: Float,
    color: Color,
    sweepAngle: Float = 45f,
    gradientWidth: Float = 7.dp.toPx(),
    startAngle: Float = 270f
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val outerRadius = radius + gradientWidth

    val innerStop = (radius - gradientWidth) / outerRadius
    val peakStop = radius / outerRadius
    val outerStop = (radius + gradientWidth) / outerRadius

    val canvas = drawContext.canvas
    val layerRect = Rect(Offset.Zero, size)

    canvas.saveLayer(layerRect, Paint())

    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                innerStop to Color.Transparent,
                peakStop to color,
                outerStop to Color.Transparent,
                1f to Color.Transparent
            ),
            center = center,
            radius = outerRadius
        ),
        radius = outerRadius,
        center = center
    )

    drawCircle(
        color = Color.White,
        radius = radius,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )

    drawIntoCanvas {
        it.save()
        it.rotate(startAngle - 180f, center.x, center.y)

        val sweepFraction = sweepAngle / 360f
        val half = sweepFraction / 2f

        drawCircle(
            brush = Brush.sweepGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    0.5f - half to Color.Transparent,
                    0.5f to Color.Black,
                    0.5f + half to Color.Transparent,
                    1f to Color.Transparent
                ),
                center = center
            ),
            radius = outerRadius,
            center = center,
            blendMode = BlendMode.DstIn
        )
        it.restore()
    }

    canvas.restore()
}

/**
 * Рисует иконку батареи и текст напряжения.
 */
private fun DrawScope.drawBatteryIconAndText(
    geometry: Geometry,
    voltage: Float,
    iconPainter: Painter,
    alpha: Float = 1f
) {
    val iconColor = voltageToColor(voltage)
    val iconSize = geometry.textSizePx

    val voltageText = String.format("%.1fV", voltage)

    val textPaint = android.graphics.Paint().apply {
        color = Color.Gray.toArgb()
        textSize = geometry.textSizePx * 0.8f
        isAntiAlias = true
    }
    val textWidth = textPaint.measureText(voltageText)

    val spacing = 4.dp.toPx()
    val totalWidth = iconSize + spacing + textWidth

    val radiusBlock = geometry.textRadius - 5f
    val angle = 90f
    val rad = Math.toRadians(angle.toDouble()).toFloat()
    val cos = cos(rad)
    val sin = sin(rad)

    val blockCenterX = geometry.center.x + cos * radiusBlock
    val blockCenterY = geometry.center.y + sin * radiusBlock

    val blockLeft = blockCenterX - totalWidth / 2f

    val iconX = blockLeft
    val iconY = blockCenterY - iconSize / 2f

    val textX = blockLeft + iconSize + spacing
    val textY = blockCenterY - (textPaint.ascent() + textPaint.descent()) / 2f

    translate(left = iconX, top = iconY) {
        with(iconPainter) {
            draw(
                size = Size(iconSize, iconSize),
                alpha = alpha,
                colorFilter = ColorFilter.tint(iconColor)
            )
        }
    }

    drawContext.canvas.nativeCanvas.apply {
        drawText(
            voltageText,
            textX,
            textY,
            textPaint
        )
    }
}