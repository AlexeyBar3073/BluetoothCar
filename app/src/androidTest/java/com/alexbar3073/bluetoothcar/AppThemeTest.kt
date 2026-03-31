// Файл: AppThemeTest.kt
package com.alexbar3073.bluetoothcar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ТЕГ: Тесты тем и запуска / AppThemeTest
 *
 * ФАЙЛ: AppThemeTest.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: /src/androidTest/java/com/alexbar3073/bluetoothcar/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Инструментальные тесты для проверки корректности инициализации приложения 
 * и смены тем оформления в реальном окружении.
 *
 * ОТВЕТСТВЕННОСТЬ: Гарантия работоспособности механизмов переключения тем и запуска.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: UI Testing (Compose Test Rule)
 *
 * КЛЮЧЕВОЙ ПРИНЦИП: Имитация действий пользователя для проверки реактивности UI.
 */
@RunWith(AndroidJUnit4::class)
class AppThemeTest {

    /** Правило для запуска основной активности */
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * Тест: Проверка запуска приложения.
     * Проверяет, что после этапов загрузки мы видим либо экран разрешений,
     * либо главный экран.
     */
    @Test
    fun testAppStartupFlow() {
        // Ждем исчезновения экранов инициализации (таймаут 20 сек для медленных устройств)
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithText("Инициализация", substring = true)
                .fetchSemanticsNodes().isEmpty()
        }

        // Проверяем наличие ключевых элементов. 
        // Используем более гибкий поиск через substring или ignoreCase.
        val hasPermissions = composeTestRule.onAllNodesWithText("РАЗРЕШЕНИЯ", substring = true, ignoreCase = true)
            .fetchSemanticsNodes().isNotEmpty()
            
        val hasHome = composeTestRule.onAllNodesWithText("БОРТОВОЙ КОМПЬЮТЕР", substring = true, ignoreCase = true)
            .fetchSemanticsNodes().isNotEmpty()
                               
        assert(hasPermissions || hasHome) { 
            "Приложение не дошло до ожидаемого начального экрана. Текущие ноды: " + 
            composeTestRule.onRoot().printToString() 
        }
    }

    /**
     * Тест: Проверка смены темы оформления.
     * Имитирует переход в настройки и выбор темы "Синяя темная".
     */
    @Test
    fun testThemeSelection() {
        // 1. Ожидаем загрузки приложения
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithText("Инициализация", substring = true)
                .fetchSemanticsNodes().isEmpty()
        }

        // 2. Если мы на экране разрешений, нажимаем "Пропустить" (Skip)
        val skipButton = composeTestRule.onAllNodesWithText("ПРОПУСТИТЬ", ignoreCase = true)
        if (skipButton.fetchSemanticsNodes().isNotEmpty()) {
            skipButton[0].performClick()
        }

        // 3. Переходим в настройки
        // Ищем по content description, так как это IconButton
        composeTestRule.onNodeWithContentDescription("Настройки", ignoreCase = true).performClick()

        // 4. Ищем пункт "Тема оформления" в списке настроек и кликаем
        composeTestRule.onNodeWithText("Тема оформления", substring = true).performClick()

        // 5. В диалоге ищем "Синяя темная" и выбираем
        composeTestRule.onNodeWithText("Синяя темная", substring = true).performClick()

        // 6. Проверяем, что в списке настроек теперь отображается "Синяя темная"
        // (Диалог закрывается сам после выбора в ThemeSelectionDialog)
        composeTestRule.onNodeWithText("Синяя темная").assertIsDisplayed()
        
        // 7. Проверяем, что заголовок диалога исчез
        composeTestRule.onNodeWithText("Выбор темы оформления").assertDoesNotExist()
    }
}
