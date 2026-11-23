package ru.itone.illya4gurenko.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import ru.itone.illya4gurenko.dto.request.RequestBodyDto
import ru.itone.illya4gurenko.dto.request.RequestDataDto
import ru.itone.illya4gurenko.entity.AppDispatcherDatasource
import ru.itone.illya4gurenko.entity.AppDispatcherPermission
import ru.itone.illya4gurenko.exception.DispatcherException
import ru.itone.illya4gurenko.repository.DatasourceRepository
import java.util.UUID

@DisplayName("Тестирование DispatcherService")
class DispatcherServiceTest {

    private val authService = mock<AuthService>()
    private val transactionService = mock<TransactionService>()
    private val datasourceRepository = mock<DatasourceRepository>()

    private lateinit var dispatcherService: DispatcherService

    private val testRequestId = UUID.randomUUID().toString()

    @BeforeEach
    fun setUp() {
        dispatcherService = DispatcherService(authService, transactionService, datasourceRepository)
    }

    @Test
    @DisplayName("Успешная обработка запроса с группировкой по jdbc_url и пустым URL для ненайденных счетов")
    fun testProcessRequestSuccess() {
        val request = RequestBodyDto(
            data = RequestDataDto(
                systemId = "APP_CHANGER",
                requestId = testRequestId,
                account = listOf("ACC1", "ACC2", "ACC3", "ACC_MISSING")
            ),
            status = "Success",
            error = null
        )

        val ds1 = AppDispatcherDatasource().apply {
            account = "ACC1"
            jdbcUrl = "jdbc:postgresql://db1:5432/main"
            status = "ACTIVE"
        }
        val ds2 = AppDispatcherDatasource().apply {
            account = "ACC2"
            jdbcUrl = "jdbc:postgresql://db1:5432/main"
            status = "ACTIVE"
        }
        val ds3 = AppDispatcherDatasource().apply {
            account = "ACC3"
            jdbcUrl = "jdbc:postgresql://db2:5432/second"
            status = "ACTIVE"
        }

        whenever(datasourceRepository.findAllByAccountInAndStatus(setOf("ACC1", "ACC2", "ACC3", "ACC_MISSING"), "ACTIVE"))
            .thenReturn(listOf(ds1, ds2, ds3))

        val response = dispatcherService.processRequest("Basic dGVzdA==", null, request)

        assertNotNull(response)
        assertEquals(testRequestId, response.requestId)
        assertEquals("APP_DISPATCHER", response.systemId)
        assertEquals("Success", response.status)
        assertNull(response.error)
        assertNotNull(response.data)

        val datasources = response.data!!.datasource
        assertEquals(3, datasources.size)

        // Группа 1: db1 -> ACC1, ACC2
        val group1 = datasources.find { it.jdbcUrl == "jdbc:postgresql://db1:5432/main" }
        assertNotNull(group1)
        assertEquals(listOf("ACC1", "ACC2"), group1!!.account)

        // Группа 2: db2 -> ACC3
        val group2 = datasources.find { it.jdbcUrl == "jdbc:postgresql://db2:5432/second" }
        assertNotNull(group2)
        assertEquals(listOf("ACC3"), group2!!.account)

        // Группа 3: пустой jdbcUrl для ACC_MISSING (согласно ТЗ)
        val missingGroup = datasources.find { it.jdbcUrl == "" }
        assertNotNull(missingGroup)
        assertEquals(listOf("ACC_MISSING"), missingGroup!!.account)

        // Проверяем, что была создана транзакция и зафиксирован успех
        verify(transactionService).createTransaction(testRequestId, "APP_CHANGER", request)
        verify(transactionService).recordSuccess(testRequestId, response)
    }

    @Test
    @DisplayName("Ошибка авторизации: фиксируется ошибка в транзакции и пробрасывается DispatcherException")
    fun testProcessRequestAuthFailure() {
        val request = RequestBodyDto(
            data = RequestDataDto(
                systemId = "APP_CHANGER",
                requestId = testRequestId,
                account = listOf("ACC1")
            ),
            status = "Success",
            error = null
        )

        whenever(authService.authenticate(anyOrNull(), anyOrNull(), anyOrNull()))
            .thenThrow(DispatcherException(testRequestId, "ACCESS_DENIED", "Access denied", HttpStatus.FORBIDDEN))

        val ex = assertThrows(DispatcherException::class.java) {
            dispatcherService.processRequest("Basic bad", null, request)
        }

        assertEquals("ACCESS_DENIED", ex.errorCode)
        verify(transactionService).createTransaction(testRequestId, "APP_CHANGER", request)
        verify(transactionService).recordError(
            eq(testRequestId),
            eq("ACCESS_DENIED"),
            eq("Access denied"),
            any(),
            eq("APP_CHANGER"),
            eq(request)
        )
    }
}
