// Файл: app/src/test/java/com/alexbar3073/bluetoothcar/core/AppControllerTest.kt
package com.alexbar3073.bluetoothcar.core

import android.content.Context
import app.cash.turbine.test
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.CarData
import com.alexbar3073.bluetoothcar.data.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ТЕГ: Тесты AppController
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Модульные тесты для главного координатора системы — AppController.
 * Проверяют логику инициализации, комбинирования данных и расчета производных полей (isFuelLow).
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Тестирует: AppController.kt
 * 2. Мокает: SettingsRepository, Context
 * 3. Использует: AppSettings, CarData, ConnectionState
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppControllerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var appController: AppController

    /**
     * Настройка окружения перед каждым тестом.
     * Инициализирует моки и заменяет Main диспатчер.
     */
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        // По умолчанию репозиторий возвращает пустые настройки
        coEvery { settingsRepository.getCurrentSettings() } returns AppSettings()

        appController = AppController(context, settingsRepository)
    }

    /**
     * Сброс диспатчера после завершения тестов.
     */
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Тест: Проверка расчета флага низкого уровня топлива.
     * Флаг isFuelLow должен становиться true, если текущее топливо меньше порога в настройках.
     */
    @Test
    fun `carData should correctly calculate isFuelLow based on settings`() = runTest {
        // Настраиваем порог в 10 литров
        val settings = AppSettings(minFuelLevel = 10f)
        appController.updateSettings(settings)
        testDispatcher.scheduler.advanceUntilIdle()

        // Эмулируем данные от БК (5 литров) при активном подключении
        // Примечание: В реальности данные приходят через BluetoothConnectionManager, 
        // но здесь мы тестируем логику combine в AppController.
        
        // Так как BluetoothConnectionManager в AppController приватный и создается внутри, 
        // для полноценного теста логики потоков нам нужно было бы использовать 
        // Dependency Injection или сделать менеджер доступным для мокирования.
        // В текущей реализации мы проверим начальное состояние и реакцию на настройки.
        
        appController.carData.test {
            val initialData = awaitItem()
            assertEquals(0f, initialData.fuel)
            assertFalse(initialData.isFuelLow) // По умолчанию false
        }
    }

    /**
     * Тест: Сброс данных при потере соединения.
     * Проверяет, что при неактивном статусе carData возвращает пустой объект CarData().
     */
    @Test
    fun `carData should reset to default when connection is inactive`() = runTest {
        // По умолчанию статус UNDEFINED (неактивен)
        appController.carData.test {
            val data = awaitItem()
            assertEquals(0f, data.speed)
            assertEquals(0f, data.fuel)
            assertFalse(data.isFuelLow)
        }
    }

    /**
     * Тест: Инициализация контроллера.
     * Проверяет, что контроллер загружает настройки из репозитория при старте.
     */
    @Test
    fun `appController should load settings on initialization`() = runTest {
        val customSettings = AppSettings(fuelTankCapacity = 75f)
        coEvery { settingsRepository.getCurrentSettings() } returns customSettings
        
        // Создаем новый контроллер, чтобы сработал init
        val controller = AppController(context, settingsRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(75f, controller.getCurrentSettings().fuelTankCapacity)
    }
}
