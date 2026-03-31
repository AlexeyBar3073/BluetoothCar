// Файл: data/bluetooth/ConnectionStateTest.kt
package com.alexbar3073.bluetoothcar.data.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ТЕГ: Bluetooth/ConnectionState/Test
 *
 * ФАЙЛ: data/bluetooth/ConnectionStateTest.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: data/bluetooth/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Модульное тестирование перечисления ConnectionState и вспомогательной структуры ConnectionStatusInfo.
 * Проверяет корректность логических свойств состояний (активность, терминальность, возможность повтора)
 * и правильность преобразования перечисления в объект информации для UI.
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Проверка метода isActiveState(): определение состояний с установленным соединением.
 * 2. Тестирование маппинга в ConnectionStatusInfo (метод toStatusInfo()).
 * 3. Верификация терминальных состояний (isTerminalState()).
 * 4. Проверка условий доступности ручного перезапуска (allowsManualRetry).
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Unit Testing (Pure Kotlin).
 *
 * КЛЮЧЕВОЙ ПРИНЦИП:
 * Гарантия того, что бизнес-логика переходов состояний и их отображение в интерфейсе
 * базируются на корректно определенных свойствах перечисления.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * Тестирует: ConnectionState.kt.
 * Взаимодействует: Используется в UI-компонентах и ViewModel для принятия решений о показе данных.
 */

class ConnectionStateTest {

    /**
     * Тест: Проверка метода isActiveState().
     * Активными считаются состояния, где соединение установлено и идет (или готово к) обмену данными.
     */
    @Test
    fun `isActiveState should return true only for active states`() {
        // 1. ПРОВЕРКА АКТИВНЫХ СОСТОЯНИЙ
        assertTrue("CONNECTED должен быть активным", ConnectionState.CONNECTED.isActiveState())
        assertTrue("LISTENING_DATA должен быть активным", ConnectionState.LISTENING_DATA.isActiveState())
        assertTrue("SENDING_SETTINGS должен быть активным", ConnectionState.SENDING_SETTINGS.isActiveState())
        assertTrue("REQUESTING_DATA должен быть активным", ConnectionState.REQUESTING_DATA.isActiveState())
        
        // 2. ПРОВЕРКА НЕАКТИВНЫХ СОСТОЯНИЙ
        assertFalse("CONNECTING не является активным (еще не подключено)", ConnectionState.CONNECTING.isActiveState())
        assertFalse("DISCONNECTED не является активным", ConnectionState.DISCONNECTED.isActiveState())
        assertFalse("ERROR не является активным", ConnectionState.ERROR.isActiveState())
        assertFalse("UNDEFINED не является активным", ConnectionState.UNDEFINED.isActiveState())
    }

    /**
     * Тест: Проверка конвертации в полную структуру для UI (ConnectionStatusInfo).
     */
    @Test
    fun `toStatusInfo should map all properties correctly`() {
        // 1. ПОДГОТОВКА
        val state = ConnectionState.LISTENING_DATA
        
        // 2. ДЕЙСТВИЕ: конвертируем в info-объект
        val info = state.toStatusInfo()
        
        // 3. ПРОВЕРКА МАППИНГА ПОЛЕЙ
        assertEquals("State enum должен совпадать", state, info.state)
        assertEquals("Отображаемое имя должно быть корректным", "Соединение установлено", info.displayName)
        assertTrue("LISTENING_DATA должен иметь флаг isActive=true", info.isActive)
        assertFalse("LISTENING_DATA не является ошибкой", info.isError)
        assertFalse("LISTENING_DATA не является процессом подключения", info.isConnecting)
    }

    /**
     * Тест: Проверка идентификации терминальных состояний.
     * Терминальные состояния требуют вмешательства или перезапуска процесса.
     */
    @Test
    fun `terminal states should be identified correctly`() {
        // 1. ПРОВЕРКА СОСТОЯНИЙ, ТРЕБУЮЩИХ ВМЕШАТЕЛЬСТВА
        assertTrue("ERROR - терминальное состояние", ConnectionState.ERROR.isTerminalState())
        assertTrue("DISCONNECTED - терминальное состояние", ConnectionState.DISCONNECTED.isTerminalState())
        assertTrue("DEVICE_UNAVAILABLE - терминальное состояние", ConnectionState.DEVICE_UNAVAILABLE.isTerminalState())
        
        // 2. ПРОВЕРКА ПРОМЕЖУТОЧНЫХ СОСТОЯНИЙ
        assertFalse("CONNECTED не терминальное", ConnectionState.CONNECTED.isTerminalState())
        assertFalse("CONNECTING не терминальное", ConnectionState.CONNECTING.isTerminalState())
    }

    /**
     * Тест: Проверка флага возможности ручного переподключения.
     * Согласно бизнес-логике, только определенные состояния ошибок позволяют ручной повтор пользователем.
     */
    @Test
    fun `manual retry should be allowed only for specific states`() {
        // 1. ПРОВЕРКА РАЗРЕШЕННЫХ ДЛЯ ПОВТОРА СОСТОЯНИЙ
        assertTrue("ERROR должен позволять ручной повтор", ConnectionState.ERROR.allowsManualRetry)
        assertTrue("DEVICE_UNAVAILABLE должен позволять ручной повтор", ConnectionState.DEVICE_UNAVAILABLE.allowsManualRetry)
        
        // 2. ПРОВЕРКА ЗАПРЕЩЕННЫХ СОСТОЯНИЙ
        assertFalse("CONNECTING не должен позволять ручной повтор", ConnectionState.CONNECTING.allowsManualRetry)
        assertFalse("LISTENING_DATA не должен позволять ручной повтор", ConnectionState.LISTENING_DATA.allowsManualRetry)
    }
}
