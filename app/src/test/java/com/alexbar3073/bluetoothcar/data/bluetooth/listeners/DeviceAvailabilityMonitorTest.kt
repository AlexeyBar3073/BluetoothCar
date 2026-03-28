package com.alexbar3073.bluetoothcar.data.bluetooth.listeners

import com.alexbar3073.bluetoothcar.MainDispatcherRule
import com.alexbar3073.bluetoothcar.data.bluetooth.AppBluetoothService
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
    private val testDispatcher = StandardTestDispatcher()

    private val onDeviceFoundSlot = slot<(String) -> Unit>()
    private val onDiscoveryFinishedSlot = slot<() -> Unit>()
    private val onDiscoveryErrorSlot = slot<(String) -> Unit>()

    @Before
    fun setup() {
        bluetoothService = mockk(relaxed = true)
        stateCallback = mockk(relaxed = true)
        monitor = DeviceAvailabilityMonitor(bluetoothService, stateCallback)

        every { 
            bluetoothService.startDiscovery(
                capture(onDeviceFoundSlot),
                capture(onDiscoveryFinishedSlot),
                capture(onDiscoveryErrorSlot)
            ) 
        } just runs
    }

    @Test
    fun `should return DEVICE_AVAILABLE when target device is found`() = runTest {
        val device = BluetoothDeviceData("Target", "00:11:22:33:44:55")
        
        monitor.start(device)
        
        if (onDeviceFoundSlot.isCaptured) {
            onDeviceFoundSlot.captured("00:11:22:33:44:55")
            verify { stateCallback(ConnectionState.SEARCHING_DEVICE, null) }
            verify { stateCallback(ConnectionState.DEVICE_AVAILABLE, null) }
            verify { bluetoothService.stopDiscovery() }
        }
    }

    @Test
    fun `should retry discovery when first attempt finishes without finding device`() = runTest {
        val device = BluetoothDeviceData("Target", "00:11:22:33:44:55")
        
        monitor.start(device)
        
        if (onDiscoveryFinishedSlot.isCaptured) {
            onDiscoveryFinishedSlot.captured()
            advanceTimeBy(1100)
            verify(exactly = 2) { bluetoothService.startDiscovery(any(), any(), any()) }
        }
    }

    @Test
    fun `should return DEVICE_UNAVAILABLE after 3 failed attempts`() = runTest {
        val device = BluetoothDeviceData("Target", "00:11:22:33:44:55")
        
        monitor.start(device)
        
        repeat(3) {
            if (onDiscoveryFinishedSlot.isCaptured) {
                onDiscoveryFinishedSlot.captured()
                advanceTimeBy(1100)
            }
        }

        verify { stateCallback(ConnectionState.DEVICE_UNAVAILABLE, null) }
    }

    @Test
    fun `should ignore other devices found in air`() = runTest {
        val device = BluetoothDeviceData("Target", "00:11:22:33:44:55")
        
        monitor.start(device)
        
        if (onDeviceFoundSlot.isCaptured) {
            onDeviceFoundSlot.captured("AA:BB:CC:DD:EE:FF")
            verify(exactly = 0) { stateCallback(ConnectionState.DEVICE_AVAILABLE, any()) }
        }
    }

    @Test
    fun `should return ERROR when discovery fails`() = runTest {
        val device = BluetoothDeviceData("Target", "00:11:22:33:44:55")
        
        monitor.start(device)
        
        if (onDiscoveryErrorSlot.isCaptured) {
            onDiscoveryErrorSlot.captured("Discovery failed")
            verify { stateCallback(ConnectionState.ERROR, "Discovery failed") }
        }
    }
}
