package com.alexbar3073.bluetoothcar.data.bluetooth.listeners

import com.alexbar3073.bluetoothcar.MainDispatcherRule
import com.alexbar3073.bluetoothcar.data.bluetooth.AppBluetoothService
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceAvailabilityMonitorTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var bluetoothService: AppBluetoothService
    private lateinit var monitor: DeviceAvailabilityMonitor
    private lateinit var stateCallback: (ConnectionState, String?) -> Unit
    
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        bluetoothService = mockk(relaxed = true)
        stateCallback = mockk(relaxed = true)
        // Передаем тестовый диспетчер в конструктор
        monitor = DeviceAvailabilityMonitor(bluetoothService, stateCallback, testDispatcher)
    }

    @Test
    fun `should return DEVICE_AVAILABLE when device is found`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Target", "00:11:22:33:44:55")
        val foundAddressSlot = slot<(String) -> Unit>()

        every { 
            bluetoothService.startDiscovery(capture(foundAddressSlot), any(), any()) 
        } just runs

        monitor.start(device)
        runCurrent()

        verify { stateCallback(ConnectionState.SEARCHING_DEVICE, null) }

        if (foundAddressSlot.isCaptured) {
            foundAddressSlot.captured("00:11:22:33:44:55")
            runCurrent()
            verify { stateCallback(ConnectionState.DEVICE_AVAILABLE, null) }
            verify { bluetoothService.stopDiscovery() }
        }
    }

    @Test
    fun `should retry discovery when first attempt finishes without finding device`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Target", "00:11:22:33:44:55")
        val onFinishedSlot = slot<() -> Unit>()

        every { 
            bluetoothService.startDiscovery(any(), capture(onFinishedSlot), any()) 
        } just runs

        monitor.start(device)
        runCurrent()

        // Завершаем первую попытку
        if (onFinishedSlot.isCaptured) {
            onFinishedSlot.captured()
            
            // Ждем интервал ретрая (1000мс)
            advanceTimeBy(1100)
            runCurrent()

            // Должна быть вызвана вторая попытка поиска
            verify(exactly = 2) { bluetoothService.startDiscovery(any(), any(), any()) }
        }
    }

    @Test
    fun `should return DEVICE_UNAVAILABLE after 3 failed attempts`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Target", "00:11:22:33:44:55")
        val onFinishedSlot = mutableListOf<() -> Unit>()

        every { 
            bluetoothService.startDiscovery(any(), capture(onFinishedSlot), any()) 
        } just runs

        monitor.start(device)
        runCurrent()

        // Имитируем 3 неудачных завершения поиска
        repeat(3) {
            onFinishedSlot.last().invoke()
            advanceTimeBy(1100)
            runCurrent()
        }

        verify { stateCallback(ConnectionState.DEVICE_UNAVAILABLE, null) }
    }

    @Test
    fun `should stop discovery and cleanup on stop`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Target", "00:11:22:33:44:55")
        
        monitor.start(device)
        runCurrent()
        
        monitor.stop()
        runCurrent()

        verify { bluetoothService.stopDiscovery() }
        assert(!monitor.isActive())
    }
}
