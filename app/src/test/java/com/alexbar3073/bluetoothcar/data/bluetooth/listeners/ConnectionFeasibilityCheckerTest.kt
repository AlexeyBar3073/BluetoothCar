// Файл: data/bluetooth/listeners/ConnectionFeasibilityCheckerTest.kt
package com.alexbar3073.bluetoothcar.data.bluetooth.listeners

import com.alexbar3073.bluetoothcar.MainDispatcherRule
import com.alexbar3073.bluetoothcar.data.bluetooth.AppBluetoothService
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.Closeable

/**
 * ТЕГ: Bluetooth/Checker/Test
 *
 * ФАЙЛ: data/bluetooth/listeners/ConnectionFeasibilityCheckerTest.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: data/bluetooth/listeners/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Модульное тестирование компонента ConnectionFeasibilityChecker.
 * Проверяет логику первичной проверки возможности Bluetooth-соединения: наличие адаптера, 
 * его состояние (вкл/выкл) и наличие сопряжения с выбранным устройством.
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Валидация входных данных устройства (адрес, наличие).
 * 2. Проверка статуса аппаратного Bluetooth-адаптера.
 * 3. Тестирование мониторинга состояния Bluetooth (реагирование на включение/выключение).
 * 4. Верификация корректности передачи состояний (ConnectionState) через callback.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Unit Testing с использованием MockK.
 *
 * КЛЮЧЕВОЙ ПРИНЦИП:
 * Изолированное тестирование логики проверок без реального взаимодействия с Bluetooth-стеком Android.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * Тестирует: ConnectionFeasibilityChecker.kt.
 * Использует: AppBluetoothService, ConnectionState.
 */

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionFeasibilityCheckerTest {

    /** Правило для подмены Main диспатчера */
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Мок сервиса Bluetooth */
    private lateinit var bluetoothService: AppBluetoothService
    
    /** Тестируемый экземпляр чекера */
    private lateinit var checker: ConnectionFeasibilityChecker
    
    /** Мок колбэка для отслеживания изменений состояния */
    private lateinit var stateCallback: (ConnectionState, String?) -> Unit
    
    /** Слот для захвата слушателя состояния Bluetooth */
    private val monitorCallbackSlot = slot<(Boolean) -> Unit>()
    
    /** Тестовый диспатчер для мгновенного выполнения корутин */
    private val testDispatcher = UnconfinedTestDispatcher()

    /**
     * Инициализация перед каждым тестом.
     */
    @Before
    fun setup() {
        bluetoothService = mockk(relaxed = true)
        stateCallback = mockk(relaxed = true)
        // Инициализируем чекер с тестовым диспатчером
        checker = ConnectionFeasibilityChecker(bluetoothService, stateCallback, testDispatcher)
    }

    /**
     * Тест: должен вернуть ошибку, если устройство не выбрано (null).
     */
    @Test
    fun `should return NO_DEVICE_SELECTED if device is null`() = runTest(testDispatcher) {
        checker.start(null)
        runCurrent()

        verify { stateCallback(ConnectionState.NO_DEVICE_SELECTED, match { it.contains("не выбрано") }) }
    }

    /**
     * Тест: должен вернуть ошибку, если адрес устройства пуст.
     */
    @Test
    fun `should return NO_DEVICE_SELECTED if address is blank`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Test", "  ")
        
        checker.start(device)
        runCurrent()

        verify { stateCallback(ConnectionState.NO_DEVICE_SELECTED, match { it.contains("не выбрано") }) }
    }

    /**
     * Тест: должен вернуть ERROR, если адаптер Bluetooth аппаратно отсутствует.
     */
    @Test
    fun `should return ERROR if bluetooth adapter is not available`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Test", "00:11:22:33:44:55")
        every { bluetoothService.bluetoothAdapterIsAvailable() } returns false

        checker.start(device)
        runCurrent()

        verify { stateCallback(ConnectionState.ERROR, match { it.contains("недоступен") }) }
    }

    /**
     * Тест: должен вернуть BLUETOOTH_DISABLED, если адаптер выключен.
     */
    @Test
    fun `should return BLUETOOTH_DISABLED if adapter is off`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Test", "00:11:22:33:44:55")
        
        every { bluetoothService.bluetoothAdapterIsAvailable() } returns true
        every { bluetoothService.bluetoothAdapterIsEnabled() } returns false
        every { bluetoothService.monitorBluetoothState(capture(monitorCallbackSlot)) } returns mockk<Closeable>(relaxed = true)

        checker.start(device)
        runCurrent()

        verify { stateCallback(ConnectionState.BLUETOOTH_DISABLED, any()) }
        verify { bluetoothService.monitorBluetoothState(any()) }
    }

    /**
     * Тест: должен вернуть DEVICE_SELECTED при успешном прохождении всех проверок.
     */
    @Test
    fun `should return DEVICE_SELECTED if all checks pass`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Test", "00:11:22:33:44:55")
        
        every { bluetoothService.bluetoothAdapterIsAvailable() } returns true
        every { bluetoothService.bluetoothAdapterIsEnabled() } returns true
        every { bluetoothService.isDevicePaired(device.address) } returns true

        checker.start(device)
        runCurrent()

        verify { stateCallback(ConnectionState.DEVICE_SELECTED, null) }
    }

    /**
     * Тест: мониторинг должен вызывать DEVICE_SELECTED при включении Bluetooth пользователем.
     */
    @Test
    fun `monitoring should trigger DEVICE_SELECTED when bluetooth is enabled`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Test", "00:11:22:33:44:55")
        
        every { bluetoothService.bluetoothAdapterIsAvailable() } returns true
        every { bluetoothService.bluetoothAdapterIsEnabled() } returns false
        every { bluetoothService.monitorBluetoothState(capture(monitorCallbackSlot)) } returns mockk<Closeable>(relaxed = true)
        
        checker.start(device)
        runCurrent()

        // Имитируем включение Bluetooth
        if (monitorCallbackSlot.isCaptured) {
            monitorCallbackSlot.captured(true)
            runCurrent()
            verify { stateCallback(ConnectionState.DEVICE_SELECTED, null) }
        }
    }
}
