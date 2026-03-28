package com.alexbar3073.bluetoothcar.data.bluetooth.listeners

import app.cash.turbine.test
import com.alexbar3073.bluetoothcar.MainDispatcherRule
import com.alexbar3073.bluetoothcar.data.bluetooth.AppBluetoothService
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DataStreamHandlerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var bluetoothService: AppBluetoothService
    private lateinit var handler: DataStreamHandler
    private lateinit var stateCallback: (ConnectionState, String?) -> Unit
    
    private val incomingDataFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)

    @Before
    fun setup() {
        bluetoothService = mockk(relaxed = true)
        stateCallback = mockk(relaxed = true)
        every { bluetoothService.startDataListening() } returns incomingDataFlow
        // Мы передаем TestScope в конструктор, чтобы использовать его для управления временем
    }

    @Test
    fun `should complete full protocol sequence from settings to listening data`() = runTest {
        handler = DataStreamHandler(bluetoothService, this, stateCallback)
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        val settings = AppSettings()
        
        handler.start(device, settings)
        testScheduler.runCurrent()
        
        verify { stateCallback(ConnectionState.SENDING_SETTINGS, null) }
        coVerify { bluetoothService.sendData(match { it.contains("settings") }) }

        incomingDataFlow.emit("{\"settings\":\"OK\"}")
        testScheduler.advanceTimeBy(1600) 
        testScheduler.runCurrent()

        verify { stateCallback(ConnectionState.REQUESTING_DATA, null) }
        coVerify { bluetoothService.sendData(match { it.contains("GET_DATA") }) }

        incomingDataFlow.emit("{\"GET_DATA\":\"OK\"}")
        testScheduler.advanceTimeBy(1600)
        testScheduler.runCurrent()

        verify { stateCallback(ConnectionState.LISTENING_DATA, null) }
    }

    @Test
    fun `should emit CarData when receiving valid data JSON in listening mode`() = runTest {
        handler = DataStreamHandler(bluetoothService, this, stateCallback)
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        coEvery { bluetoothService.sendData(any()) } returns true
        
        handler.start(device, AppSettings())
        testScheduler.runCurrent()
        
        incomingDataFlow.emit("{\"settings\":\"OK\"}")
        testScheduler.advanceTimeBy(1600)
        testScheduler.runCurrent()
        incomingDataFlow.emit("{\"GET_DATA\":\"OK\"}")
        testScheduler.advanceTimeBy(1600)
        testScheduler.runCurrent()

        handler.carDataFlow.test {
            val jsonResponse = "{\"data\":{\"speed\":85.5,\"voltage\":13.8,\"fuel\":42.0}}"
            incomingDataFlow.emit(jsonResponse)
            
            val result = awaitItem()
            assert(result.speed == 85.5f)
            assert(result.voltage == 13.8f)
            assert(result.fuel == 42.0f)
        }
    }

    @Test
    fun `should return ERROR if settings acknowledgment timeout occurs`() = runTest {
        handler = DataStreamHandler(bluetoothService, this, stateCallback)
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        
        handler.start(device, AppSettings())
        testScheduler.runCurrent()

        testScheduler.advanceTimeBy(11000)
        testScheduler.runCurrent()

        verify { stateCallback(ConnectionState.ERROR, match { it.contains("Таймаут") }) }
    }

    @Test
    fun `should wait for subscription before sending settings`() = runTest {
        handler = DataStreamHandler(bluetoothService, this, stateCallback)
        every { bluetoothService.startDataListening() } answers {
            incomingDataFlow
        }

        handler.start(BluetoothDeviceData("Car", "00:11:22:33:44:55"), AppSettings())
        
        coVerify(exactly = 0) { bluetoothService.sendData(any()) }
        
        testScheduler.runCurrent()
        coVerify(atLeast = 1) { bluetoothService.sendData(match { it.contains("settings") }) }
    }
}
