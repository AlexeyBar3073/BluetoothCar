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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * ТЕГ: Theme/Core/Colors
 * 
 * ФАЙЛ: ui/theme/Theme.kt
 * 
 * МЕСТОНАХОЖДЕНИЕ: ui/theme/Theme.kt
 * 
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ: Центральный файл темы приложения. Определяет цветовые палитры,
 * типографику и общие константы размеров для обеспечения консистентного UI.
 * 
 * ОТВЕТСТВЕННОСТЬ: Управление визуальным стилем, поддержка светлой, темной и динамических тем.
 * 
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Jetpack Compose Theme (Material3).
 * 
 * КЛЮЧЕВОЙ ПРИНЦИП: Single Source of Truth для всех стилистических параметров приложения.
 */

/** 
 * Высота компактного топбара. 
 */
val COMPACT_TOP_BAR_HEIGHT = 40.dp

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    secondary = Blue40,
    tertiary = Pink80,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkBlueGrey80,
    outline = Color.White.copy(alpha = 0.12f)
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = Blue30,
    tertiary = Purple40,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color.Black.copy(alpha = 0.6f),
    outline = Color.Black.copy(alpha = 0.12f)
)

private val BlueDarkColorScheme = darkColorScheme(
    primary = Blue80,
    secondary = Blue40,
    tertiary = Blue90,
    background = Blue10,
    surface = Blue20,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Blue95,
    onSurface = Blue90,
)

object AppColors {
    /** Основной синий цвет бренда */
    val PrimaryBlue: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
    
    /** Полупрозрачный основной синий для подложек */
    val TransparentPrimary: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    
    /** Основной цвет текста */
    val TextPrimary: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface
    
    /** Второстепенный цвет текста */
    val TextSecondary: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
    
    /** 
     * Третичный (неактивный) цвет текста.
     * Скорректирован для соответствия DASHBOARD_4_SPEC в темной теме.
     */
    val TextTertiary: Color @Composable @ReadOnlyComposable get() = 
        if (MaterialTheme.colorScheme.background.luminance() > 0.5f)
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
        else
            DarkBlueGrey40

    /**
     * Семантический цвет для подробностей и описаний в карточках.
     * Базируется на TextSecondary, который обеспечивает хорошую читаемость.
     */
    val ContentDetail: Color @Composable @ReadOnlyComposable get() = TextSecondary
    
    /** Цвет успеха (зеленый) */
    val Success = Green80

    /** Цвет активного состояния получения данных (BT) */
    val StatusListening: Color @Composable @ReadOnlyComposable get() = 
        if (MaterialTheme.colorScheme.background.luminance() > 0.5f) ListeningGreenDim else ListeningGreenBright
    
    /** Цвет предупреждения (желтый) */
    val Warning = Yellow80
    
    /** Цвет ошибки (красный) */
    val Error = Red80
    
    /** Полупрозрачный цвет ошибки для фона уведомлений */
    val ErrorAlpha: Color @Composable @ReadOnlyComposable get() = Error.copy(alpha = 0.15f)

    /** Легкое выделение поверхности */
    val SurfaceLight: Color @Composable @ReadOnlyComposable get() = Color(0x1AFFFFFF)
    
    /** Среднее выделение поверхности */
    val SurfaceMedium: Color @Composable @ReadOnlyComposable get() = Color(0x15FFFFFF)
    
    /** Сильное выделение поверхности (SurfaceOverlay из спецификации) */
    val SurfaceDark: Color @Composable @ReadOnlyComposable get() = Color(0x0AFFFFFF)

    /** Белый с прозрачностью */
    val WhiteAlpha10 = Color.White.copy(alpha = 0.1f)
    val WhiteAlpha20 = Color.White.copy(alpha = 0.2f)
    val WhiteAlpha30 = Color.White.copy(alpha = 0.3f)
    val WhiteAlpha60 = Color.White.copy(alpha = 0.6f)
    val WhiteAlpha80 = Color.White.copy(alpha = 0.8f)

    /** Фон диалоговых окон */
    val DialogBackground: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surface
    
    /** Цвет границы диалоговых окон */
    val DialogBorder: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outline

    /** Градиент для поверхностей (TopBar и т.д.) согласно DASHBOARD_4_SPEC */
    val SurfaceGradient: List<Color> @Composable @ReadOnlyComposable get() = 
        if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
            listOf(DarkBackground.copy(alpha = 0.6f), DarkBackground.copy(alpha = 0.4f))
        } else {
            listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            )
        }
    
    /** Цвет подключенного Bluetooth-устройства */
    val BluetoothDeviceConnected = Blue80
    
    /** Цвет доступного для подключения Bluetooth-устройства */
    val BluetoothDeviceAvailable: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    // ===== ЦВЕТА ПРИБОРОВ (DASHBOARD) =====

    val GaugeStroke: Color @Composable @ReadOnlyComposable get() = 
        if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Color(0xFF2A2A34)
        else Color.White

    val GaugeValue: Color @Composable @ReadOnlyComposable get() = 
        if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Color(0xFF1A1A24)
        else Color.White

    val GaugeSecondary: Color @Composable @ReadOnlyComposable get() = 
        if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Color(0xFF2A2A34).copy(alpha = 0.7f)
        else Color.White.copy(alpha = 0.6f)

    val SpeedometerGradient = listOf(Blue80, Blue40)
    val SpeedometerDialStroke: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val SpeedometerCenterStroke: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val SpeedometerTickMajor: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface
    val SpeedometerTickMinor: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val SpeedometerTickSmall: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val SpeedometerArrow = Blue80
    val SpeedometerArrowStroke: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
}

@Composable
fun verticalGradientBackground(): Brush {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val colors = if (isDark) {
        // Возвращаем наш эталонный глубокий градиент из спецификации
        listOf(DarkBackground, DarkTransitionGray, DarkSurface)
    } else {
        listOf(colorScheme.background, colorScheme.surfaceVariant.copy(alpha = 0.5f), colorScheme.surface)
    }
    return Brush.verticalGradient(colors = colors)
}

@Composable
fun BluetoothCarTheme(
    themeMode: String = "dark",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (themeMode == "light") dynamicLightColorScheme(context) else dynamicDarkColorScheme(context)
        }
        themeMode == "light" -> LightColorScheme
        themeMode == "blue_dark" -> BlueDarkColorScheme
        else -> DarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val context = view.context
            if (context is Activity) {
                val window = context.window
                val statusBarColor = colorScheme.background.toArgb()
                @Suppress("DEPRECATION")
                window.statusBarColor = statusBarColor
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = statusBarColor
                }
                val controller = WindowCompat.getInsetsController(window, view)
                val isLightTheme = themeMode == "light"
                controller.isAppearanceLightStatusBars = isLightTheme
                controller.isAppearanceLightNavigationBars = isLightTheme
            }
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
