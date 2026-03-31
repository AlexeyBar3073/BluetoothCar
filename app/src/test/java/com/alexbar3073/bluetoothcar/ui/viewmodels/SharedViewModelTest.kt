// Файл: ui/viewmodels/SharedViewModelTest.kt
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
 * ТЕГ: UI/ViewModel/SharedTest
 *
 * ФАЙЛ: ui/viewmodels/SharedViewModelTest.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: ui/viewmodels/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Модульное тестирование SharedViewModel — основного связующего звена между UI и бизнес-логикой.
 * Проверяет корректность трансляции данных из AppController в потоки состояний для Compose-экранов.
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Проверка реактивного обновления данных автомобиля (CarData).
 * 2. Тестирование логики определения активного соединения (isConnected).
 * 3. Верификация проброса команд на изменение настроек (выбор Bluetooth устройства) в AppController.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Unit Testing с использованием MockK и Turbine.
 *
 * КЛЮЧЕВОЙ ПРИНЦИП:
 * ViewModel должна выступать чистым посредником, правильно интерпретируя состояния контроллера
 * и предоставляя их в удобном для UI виде.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * Тестирует: SharedViewModel.kt.
 * Взаимодействует: AppController.kt.
 */

@OptIn(ExperimentalCoroutinesApi::class)
class SharedViewModelTest {

    /** Тестовый диспатчер для управления временем выполнения корутин */
    private val testDispatcher = StandardTestDispatcher()
    
    /** Мок главного контроллера приложения */
    private lateinit var appController: AppController
    
    /** Тестируемый экземпляр вьюмодели */
    private lateinit var viewModel: SharedViewModel

    /** Имитация реактивных потоков данных из AppController */
    private val connectionStatusFlow = MutableStateFlow(ConnectionState.UNDEFINED.toStatusInfo())
    private val carDataFlow = MutableStateFlow(CarData())
    private val appSettingsFlow = MutableStateFlow(AppSettings())

    /**
     * Настройка окружения перед каждым тестом.
     */
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        appController = mockk(relaxed = true)

        // Настраиваем мок AppController на возврат наших тестовых потоков
        every { appController.connectionStatusInfo } returns connectionStatusFlow
        every { appController.carData } returns carDataFlow
        every { appController.appSettings } returns appSettingsFlow

        viewModel = SharedViewModel(appController)
    }

    /**
     * Очистка после завершения тестов.
     */
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Тест: ViewModel должна корректно отражать данные автомобиля, игнорируя метку времени.
     */
    @Test
    fun `viewModel should reflect car data from controller ignoring timestamp`() = runTest {
        val testCarData = CarData(speed = 60f, fuel = 25f)
        
        viewModel.carData.test {
            // 1. ПЕРВИЧНОЕ СОСТОЯНИЕ: Пропускаем начальное значение
            awaitItem() 
            
            // 2. ДЕЙСТВИЕ: Эмитируем новые данные от автомобиля через поток контроллера
            carDataFlow.value = testCarData
            
            // 3. ПРОВЕРКА: ViewModel должна транслировать эти данные в свой поток
            val received = awaitItem()
            assertEquals(testCarData.speed, received.speed)
            assertEquals(testCarData.fuel, received.fuel)
        }
    }

    /**
     * Тест: Флаг isConnected должен быть true только при активном состоянии соединения.
     */
    @Test
    fun `isConnected should be true when state is active`() = runTest {
        viewModel.isConnected.test {
            // 1. ПРОВЕРКА: Изначально соединение не установлено
            assertEquals(false, awaitItem())

            // 2. ДЕЙСТВИЕ: Устанавливаем активное состояние (передача данных)
            connectionStatusFlow.value = ConnectionState.LISTENING_DATA.toStatusInfo()
            
            // 3. ПРОВЕРКА: Флаг должен стать true
            assertEquals(true, awaitItem())
        }
    }

    /**
     * Тест: Выбор Bluetooth-устройства должен инициировать обновление настроек в контроллере.
     */
    @Test
    fun `selectBluetoothDevice should update settings in appController`() {
        // 1. ПОДГОТОВКА ДАННЫХ
        val device = BluetoothDeviceData("Test Device", "00:11:22:33:44:55")
        val currentSettings = AppSettings(fuelTankCapacity = 50f)
        every { appController.getCurrentSettings() } returns currentSettings

        // 2. ДЕЙСТВИЕ: Выбираем устройство в ViewModel
        viewModel.selectBluetoothDevice(device)

        // 3. ПРОВЕРКА: Вызов должен быть проброшен в AppController с сохранением текущих параметров БК
        verify { 
            appController.updateSettings(match { 
                it.selectedDevice == device && it.fuelTankCapacity == 50f 
            }) 
        }
    }
}
