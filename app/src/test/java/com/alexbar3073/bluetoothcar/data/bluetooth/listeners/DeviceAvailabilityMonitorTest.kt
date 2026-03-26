// Файл: app/src/test/java/com/alexbar3073/bluetoothcar/data/bluetooth/listeners/DeviceAvailabilityMonitorTest.kt
package com.alexbar3073.bluetoothcar.data.bluetooth.listeners

import com.alexbar3073.bluetoothcar.data.bluetooth.BluetoothService
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * ТЕГ: Тесты монитора доступности устройства
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Модульные тесты для DeviceAvailabilityMonitor. Проверяют логику поиска устройства
 * в эфире, обработку попыток (до 3-х) и таймауты.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Тестирует: DeviceAvailabilityMonitor.kt
 * 2. Мокает: BluetoothService.kt
 * 3. Использует: ConnectionState, BluetoothDeviceData
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeviceAvailabilityMonitorTest {

    private lateinit var bluetoothService: BluetoothService
    private lateinit var monitor: DeviceAvailabilityMonitor
    private lateinit var stateCallback: (ConnectionState, String?) -> Unit
    private val testDispatcher = StandardTestDispatcher()

    /** Слот для захвата callback-функции обнаружения устройства */
    private val onDeviceFoundSlot = slot<(String) -> Unit>()
    /** Слот для захвата callback-функции завершения системного поиска */
    private val onDiscoveryFinishedSlot = slot<() -> Unit>()

    @Before
    fun setup() {
        bluetoothService = mockk(relaxed = true)
        stateCallback = mockk(relaxed = true)
        monitor = DeviceAvailabilityMonitor(bluetoothService, stateCallback)

        // Захватываем колбеки при вызове startDiscovery
        every { 
            bluetoothService.startDiscovery(
                capture(onDeviceFoundSlot),
                capture(onDiscoveryFinishedSlot),
                any()
            ) 
        } answers { /* имитация запуска */ }
    }

    /**
     * Тест: Успешное обнаружение устройства.
     */
    @Test
    fun `should return DEVICE_AVAILABLE when target device is found`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Target", "00:11:22:33:44:55")
        
        monitor.start(device)
        
        // Эмулируем нахождение устройства сервисом
        onDeviceFoundSlot.captured("00:11:22:33:44:55")

        verify { stateCallback(ConnectionState.DEVICE_AVAILABLE, null) }
        verify { bluetoothService.stopDiscovery() }
    }

    /**
     * Тест: Устройство не найдено после первой попытки (должен быть перезапуск).
     */
    @Test
    fun `should retry discovery when first attempt finishes without finding device`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Target", "00:11:22:33:44:55")
        
        monitor.start(device)
        
        // Завершаем первую попытку (ничего не найдено)
        onDiscoveryFinishedSlot.captured()
        
        // Ждем интервал между попытками (1000мс)
        advanceTimeBy(1100)
        
        // Проверяем, что startDiscovery был вызван повторно (всего 2 раза)
        verify(exactly = 2) { bluetoothService.startDiscovery(any(), any(), any()) }
    }

    /**
     * Тест: Статус DEVICE_UNAVAILABLE после 3-х неудачных попыток.
     */
    @Test
    fun `should return DEVICE_UNAVAILABLE after 3 failed attempts`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Target", "00:11:22:33:44:55")
        
        monitor.start(device)
        
        // 1-я попытка завершена
        onDiscoveryFinishedSlot.captured()
        advanceTimeBy(1100)
        
        // 2-я попытка завершена
        onDiscoveryFinishedSlot.captured()
        advanceTimeBy(1100)
        
        // 3-я попытка завершена
        onDiscoveryFinishedSlot.captured()

        verify { stateCallback(ConnectionState.DEVICE_UNAVAILABLE, null) }
    }

    /**
     * Тест: Игнорирование устройств с другими MAC-адресами.
     */
    @Test
    fun `should ignore other devices found in air`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Target", "00:11:22:33:44:55")
        
        monitor.start(device)
        
        // Найдено другое устройство
        onDeviceFoundSlot.captured("AA:BB:CC:DD:EE:FF")

        // Состояние не должно измениться на AVAILABLE
        verify(exactly = 0) { stateCallback(ConnectionState.DEVICE_AVAILABLE, any()) }
    }
}
