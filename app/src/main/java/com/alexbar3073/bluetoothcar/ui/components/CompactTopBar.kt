// Файл: ui/components/CompactTopBar.kt
package com.alexbar3073.bluetoothcar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alexbar3073.bluetoothcar.ui.theme.AppColors
import com.alexbar3073.bluetoothcar.ui.theme.COMPACT_TOP_BAR_HEIGHT

/**
 * ТЕГ: UI/Components/TopBar
 * 
 * ФАЙЛ: ui/components/CompactTopBar.kt
 * 
 * МЕСТОНАХОЖДЕНИЕ: ui/components/
 * 
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ: Общий компонент верхней панели навигации. 
 * Обеспечивает единый визуальный стиль и фиксированную высоту (40 dp) для всех экранов.
 * 
 * ОТВЕТСТВЕННОСТЬ: Отображение заголовка, навигационных кнопок и статусных индикаторов.
 */

@Composable
fun CompactTopBar(
    title: String,
    modifier: Modifier = Modifier,
    titleIcon: ImageVector? = null,
    titleIconTint: Color = AppColors.PrimaryBlue,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    leftContent: @Composable (BoxScope.() -> Unit)? = null,
    rightContent: @Composable (BoxScope.() -> Unit)? = null,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(COMPACT_TOP_BAR_HEIGHT),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp)
        ) {
            // ЛЕВАЯ СЕКЦИЯ: Кнопка навигации или пользовательский контент
            Box(
                modifier = Modifier.align(Alignment.CenterStart),
                contentAlignment = Alignment.Center
            ) {
                if (leftContent != null) {
                    leftContent()
                } else if (navigationIcon != null && onNavigationClick != null) {
                    TopBarButton(
                        icon = navigationIcon,
                        onClick = onNavigationClick,
                        contentDescription = "Назад"
                    )
                }
            }

            // ЦЕНТРАЛЬНАЯ СЕКЦИЯ: Иконка и заголовок (увеличены на 10%)
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (titleIcon != null) {
                    // Иконка увеличена с 16.dp до 18.dp (+10%)
                    Icon(
                        imageVector = titleIcon,
                        contentDescription = null,
                        tint = titleIconTint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                // Текст заголовка увеличен с 12.sp до 13.5.sp (+10%)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 13.5.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = AppColors.TextPrimary
                )
            }

            // ПРАВАЯ СЕКЦИЯ: Кнопки действий или настройки
            if (rightContent != null) {
                Box(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    contentAlignment = Alignment.Center
                ) {
                    rightContent()
                }
            }
        }
    }
}

/**
 * Базовый компонент кнопки для Топбара. Обеспечивает единый размер.
 */
@Composable
fun TopBarButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        // Единый фоновый круг для всех кнопок (фиксированный размер 28dp)
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(AppColors.SurfaceMedium, shape = CircleShape)
        )
        content()
    }
}

/**
 * Кнопка с иконкой для Топбара.
 * Используется для кнопок навигации и действий в панели.
 */
@Composable
fun TopBarButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    iconSize: androidx.compose.ui.unit.Dp = 16.dp,
    tint: Color = AppColors.TextPrimary
) {
    TopBarButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
