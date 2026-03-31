// Файл: data/bluetooth/BluetoothConnectionManagerTest.kt
package com.alexbar3073.bluetoothcar.data.bluetooth

import android.content.Context
import com.alexbar3073.bluetoothcar.MainDispatcherRule
import com.alexbar3073.bluetoothcar.data.bluetooth.listeners.ConnectionFeasibilityChecker
import com.alexbar3073.bluetoothcar.data.bluetooth.listeners.ConnectionStateManager
import com.alexbar3073.bluetoothcar.data.bluetooth.listeners.DataStreamHandler
import com.alexbar3073.bluetoothcar.data.bluetooth.listeners.DeviceAvailabilityMonitor
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * ТЕГ: Bluetooth/Connection/Test
 *
 * ФАЙЛ: data/bluetooth/BluetoothConnectionManagerTest.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: data/bluetooth/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Модульное тестирование главного координатора Bluetooth-соединения.
 * Проверяет логику оркестрации между четырьмя помощниками (Checker, Monitor, Manager, Handler)
 * и корректность переходов состояний при изменении настроек или внешних событиях.
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Проверка инициализации помощников при создании менеджера.
 * 2. Верификация запуска процесса подключения при выборе устройства.
 * 3. Тестирование корректности остановки всех процессов при очистке (cleanup).
 * 4. Проверка логики перезапуска соединения при смене устройства.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Unit Testing с использованием MockK для имитации зависимостей и конструкторов.
 *
 * КЛЮЧЕВОЙ ПРИНЦИП:
 * Использование mockkConstructor для перехвата создания внутренних объектов-помощников,
 * так как BluetoothConnectionManager создает их в методе init.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * Тестирует: BluetoothConnectionManager.kt.
 * Использует: AppBluetoothService, помощники из папки listeners/.
 */

@OptIn(ExperimentalCoroutinesApi::class)
class BluetoothConnectionManagerTest {

    /** Правило для подмены Main диспатчера в тестах */
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Контекст приложения (заглушка) */
    private lateinit var context: Context
    
    /** Сервис Bluetooth (заглушка) */
    private lateinit var bluetoothService: AppBluetoothService
    
    /** Тестируемый экземпляр менеджера */
    private lateinit var manager: BluetoothConnectionManager
    
    /** Тестовый диспатчер для управления временем выполнения корутин */
    private val testDispatcher = UnconfinedTestDispatcher()

    /**
     * Настройка окружения перед каждым тестом.
     * Мокаются конструкторы всех помощников, чтобы контролировать их вызовы.
     */
    @Before
    fun setup() {
        // Создаем расслабленные моки для базовых зависимостей
        context = mockk(relaxed = true)
        bluetoothService = mockk(relaxed = true)
        
        // Перехватываем создание экземпляров помощников через mockkConstructor
        mockkConstructor(ConnectionFeasibilityChecker::class)
        mockkConstructor(DeviceAvailabilityMonitor::class)
        mockkConstructor(ConnectionStateManager::class)
        mockkConstructor(DataStreamHandler::class)

        // Задаем поведение по умолчанию для методов start/stop у всех помощников
        every { anyConstructed<ConnectionFeasibilityChecker>().start(any()) } just runs
        every { anyConstructed<ConnectionFeasibilityChecker>().stop() } just runs
        
        every { anyConstructed<DeviceAvailabilityMonitor>().start(any()) } just runs
        every { anyConstructed<DeviceAvailabilityMonitor>().stop() } just runs
        
        every { anyConstructed<ConnectionStateManager>().start(any()) } just runs
        every { anyConstructed<ConnectionStateManager>().stop() } just runs
        
        every { anyConstructed<DataStreamHandler>().stop() } just runs

        // Инициализируем менеджер, что приведет к вызову createAllHelpers()
        manager = BluetoothConnectionManager(context, bluetoothService)
    }

    /**
     * Очистка после каждого теста.
     * Снимает все моки, чтобы не влиять на следующие тесты.
     */
    @After
    fun tearDown() {
        unmockkAll()
    }

    /**
     * Тест: должен запустить процесс подключения при первой установке устройства.
     */
    @Test
    fun `should start connection process when device is first selected`() = runTest(testDispatcher) {
        // Подготовка данных
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        
        // Действие: обновляем выбранное устройство (аналог выбора в UI)
        manager.updateSelectedDevice(device)
        runCurrent()
        
        // Проверка: должен быть вызван метод start у FeasibilityChecker для этого устройства
        verify { anyConstructed<ConnectionFeasibilityChecker>().start(device) }
    }

    /**
     * Тест: должен перезапустить процесс подключения при смене адреса устройства.
     */
    @Test
    fun `should restart connection process when device changes`() = runTest(testDispatcher) {
        // Подготовка двух разных устройств
        val device1 = BluetoothDeviceData("Device1", "00:11:22:33:44:55")
        val device2 = BluetoothDeviceData("Device2", "AA:BB:CC:DD:EE:FF")
        
        // Устанавливаем первое устройство
        manager.updateSelectedDevice(device1)
        runCurrent()
        
        // Устанавливаем второе устройство (смена адреса)
        manager.updateSelectedDevice(device2)
        runCurrent()
        
        // Проверка: старые процессы должны быть остановлены, а для нового устройства запущен Checker
        verify { anyConstructed<ConnectionFeasibilityChecker>().stop() }
        verify { anyConstructed<ConnectionFeasibilityChecker>().start(device2) }
    }

    /**
     * Тест: принудительный перезапуск должен остановить всё и начать с первого шага.
     */
    @Test
    fun `retryConnection should stop all and start CFC`() = runTest(testDispatcher) {
        // Подготовка
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        manager.updateSelectedDevice(device)
        runCurrent()
        
        // Действие: инициируем полный перезапуск процесса
        manager.startConnectionProcess()
        runCurrent()
        
        // Проверка: все помощники должны быть остановлены перед новым стартом
        verify { anyConstructed<ConnectionFeasibilityChecker>().stop() }
        verify { anyConstructed<DeviceAvailabilityMonitor>().stop() }
        verify { anyConstructed<ConnectionStateManager>().stop() }
        verify { anyConstructed<DataStreamHandler>().stop() }
        
        // Checker должен быть запущен минимум 2 раза (первый раз при установке девайса, второй при startConnectionProcess)
        verify(atLeast = 2) { anyConstructed<ConnectionFeasibilityChecker>().start(device) }
    }
    
    /**
     * Тест: очистка ресурсов должна останавливать все активные процессы.
     */
    @Test
    fun `should stop all processes on cleanup`() = runTest(testDispatcher) {
        // Действие: вызываем очистку
        manager.cleanup()
        runCurrent()
        
        // Проверка: все 4 помощника должны получить команду на остановку
        verify { anyConstructed<ConnectionFeasibilityChecker>().stop() }
        verify { anyConstructed<DeviceAvailabilityMonitor>().stop() }
        verify { anyConstructed<ConnectionStateManager>().stop() }
        verify { anyConstructed<DataStreamHandler>().stop() }
    }
}
