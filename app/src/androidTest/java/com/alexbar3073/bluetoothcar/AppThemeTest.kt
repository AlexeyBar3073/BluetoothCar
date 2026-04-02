// Файл: AppThemeTest.kt
package com.alexbar3073.bluetoothcar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ТЕГ: Тесты запуска / AppStartupTest
 * 
 * ИЗМЕНЕНИЕ: Тест смены темы удален, так как приложение теперь поддерживает только темную тему.
 */
@RunWith(AndroidJUnit4::class)
class AppThemeTest {

    /** Правило для запуска основной активности */
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * Тест: Проверка запуска приложения.
     * Проверяет, что приложение успешно проходит этап инициализации и доходит 
     * до экрана разрешений или главного экрана.
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
