// Файл: ui/viewmodels/SharedViewModelFactory.kt
package com.alexbar3073.bluetoothcar.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.alexbar3073.bluetoothcar.core.CoreModule
import com.alexbar3073.bluetoothcar.data.repository.SettingsRepository

/**
 * ФАЙЛ: ui/viewmodels/SharedViewModelFactory.kt
 * МЕСТОНАХОЖДЕНИЕ: ui/viewmodels/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Фабрика для создания SharedViewModel с внедрением зависимостей.
 * Получает зависимости через CoreModule (ServiceLocator).
 *
 * ИЗМЕНЕНИЯ ДЛЯ НОВОГО SHAREDVIEWMODEL:
 * 1. AppController теперь НЕ NULLABLE - фабрика должна гарантировать его наличие
 * 2. Удален applicationContext из конструктора SharedViewModel
 * 3. Упрощена логика - либо получаем AppController, либо бросаем исключение
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ ПРОЕКТА:
 * 1. Использует: CoreModule.kt (получение AppController)
 * 2. Создает: SharedViewModel.kt (новая версия)
 * 3. Получает зависимости: SettingsRepository (но SharedViewModel больше не использует его)
 *
 * ИСТОРИЯ ИЗМЕНЕНИЙ:
 * - 2026.02.02 19:45 UTC: ИНТЕГРАЦИЯ С COREMODULE
 * - 2026.02.02 22:45 UTC: УПРОЩЕНИЕ ФАБРИКИ
 * - 2026.02.03 12:00 UTC: ОБНОВЛЕНИЕ ДЛЯ НОВОГО SHAREDVIEWMODEL
 *   1. AppController теперь обязательный (не nullable)
 *   2. Удален applicationContext из вызова конструктора
 *   3. SettingsRepository больше не передается (SharedViewModel не использует его)
 *   4. Добавлена проверка на null для AppController
 */
class SharedViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    companion object {
        private const val TAG = "SharedViewModelFactory"

        private fun log(message: String) {
            Log.d(TAG, message)
        }

        private fun logError(message: String) {
            Log.e(TAG, message)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(SharedViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }

        try {
            log("Создание SharedViewModel через фабрику")

            // 1. Пытаемся получить AppController из CoreModule
            val appController = try {
                CoreModule.getAppController()
            } catch (e: IllegalStateException) {
                logError("AppController еще не зарегистрирован в CoreModule: ${e.message}")
                throw IllegalStateException(
                    "AppController не инициализирован. Убедитесь, что CoreModule.initialize() был вызван.",
                    e
                )
            }

            if (appController == null) {
                logError("CoreModule.getAppController() вернул null")
                throw IllegalStateException("AppController не доступен")
            }

            // 2. Создаем SharedViewModel
            // НОВЫЙ конструктор: только AppController
            return SharedViewModel(appController = appController) as T

        } catch (e: Exception) {
            logError("Критическая ошибка создания SharedViewModel: ${e.message}")
            e.printStackTrace()
            throw RuntimeException("Не удалось создать SharedViewModel: ${e.message}", e)
        }
    }
}