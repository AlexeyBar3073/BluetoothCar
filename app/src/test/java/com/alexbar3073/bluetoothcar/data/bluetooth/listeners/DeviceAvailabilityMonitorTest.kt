// Файл: data/bluetooth/listeners/DeviceAvailabilityMonitorTest.kt
package com.alexbar3073.bluetoothcar.data.bluetooth.listeners

import com.alexbar3073.bluetoothcar.MainDispatcherRule
import com.alexbar3073.bluetoothcar.data.bluetooth.AppBluetoothService
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * ТЕГ: Bluetooth/Monitor/Test
 *
 * ФАЙЛ: data/bluetooth/listeners/DeviceAvailabilityMonitorTest.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: data/bluetooth/listeners/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Модульное тестирование компонента DeviceAvailabilityMonitor.
 * Проверяет логику поиска устройства в эфире (Discovery) перед попыткой подключения.
 * Тестирует механизм повторных попыток поиска (Retries) и переходы состояний при обнаружении устройства.
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Проверка запуска процесса сканирования (Discovery).
 * 2. Тестирование перехода в состояние DEVICE_AVAILABLE при нахождении нужного MAC-адреса.
 * 3. Проверка логики ретраев (3 попытки с интервалом) при неудачном поиске.
 * 4. Верификация перехода в DEVICE_UNAVAILABLE после исчерпания всех попыток.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Unit Testing с использованием MockK.
 *
 * КЛЮЧЕВОЙ ПРИНЦИП:
 * Имитация асинхронных событий Bluetooth-сканирования (через захват колбэков)
 * и управление виртуальным временем для проверки задержек между попытками.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * Тестирует: DeviceAvailabilityMonitor.kt.
 * Использует: AppBluetoothService, ConnectionState.
 */

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceAvailabilityMonitorTest {

    /** Правило для подмены Main диспатчера */
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Мок сервиса Bluetooth */
    private lateinit var bluetoothService: AppBluetoothService
    
    /** Тестируемый экземпляр монитора */
    private lateinit var monitor: DeviceAvailabilityMonitor
    
    /** Мок колбэка для отслеживания изменений состояния */
    private lateinit var stateCallback: (ConnectionState, String?) -> Unit
    
    /** Тестовый диспатчер для управления временем выполнения корутин */
    private val testDispatcher = StandardTestDispatcher()

    /** Поток для имитации обнаружения устройств */
    private val discoveryFlow = MutableSharedFlow<BluetoothDeviceData>()
    
    /** Поток для имитации состояния поиска */
    private val isDiscoveringFlow = MutableStateFlow(false)

    /**
     * Инициализация перед каждым тестом.
     */
    @Before
    fun setup() {
        bluetoothService = mockk(relaxed = true)
        stateCallback = mockk(relaxed = true)
        
        // Настраиваем мок сервиса на возврат тестовых потоков
        every { bluetoothService.discoveryFlow } returns discoveryFlow
        every { bluetoothService.isDiscovering } returns isDiscoveringFlow
        every { bluetoothService.startDiscovery() } answers {
            isDiscoveringFlow.value = true
            true
        }
        every { bluetoothService.stopDiscovery() } answers {
            isDiscoveringFlow.value = false
        }

        // Инициализируем монитор с тестовым диспатчером
        monitor = DeviceAvailabilityMonitor(bluetoothService, stateCallback, testDispatcher)
    }

    /**
     * Тест: должен перейти в состояние DEVICE_AVAILABLE при нахождении устройства.
     */
    @Test
    fun `should return DEVICE_AVAILABLE when device is found`() = runTest(testDispatcher) {
        // 1. ПОДГОТОВКА: Создаем тестовое устройство
        val device = BluetoothDeviceData("Target", "00:11:22:33:44:55")

        // 2. ДЕЙСТВИЕ: Запускаем мониторинг
        monitor.start(device)
        runCurrent()

        // 3. ПРОВЕРКА: Проверяем, что состояние изменилось на "Поиск"
        verify { stateCallback(ConnectionState.SEARCHING_DEVICE, null) }

        // 4. ДЕЙСТВИЕ: Имитируем нахождение устройства с нужным адресом через поток
        discoveryFlow.emit(device)
        runCurrent()
            
        // 5. ПРОВЕРКА: Состояние должно стать DEVICE_AVAILABLE, а сканирование — прекратиться
        verify { stateCallback(ConnectionState.DEVICE_AVAILABLE, null) }
        verify { bluetoothService.stopDiscovery() }
    }

    /**
     * Тест: должен повторить поиск, если первая попытка завершилась безрезультатно.
     */
    @Test
    fun `should retry discovery when first attempt finishes without finding device`() = runTest(testDispatcher) {
        // 1. ПОДГОТОВКА: Устройство
        val device = BluetoothDeviceData("Target", "00:11:22:33:44:55")

        // 2. ДЕЙСТВИЕ: Запускаем мониторинг
        monitor.start(device)
        runCurrent()

        // 3. ДЕЙСТВИЕ: Имитируем завершение первого цикла сканирования
        // В реальном сервисе это происходит через BroadcastReceiver, 
        // который меняет isDiscovering на false.
        isDiscoveringFlow.value = false
        runCurrent()
            
        // Ждем интервал между попытками (1000мс)
        advanceTimeBy(1001)
        runCurrent()

        // 4. ПРОВЕРКА: Метод запуска сканирования должен быть вызван повторно
        verify(exactly = 2) { bluetoothService.startDiscovery() }
    }

    /**
     * Тест: должен выдать DEVICE_UNAVAILABLE после 3-х неудачных попыток.
     */
    @Test
    fun `should return DEVICE_UNAVAILABLE after 3 failed attempts`() = runTest(testDispatcher) {
        // 1. ПОДГОТОВКА: Устройство
        val device = BluetoothDeviceData("Target", "00:11:22:33:44:55")

        // 2. ДЕЙСТВИЕ: Запускаем мониторинг
        monitor.start(device)
        runCurrent()

        // 3. ДЕЙСТВИЕ: Имитируем 3 неудачных цикла сканирования
        repeat(3) {
            isDiscoveringFlow.value = false
            runCurrent()
            advanceTimeBy(1001)  // Интервал перед ретраем
            runCurrent()
        }

        // 4. ПРОВЕРКА: После 3 попыток состояние должно стать DEVICE_UNAVAILABLE
        verify { 
            stateCallback(
                ConnectionState.DEVICE_UNAVAILABLE, 
                any()
            ) 
        }
    }

    /**
     * Тест: должен корректно останавливать процессы и очищать ресурсы.
     */
    @Test
    fun `should stop discovery and cleanup on stop`() = runTest(testDispatcher) {
        // 1. ПОДГОТОВКА: Запускаем мониторинг
        val device = BluetoothDeviceData("Target", "00:11:22:33:44:55")
        monitor.start(device)
        runCurrent()
        
        // 2. ДЕЙСТВИЕ: Останавливаем монитор
        monitor.stop()
        runCurrent()

        // 3. ПРОВЕРКА: Проверяем вызов остановки сканирования в сервисе и статус активности
        verify { bluetoothService.stopDiscovery() }
        assertFalse(monitor.isActive())
    }
}
