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
 */
@RunWith(AndroidJUnit4::class)
class AppThemeTest {

    /** Правило для запуска основной активности */
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * Тест: Проверка запуска приложения.
     */
    @Test
    fun testAppStartupFlow() {
        // 1. Ожидаем появления иерархии
        waitForHierarchy()

        // 2. Ждем исчезновения экранов инициализации
        composeTestRule.waitUntil(30000) {
            try {
                composeTestRule.onAllNodes(hasText("Инициализация", substring = true))
                    .fetchSemanticsNodes().isEmpty()
            } catch (e: Exception) {
                false
            }
        }

        // 3. Проверяем наличие ключевых элементов (Разрешения или Главный экран)
        val hasPermissions = composeTestRule.onAllNodes(hasText("Необходимые разрешения", substring = true, ignoreCase = true))
            .fetchSemanticsNodes().isNotEmpty()
            
        val hasHome = composeTestRule.onAllNodes(hasText("БОРТОВОЙ КОМПЬЮТЕР", substring = true, ignoreCase = true))
            .fetchSemanticsNodes().isNotEmpty()
                               
        assert(hasPermissions || hasHome) { 
            "Приложение не дошло до ожидаемого начального экрана." 
        }
    }

    /**
     * Тест: Проверка смены темы оформления.
     */
    @Test
    fun testThemeSelection() {
        // 1. Ожидаем появления иерархии
        waitForHierarchy()

        // 2. Ждем завершения инициализации
        composeTestRule.waitUntil(30000) {
            try {
                composeTestRule.onAllNodes(hasText("Инициализация", substring = true))
                    .fetchSemanticsNodes().isEmpty()
            } catch (e: Exception) {
                false
            }
        }

        // 3. Если мы на экране разрешений, нажимаем "Продолжить без разрешений"
        try {
            val skipButton = composeTestRule.onAllNodes(hasText("без разрешений", substring = true, ignoreCase = true))
            if (skipButton.fetchSemanticsNodes().isNotEmpty()) {
                skipButton[0].performClick()
            }
        } catch (e: Exception) {
            // Игнорируем, если экран уже сменился
        }

        // 4. Ожидаем появления главного экрана
        composeTestRule.waitUntil(30000) {
            try {
                composeTestRule.onAllNodes(hasText("БОРТОВОЙ КОМПЬЮТЕР", ignoreCase = true))
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }

        // 5. Переходим в настройки
        composeTestRule.onNodeWithContentDescription("Настройки", ignoreCase = true).performClick()

        // 6. Ищем пункт "Тема оформления" и кликаем
        composeTestRule.onNodeWithText("Тема оформления", substring = true).performClick()

        // 7. Выбираем "Синяя темная"
        composeTestRule.onNodeWithText("Синяя темная", substring = true).performClick()

        // 8. Проверка
        composeTestRule.onNodeWithText("Синяя темная").assertIsDisplayed()
    }

    /**
     * Вспомогательный метод для ожидания готовности Compose иерархии.
     */
    private fun waitForHierarchy() {
        composeTestRule.waitUntil(20000) {
            try {
                composeTestRule.onAllNodes(isRoot()).fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
    }
}
