// Файл: app/src/test/java/com/alexbar3073/bluetoothcar/ui/viewmodels/SharedViewModelTest.kt
package com.alexbar3073.bluetoothcar.ui.viewmodels

import app.cash.turbine.test
import com.alexbar3073.bluetoothcar.core.AppController
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import com.alexbar3073.bluetoothcar.data.models.CarData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * ТЕГ: Тесты SharedViewModel
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Модульные тесты для SharedViewModel. Проверяют корректность передачи данных 
 * от AppController в UI и правильность делегирования команд.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Тестирует: SharedViewModel.kt
 * 2. Мокает: AppController.kt
 * 3. Использует модели: ConnectionStatusInfo, AppSettings, CarData
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SharedViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var appController: AppController
    private lateinit var viewModel: SharedViewModel

    /** Потоки-заглушки для имитации данных от контроллера */
    private val connectionStatusFlow = MutableStateFlow(ConnectionState.UNDEFINED.toStatusInfo())
    private val carDataFlow = MutableStateFlow(CarData())
    private val appSettingsFlow = MutableStateFlow(AppSettings())

    /**
     * Настройка окружения перед каждым тестом.
     * Заменяет Main диспатчер на тестовый и настраивает моки.
     */
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        appController = mockk(relaxed = true)

        // Настройка возвращаемых значений для потоков контроллера
        every { appController.connectionStatusInfo } returns connectionStatusFlow
        every { appController.carData } returns carDataFlow
        every { appController.appSettings } returns appSettingsFlow

        viewModel = SharedViewModel(appController)
    }

    /**
     * Сброс диспатчера после завершения тестов.
     */
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Тест: Проверка реактивного обновления статуса подключения.
     * Проверяет, что изменения в AppController корректно доходят до ViewModel.
     */
    @Test
    fun `viewModel should reflect connection status from controller`() = runTest {
        val expectedStatus = ConnectionState.CONNECTED.toStatusInfo()
        
        viewModel.connectionStatusInfo.test {
            // Первое значение при подписке
            assertEquals(ConnectionState.UNDEFINED.toStatusInfo(), awaitItem())
            
            // Эмулируем изменение в контроллере
            connectionStatusFlow.value = expectedStatus
            assertEquals(expectedStatus, awaitItem())
        }
    }

    /**
     * Тест: Проверка делегирования команды повторного подключения.
     */
    @Test
    fun `retryConnection should delegate to appController`() {
        viewModel.retryConnection()
        verify { appController.retryConnection() }
    }

    /**
     * Тест: Проверка логики выбора устройства.
     * Проверяет сохранение существующих настроек при обновлении устройства.
     */
    @Test
    fun `selectBluetoothDevice should update settings in appController`() {
        val device = BluetoothDeviceData("Test Device", "00:11:22:33:44:55")
        val currentSettings = AppSettings(fuelTankCapacity = 50f)
        every { appController.getCurrentSettings() } returns currentSettings

        viewModel.selectBluetoothDevice(device)

        // Проверяем, что в контроллер ушли обновленные настройки с сохранением объема бака
        verify { 
            appController.updateSettings(match { 
                it.selectedDevice == device && it.fuelTankCapacity == 50f 
            }) 
        }
    }

    /**
     * Тест: Проверка производного состояния isConnected.
     * Проверяет маппинг физического состояния в булево значение для UI.
     */
    @Test
    fun `isConnected should be true when state is active`() = runTest {
        viewModel.isConnected.test {
            assertEquals(false, awaitItem()) // Начальное UNDEFINED не является активным

            connectionStatusFlow.value = ConnectionState.LISTENING_DATA.toStatusInfo()
            assertEquals(true, awaitItem())

            connectionStatusFlow.value = ConnectionState.DISCONNECTED.toStatusInfo()
            assertEquals(false, awaitItem())
        }
    }
}
