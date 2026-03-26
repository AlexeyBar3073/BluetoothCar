// Файл: app/src/test/java/com/alexbar3073/bluetoothcar/data/bluetooth/listeners/ConnectionFeasibilityCheckerTest.kt
package com.alexbar3073.bluetoothcar.data.bluetooth.listeners

import com.alexbar3073.bluetoothcar.data.bluetooth.BluetoothService
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * ТЕГ: Тесты проверщика возможности подключения
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Модульные тесты для ConnectionFeasibilityChecker. Проверяют цепочку проверок
 * перед началом подключения (наличие адаптера, включен ли BT, сопряжено ли устройство).
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Тестирует: ConnectionFeasibilityChecker.kt
 * 2. Мокает: BluetoothService.kt
 * 3. Использует: ConnectionState, BluetoothDeviceData
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionFeasibilityCheckerTest {

    private lateinit var bluetoothService: BluetoothService
    private lateinit var checker: ConnectionFeasibilityChecker
    private lateinit var stateCallback: (ConnectionState, String?) -> Unit
    private val testDispatcher = StandardTestDispatcher()

    /**
     * Настройка перед каждым тестом.
     */
    @Before
    fun setup() {
        bluetoothService = mockk(relaxed = true)
        stateCallback = mockk(relaxed = true)
        checker = ConnectionFeasibilityChecker(bluetoothService, stateCallback)
    }

    /**
     * Тест: Ошибка, если устройство не выбрано (null).
     */
    @Test
    fun `should return NO_DEVICE_SELECTED if device is null`() = runTest(testDispatcher) {
        checker.start(null)
        advanceUntilIdle()

        verify { stateCallback(ConnectionState.NO_DEVICE_SELECTED, any()) }
    }

    /**
     * Тест: Ошибка, если Bluetooth адаптер отсутствует на устройстве.
     */
    @Test
    fun `should return ERROR if bluetooth adapter is not available`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Test", "00:11:22:33:44:55")
        every { bluetoothService.bluetoothAdapterIsAvailable() } returns false

        checker.start(device)
        advanceUntilIdle()

        verify { stateCallback(ConnectionState.ERROR, match { it.contains("недоступен") }) }
    }

    /**
     * Тест: Статус BLUETOOTH_DISABLED, если адаптер выключен.
     */
    @Test
    fun `should return BLUETOOTH_DISABLED if adapter is off`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Test", "00:11:22:33:44:55")
        every { bluetoothService.bluetoothAdapterIsAvailable() } returns true
        every { bluetoothService.bluetoothAdapterIsEnabled() } returns false

        checker.start(device)
        advanceUntilIdle()

        verify { stateCallback(ConnectionState.BLUETOOTH_DISABLED, any()) }
    }

    /**
     * Тест: Успешный проход всех проверок.
     */
    @Test
    fun `should return DEVICE_SELECTED if all checks pass`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Test", "00:11:22:33:44:55")
        every { bluetoothService.bluetoothAdapterIsAvailable() } returns true
        every { bluetoothService.bluetoothAdapterIsEnabled() } returns true
        every { bluetoothService.isDevicePaired(device.address) } returns true

        checker.start(device)
        advanceUntilIdle()

        verify { stateCallback(ConnectionState.DEVICE_SELECTED, null) }
    }

    /**
     * Тест: Ошибка, если устройство не сопряжено в системе.
     */
    @Test
    fun `should return NO_DEVICE_SELECTED if device is not paired`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Test", "00:11:22:33:44:55")
        every { bluetoothService.bluetoothAdapterIsAvailable() } returns true
        every { bluetoothService.bluetoothAdapterIsEnabled() } returns true
        every { bluetoothService.isDevicePaired(device.address) } returns false

        checker.start(device)
        advanceUntilIdle()

        verify { stateCallback(ConnectionState.NO_DEVICE_SELECTED, match { it.contains("сопряжено") }) }
    }
}
