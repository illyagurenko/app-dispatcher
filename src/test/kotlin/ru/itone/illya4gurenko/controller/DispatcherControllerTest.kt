package ru.itone.illya4gurenko.controller

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.itone.illya4gurenko.dto.response.ResponseBodyDto
import ru.itone.illya4gurenko.dto.response.ResponseDataDto
import ru.itone.illya4gurenko.dto.response.ResponseDatasourceDto
import ru.itone.illya4gurenko.exception.DispatcherException
import ru.itone.illya4gurenko.service.DispatcherService
import ru.itone.illya4gurenko.service.TransactionService
import java.util.UUID

@WebMvcTest(DispatcherController::class)
@DisplayName("Тестирование DispatcherController")
class DispatcherControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var dispatcherService: DispatcherService

    @MockitoBean
    private lateinit var transactionService: TransactionService

    private val testRequestId = UUID.randomUUID().toString()

    @Test
    @DisplayName("Успешный запрос на /get-datasource-by-account (200 OK и snake_case jdbc_url)")
    fun testGetDatasourceSuccess() {
        val mockResponse = ResponseBodyDto(
            requestId = testRequestId,
            systemId = "APP_DISPATCHER",
            status = "Success",
            error = null,
            data = ResponseDataDto(
                datasource = listOf(
                    ResponseDatasourceDto(
                        jdbcUrl = "jdbc:postgresql://localhost:5432/testdb",
                        account = listOf("ACC123")
                    )
                )
            )
        )

        whenever(dispatcherService.processRequest(anyOrNull(), anyOrNull(), any()))
            .thenReturn(mockResponse)

        val jsonPayload = """
            {
                "data": {
                    "systemId": "APP_CHANGER",
                    "requestId": "$testRequestId",
                    "account": ["ACC123"]
                },
                "status": "Success"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/get-datasource-by-account")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic dXNlcjpwYXNz")
                .content(jsonPayload)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("Success"))
            .andExpect(jsonPath("$.systemId").value("APP_DISPATCHER"))
            .andExpect(jsonPath("$.requestId").value(testRequestId))
            .andExpect(jsonPath("$.data.datasource[0].jdbc_url").value("jdbc:postgresql://localhost:5432/testdb"))
            .andExpect(jsonPath("$.data.datasource[0].account[0]").value("ACC123"))
    }

    @Test
    @DisplayName("Успешный запрос на эндпоинт из ТЗ /get-datasoure-by-account (алиас с опечаткой)")
    fun testGetDatasoureTypoUrlSuccess() {
        val mockResponse = ResponseBodyDto(
            requestId = testRequestId,
            systemId = "APP_DISPATCHER",
            status = "Success",
            error = null,
            data = ResponseDataDto(datasource = emptyList())
        )

        whenever(dispatcherService.processRequest(anyOrNull(), anyOrNull(), any()))
            .thenReturn(mockResponse)

        val jsonPayload = """
            {
                "data": {
                    "systemId": "APP_CHANGER",
                    "requestId": "$testRequestId",
                    "account": ["ACC123"]
                }
            }
        """.trimIndent()

        mockMvc.perform(
            post("/get-datasoure-by-account")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Basic", "Basic dXNlcjpwYXNz")
                .content(jsonPayload)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("Success"))
    }

    @Test
    @DisplayName("Ошибка валидации: пустой список account -> 400 Bad Request")
    fun testValidationEmptyAccounts() {
        val jsonPayload = """
            {
                "data": {
                    "systemId": "APP_CHANGER",
                    "requestId": "$testRequestId",
                    "account": []
                }
            }
        """.trimIndent()

        mockMvc.perform(
            post("/get-datasource-by-account")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value("error"))
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
    }

    @Test
    @DisplayName("Ошибка валидации: невалидный UUID в requestId -> 400 Bad Request")
    fun testValidationInvalidUuid() {
        val jsonPayload = """
            {
                "data": {
                    "systemId": "APP_CHANGER",
                    "requestId": "NOT-A-UUID",
                    "account": ["ACC1"]
                }
            }
        """.trimIndent()

        mockMvc.perform(
            post("/get-datasource-by-account")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value("error"))
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
    }

    @Test
    @DisplayName("Ошибка авторизации: ACCESS_DENIED -> 403 Forbidden")
    fun testAuthDenied() {
        whenever(dispatcherService.processRequest(anyOrNull(), anyOrNull(), any()))
            .thenThrow(DispatcherException(testRequestId, "ACCESS_DENIED", "Access denied", HttpStatus.FORBIDDEN))

        val jsonPayload = """
            {
                "data": {
                    "systemId": "APP_CHANGER",
                    "requestId": "$testRequestId",
                    "account": ["ACC1"]
                }
            }
        """.trimIndent()

        mockMvc.perform(
            post("/get-datasource-by-account")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic bad")
                .content(jsonPayload)
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.status").value("error"))
            .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"))
    }
}
