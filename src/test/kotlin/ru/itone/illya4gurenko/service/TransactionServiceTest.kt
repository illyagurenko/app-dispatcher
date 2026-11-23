package ru.itone.illya4gurenko.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import ru.itone.illya4gurenko.entity.AppDispatcherTrans
import ru.itone.illya4gurenko.exception.DispatcherException
import ru.itone.illya4gurenko.repository.TransRepository
import tools.jackson.databind.ObjectMapper
import java.util.Optional
import java.util.UUID

@DisplayName("Тестирование TransactionService")
class TransactionServiceTest {

    private val transRepository = mock<TransRepository>()
    private val objectMapper = ObjectMapper()
    private lateinit var transactionService: TransactionService

    private val testRequestId = UUID.randomUUID().toString()
    private val testUuid = UUID.fromString(testRequestId)

    @BeforeEach
    fun setUp() {
        transactionService = TransactionService(transRepository, objectMapper)
    }

    @Test
    @DisplayName("Успешное создание начальной транзакции (Шаг 4 ТЗ)")
    fun testCreateTransactionSuccess() {
        whenever(transRepository.existsById(testUuid)).thenReturn(false)
        whenever(transRepository.save(any<AppDispatcherTrans>())).thenAnswer { it.arguments[0] }

        val trans = transactionService.createTransaction(testRequestId, "APP_CHANGER", mapOf("data" to "test"))

        assertNotNull(trans)
        assertEquals(testUuid, trans.id)
        assertEquals("REST", trans.protocolType)
        assertEquals("APP_CHANGER", trans.systemId)
        assertEquals("PROCESSING", trans.status)
        assertTrue(trans.reqData!!.contains("test"))
    }

    @Test
    @DisplayName("Ошибка: дубликат requestId при создании транзакции")
    fun testCreateTransactionDuplicateRequestId() {
        whenever(transRepository.existsById(testUuid)).thenReturn(true)

        val ex = assertThrows(DispatcherException::class.java) {
            transactionService.createTransaction(testRequestId, "APP_CHANGER", null)
        }
        assertEquals(HttpStatus.CONFLICT, ex.httpStatus)
        assertEquals("DUPLICATE_REQUEST_ID", ex.errorCode)
    }

    @Test
    @DisplayName("Успешная фиксация ответа (Шаг 8 ТЗ)")
    fun testRecordSuccess() {
        val existingTrans = AppDispatcherTrans().apply {
            id = testUuid
            status = "PROCESSING"
        }
        whenever(transRepository.findById(testUuid)).thenReturn(Optional.of(existingTrans))
        whenever(transRepository.save(any<AppDispatcherTrans>())).thenAnswer { it.arguments[0] }

        val result = transactionService.recordSuccess(testRequestId, mapOf("status" to "Success"))

        assertNotNull(result)
        assertEquals("SUCCESS", result.status)
        assertEquals("0", result.respCode)
        assertEquals("SUCCESS", result.respDesc)
        assertTrue(result.respData!!.contains("Success"))
    }

    @Test
    @DisplayName("Фиксация ошибки в транзакционной таблице (Шаг 5.1 ТЗ)")
    fun testRecordError() {
        val existingTrans = AppDispatcherTrans().apply {
            id = testUuid
            status = "PROCESSING"
        }
        whenever(transRepository.findById(testUuid)).thenReturn(Optional.of(existingTrans))
        whenever(transRepository.save(any<AppDispatcherTrans>())).thenAnswer { it.arguments[0] }

        val result = transactionService.recordError(
            requestId = testRequestId,
            errorCode = "ACCESS_DENIED",
            errorDesc = "Invalid password",
            errorBody = mapOf("error" to "denied")
        )

        assertNotNull(result)
        assertEquals("ERROR", result?.status)
        assertEquals("ACCESS_DENIED", result?.respCode)
        assertEquals("Invalid password", result?.respDesc)
    }
}
