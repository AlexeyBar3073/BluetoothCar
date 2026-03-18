// Файл: ui/theme/Theme.kt
package com.alexbar3073.bluetoothcar.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * ФАЙЛ: ui/theme/Theme.kt
 * МЕСТОНАХОЖДЕНИЕ: ui/theme/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Определение темы приложения BluetoothCar. Содержит цветовые схемы для темной и светлой тем,
 * вспомогательные функции для работы с цветами и градиентами, а также настройки системного UI.
 */

/**
 * Компактная высота топ-бара для всех экранов приложения.
 * Обеспечивает единый стиль и экономию места.
 */
val COMPACT_TOP_BAR_HEIGHT: Dp = 48.dp // Вместо стандартных 64dp

// ===== Цвета для темной темы =====
private val DarkBlue80 = Color(0xFF00D4FF)
private val DarkBlueGrey80 = Color(0xFFA0A0C0)
private val DarkPink80 = Color(0xFFFF6B6B)

private val DarkBlue40 = Color(0xFF0066CC)
private val DarkBlueGrey40 = Color(0xFF707090)
private val DarkPink40 = Color(0xFFFFB0B0)

// ===== Цвета для светлой темы =====
private val LightBlue80 = Color(0xFF0066CC)
private val LightBlueGrey80 = Color(0xFF555577)
private val LightPink80 = Color(0xFFCC0000)

private val LightBlue40 = Color(0xFF00D4FF)
private val LightBlueGrey40 = Color(0xFFA0A0C0)
private val LightPink40 = Color(0xFFFF6B6B)

// ===== Кастомные цветовые схемы =====
private val DarkColorScheme = darkColorScheme(
    primary = DarkBlue80,
    secondary = Color(0xFF4ADE80),
    tertiary = Color(0xFFFFA500),
    background = Color(0xFF0A0A0F),
    surface = Color(0xFF1A1A24),
    surfaceVariant = Color(0xFF2A2A34),
    error = DarkPink80,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFFE0E0FF),
    onSurface = Color(0xFFE0E0FF),
    onSurfaceVariant = DarkBlueGrey80,
    onError = Color.Black,
    outline = Color(0xFF444455),
    outlineVariant = Color(0xFF333344),
    scrim = Color(0x99000000),
    surfaceTint = DarkBlue80,
    inverseOnSurface = Color(0xFF1A1A24),
    inverseSurface = Color(0xFFE0E0FF),
    inversePrimary = DarkBlue40
)

private val LightColorScheme = lightColorScheme(
    primary = LightBlue80,
    secondary = Color(0xFF005522),
    tertiary = Color(0xFF664400),
    background = Color(0xFFF5F5FF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEEEEFF),
    error = LightPink80,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1A1A24),
    onSurface = Color(0xFF1A1A24),
    onSurfaceVariant = LightBlueGrey80,
    onError = Color.White,
    outline = Color(0xFFCCCCDD),
    outlineVariant = Color(0xFFDDDDEE),
    scrim = Color(0x99000000),
    surfaceTint = LightBlue80,
    inverseOnSurface = Color(0xFFF5F5FF),
    inverseSurface = Color(0xFF1A1A24),
    inversePrimary = LightBlue40
)

// ===== Объект с кастомными цветами =====
object AppColors {
    val PrimaryBlue = DarkBlue80
    val PrimaryBlueDark = DarkBlue40
    val SpeedometerYellow = Color.Yellow
    val SpeedometerOrange = Color(0xFFFFA500)
    val Success = Color(0xFF4ADE80)
    val SuccessAlpha = Color(0x224ADE80)
    val Warning = Color(0xFFFACC15)
    val WarningAlpha = Color(0x22FACC15)
    val Error = DarkPink80
    val ErrorAlpha = Color(0x22F87171)

    val WhiteAlpha10 = Color.White.copy(alpha = 0.1f)
    val WhiteAlpha20 = Color.White.copy(alpha = 0.2f)
    val WhiteAlpha30 = Color.White.copy(alpha = 0.3f)
    val WhiteAlpha50 = Color.White.copy(alpha = 0.5f)
    val WhiteAlpha60 = Color.White.copy(alpha = 0.6f)
    val WhiteAlpha70 = Color.White.copy(alpha = 0.7f)
    val WhiteAlpha80 = Color.White.copy(alpha = 0.8f)

    val TextPrimary = Color(0xFFE0E0FF)
    val TextSecondary = DarkBlueGrey80
    val TextTertiary = DarkBlueGrey40

    val SurfaceLight = Color(0x1AFFFFFF)
    val SurfaceMedium = Color(0x15FFFFFF)
    val SurfaceDark = Color(0x0AFFFFFF)

    val BackgroundGradient = listOf(
        Color(0xFF0A0A0F),
        Color(0xFF12121A),
        Color(0xFF1A1A24)
    )

    val SurfaceGradient = listOf(
        Color(0x990A0A0F),
        Color(0x660A0A0F)
    )

    val SpeedometerGradient = listOf(
        Color.White,
        Color.Yellow,
        Color(0xFFFFA500),
        Color.Transparent
    )

    val AccentCyan = DarkBlue80
    val AccentGreen = Color(0xFF4ADE80)
    val AccentRed = DarkPink80
    val AccentYellow = Color(0xFFFACC15)
    val AccentOrange = Color(0xFFFFA500)

    val TransparentPrimary = Color(0x1500D4FF)
    val TransparentSuccess = Color(0x154ADE80)
    val TransparentWarning = Color(0x15FACC15)
    val TransparentError = Color(0x15F87171)

    val SpeedometerTickMajor = WhiteAlpha80
    val SpeedometerTickMinor = WhiteAlpha70
    val SpeedometerTickSmall = WhiteAlpha60
    val SpeedometerArrow = WhiteAlpha30
    val SpeedometerArrowStroke = WhiteAlpha50
    val SpeedometerDialStroke = WhiteAlpha60
    val SpeedometerCenterStroke = WhiteAlpha20

    val BluetoothDeviceConnected = DarkBlue80
    val BluetoothDeviceAvailable = WhiteAlpha60
    val BluetoothStatusError = DarkPink80
    val BluetoothStatusOk = Color(0xFF4ADE80)
}

// ===== Вспомогательные функции =====

@Composable
fun verticalGradientBackground(colors: List<Color> = AppColors.BackgroundGradient): androidx.compose.ui.graphics.Brush {
    return androidx.compose.ui.graphics.Brush.verticalGradient(colors = colors)
}

@Composable
fun speedometerRadialGradient(
    center: androidx.compose.ui.geometry.Offset,
    radius: Float,
    colors: List<Color> = AppColors.SpeedometerGradient
): androidx.compose.ui.graphics.Brush {
    return androidx.compose.ui.graphics.Brush.radialGradient(
        colors = colors,
        center = center,
        radius = radius
    )
}

fun getMessageColor(message: String?): Color {
    return when {
        message == null -> AppColors.TextPrimary
        message.contains("✅") -> AppColors.Success
        message.contains("⚠️") -> AppColors.Warning
        message.contains("❌") -> AppColors.Error
        else -> AppColors.TextPrimary
    }
}

fun getMessageBackgroundColor(message: String?): Color {
    return when {
        message == null -> AppColors.SurfaceDark
        message.contains("✅") -> AppColors.SuccessAlpha
        message.contains("⚠️") -> AppColors.WarningAlpha
        message.contains("❌") -> AppColors.ErrorAlpha
        else -> AppColors.SurfaceDark
    }
}

fun getBluetoothDeviceColor(isPaired: Boolean, isSelected: Boolean = false): Color {
    return when {
        isSelected -> AppColors.BluetoothDeviceConnected
        isPaired -> AppColors.BluetoothDeviceConnected.copy(alpha = 0.7f)
        else -> AppColors.BluetoothDeviceAvailable
    }
}

// ===== Основная тема =====
@Composable
fun BluetoothCarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // УСТАНОВКА ЦВЕТА СИСТЕМНОГО UI
            // Используем фиксированный цвет фона для бесшовного интерфейса
            val statusBarColor = Color(0xFF0A0A0F).toArgb()
            
            @Suppress("DEPRECATION")
            window.statusBarColor = statusBarColor

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                @Suppress("DEPRECATION")
                window.navigationBarColor = statusBarColor
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = !darkTheme
                    controller.isAppearanceLightNavigationBars = !darkTheme
                } catch (e: Exception) {}
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
