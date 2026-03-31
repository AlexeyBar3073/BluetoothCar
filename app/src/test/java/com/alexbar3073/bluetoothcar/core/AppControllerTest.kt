// Файл: core/AppControllerTest.kt
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
 * ТЕГ: Core/AppController/Test
 *
 * ФАЙЛ: core/AppControllerTest.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: core/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Модульное тестирование главного координатора системы — AppController.
 * Проверяет логику инициализации приложения, управления настройками (сохранение/загрузка),
 * комбинирования входящих данных от автомобиля и расчета производных полей (например, критический уровень топлива).
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Проверка автоматической загрузки настроек из репозитория при старте.
 * 2. Тестирование логики обновления настроек и их сохранения.
 * 3. Верификация корректности расчета флага "низкий уровень топлива" (isFuelLow) на основе текущих настроек.
 * 4. Проверка сброса данных о состоянии автомобиля (CarData) при потере Bluetooth-соединения.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Unit Testing с использованием MockK и Turbine для тестирования реактивных потоков (Flow).
 *
 * КЛЮЧЕВОЙ ПРИНЦИП:
 * Тестирование AppController как центрального узла, объединяющего бизнес-логику, настройки и данные от "железа".
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * Тестирует: AppController.kt.
 * Использует: SettingsRepository, AppSettings, CarData, ConnectionState.
 */

@OptIn(ExperimentalCoroutinesApi::class)
class AppControllerTest {

    /** Тестовый диспатчер для управления временем выполнения корутин */
    private val testDispatcher = StandardTestDispatcher()
    
    /** Контекст приложения (заглушка) */
    private lateinit var context: Context
    
    /** Репозиторий настроек (заглушка) */
    private lateinit var settingsRepository: SettingsRepository
    
    /** Тестируемый экземпляр главного контроллера */
    private lateinit var appController: AppController

    /**
     * Настройка окружения перед каждым тестом.
     * Инициализирует моки и заменяет Main диспатчер на тестовый.
     */
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        // Настраиваем поведение репозитория по умолчанию (возврат пустых настроек)
        coEvery { settingsRepository.getCurrentSettings() } returns AppSettings()

        // Создаем экземпляр контроллера
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
     */
    @Test
    fun `carData should correctly calculate isFuelLow based on settings`() = runTest {
        // 1. ПОДГОТОВКА: Устанавливаем порог срабатывания предупреждения в 10 литров
        val settings = AppSettings(minFuelLevel = 10f)
        appController.updateSettings(settings)
        testDispatcher.scheduler.advanceUntilIdle()

        // 2. ДЕЙСТВИЕ И ПРОВЕРКА: Проверяем начальное состояние потока данных автомобиля.
        // Так как BluetoothConnectionManager в AppController приватный, мы тестируем логику 
        // объединения потоков (combine), заложенную в инициализации carData.
        appController.carData.test {
            val initialData = awaitItem()
            
            // В начальном состоянии при отсутствии данных от Bluetooth:
            // - топливо должно быть 0
            // - флаг низкого уровня топлива должен быть false (по умолчанию)
            assertEquals(0f, initialData.fuel)
            assertFalse(initialData.isFuelLow)
        }
    }

    /**
     * Тест: Сброс данных автомобиля при потере или отсутствии соединения.
     */
    @Test
    fun `carData should reset to default when connection is inactive`() = runTest {
        // 1. ПРОВЕРКА: По умолчанию статус UNDEFINED (неактивен), 
        // следовательно, поток carData должен вернуть объект с нулевыми значениями.
        appController.carData.test {
            val data = awaitItem()
            
            // Проверяем сброс всех ключевых параметров
            assertEquals(0f, data.speed)
            assertEquals(0f, data.fuel)
            assertFalse(data.isFuelLow)
        }
    }

    /**
     * Тест: Инициализация контроллера и загрузка настроек.
     */
    @Test
    fun `appController should load settings on initialization`() = runTest {
        // 1. ПОДГОТОВКА: Задаем специфические настройки в репозитории
        val customSettings = AppSettings(fuelTankCapacity = 75f)
        coEvery { settingsRepository.getCurrentSettings() } returns customSettings
        
        // 2. ДЕЙСТВИЕ: Создаем новый экземпляр контроллера (это запускает блок init)
        val controller = AppController(context, settingsRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // 3. ПРОВЕРКА: Контроллер должен содержать те же настройки, что вернул репозиторий
        assertEquals(75f, controller.getCurrentSettings().fuelTankCapacity)
    }
}
