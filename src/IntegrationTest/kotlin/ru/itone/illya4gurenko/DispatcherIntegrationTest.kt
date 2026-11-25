package ru.itone.illya4gurenko

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.itone.illya4gurenko.entity.AppDispatcherDatasource
import ru.itone.illya4gurenko.entity.AppDispatcherPermission
import ru.itone.illya4gurenko.repository.DatasourceRepository
import ru.itone.illya4gurenko.repository.PermissionRepository
import ru.itone.illya4gurenko.repository.TransRepository
import java.util.Base64
import java.util.UUID

/**
 * сквозные интеграционные тесты диспетчера с реальной бд h2
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("интеграционное тестирование app-dispatcher")
class DispatcherIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var datasourceRepository: DatasourceRepository

    @Autowired
    private lateinit var permissionRepository: PermissionRepository

    @Autowired
    private lateinit var transRepository: TransRepository

    private val passwordEncoder = BCryptPasswordEncoder()

    @BeforeEach
    fun setUp() {
        transRepository.deleteAll()
        datasourceRepository.deleteAll()
        permissionRepository.deleteAll()

        val perm = AppDispatcherPermission().apply {
            login = "APP_CHANGER"
            passBcrypt = passwordEncoder.encode("12345678")
            permission = 1
        }
        permissionRepository.save(perm)

        val datasources = listOf(
            AppDispatcherDatasource().apply {
                account = "40817810000000000001"
                jdbcUrl = "jdbc:postgresql://localhost:5432/changer_db"
                jdbcType = "POSTGRES"
                status = "ACTIVE"
            },
            AppDispatcherDatasource().apply {
                account = "40817810000000000002"
                jdbcUrl = "jdbc:postgresql://localhost:5432/changer_db"
                jdbcType = "POSTGRES"
                status = "ACTIVE"
            },
            AppDispatcherDatasource().apply {
                account = "40817810000000000003"
                jdbcUrl = "jdbc:postgresql://localhost:5433/other_db"
                jdbcType = "POSTGRES"
                status = "ACTIVE"
            },
            AppDispatcherDatasource().apply {
                account = "40817810000000000004"
                jdbcUrl = "jdbc:postgresql://localhost:5432/changer_db"
                jdbcType = "POSTGRES"
                status = "BLOCK"
            }
        )
        datasourceRepository.saveAll(datasources)
    }

    @Test
    @DisplayName("успешный роутинг счетов с группировкой по jdbc_url и сохранением транзакции")
    fun testGetDatasourceByAccountSuccess() {
        val requestId = UUID.randomUUID().toString()
        val authHeader = "Basic " + Base64.getEncoder().encodeToString("APP_CHANGER:12345678".toByteArray())

        val payload = """
            {
              "data": {
                "systemId": "APP_CHANGER",
                "requestId": "$requestId",
                "account": [
                  "40817810000000000001",
                  "40817810000000000002",
                  "40817810000000000003",
                  "40817810000000000004",
                  "40817810999999999999"
                ]
              },
              "status": "Success"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/get-datasource-by-account")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", authHeader)
                .content(payload)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("Success"))
            .andExpect(jsonPath("$.systemId").value("APP_DISPATCHER"))
            .andExpect(jsonPath("$.requestId").value(requestId))
            .andExpect(jsonPath("$.data.datasource[0].jdbc_url").value("jdbc:postgresql://localhost:5432/changer_db"))
            .andExpect(jsonPath("$.data.datasource[0].account.length()").value(2))
            .andExpect(jsonPath("$.data.datasource[1].jdbc_url").value("jdbc:postgresql://localhost:5433/other_db"))
            .andExpect(jsonPath("$.data.datasource[1].account[0]").value("40817810000000000003"))
            .andExpect(jsonPath("$.data.datasource[2].jdbc_url").value(""))
            .andExpect(jsonPath("$.data.datasource[2].account.length()").value(2))

        val savedTrans = transRepository.findById(UUID.fromString(requestId)).orElse(null)
        assertNotNull(savedTrans)
        assertEquals("SUCCESS", savedTrans?.status)
        assertEquals("0", savedTrans?.respCode)
    }

    @Test
    @DisplayName("успешный запрос через алиас /get-datasoure-by-account и заголовок Basic")
    fun testTypoEndpointWithCustomBasicHeader() {
        val requestId = UUID.randomUUID().toString()
        val authHeader = "Basic " + Base64.getEncoder().encodeToString("APP_CHANGER:12345678".toByteArray())

        val payload = """
            {
              "data": {
                "systemId": "APP_CHANGER",
                "requestId": "$requestId",
                "account": ["40817810000000000001"]
              },
              "status": "Success"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/get-datasoure-by-account")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Basic", authHeader)
                .content(payload)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("Success"))
            .andExpect(jsonPath("$.data.datasource[0].jdbc_url").value("jdbc:postgresql://localhost:5432/changer_db"))

        val savedTrans = transRepository.findById(UUID.fromString(requestId)).orElse(null)
        assertNotNull(savedTrans)
        assertEquals("SUCCESS", savedTrans?.status)
    }

    @Test
    @DisplayName("отказ в доступе 403 при неверном пароле и фиксация ошибки в бд")
    fun testInvalidPasswordForbidden() {
        val requestId = UUID.randomUUID().toString()
        val wrongAuthHeader = "Basic " + Base64.getEncoder().encodeToString("APP_CHANGER:wrong_password".toByteArray())

        val payload = """
            {
              "data": {
                "systemId": "APP_CHANGER",
                "requestId": "$requestId",
                "account": ["40817810000000000001"]
              },
              "status": "Success"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/get-datasoure-by-account")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", wrongAuthHeader)
                .content(payload)
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.status").value("error"))
            .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"))

        val savedTrans = transRepository.findById(UUID.fromString(requestId)).orElse(null)
        assertNotNull(savedTrans)
        assertEquals("ERROR", savedTrans?.status)
        assertEquals("ACCESS_DENIED", savedTrans?.respCode)
    }

    @Test
    @DisplayName("ошибка 401 при отсутствии заголовка авторизации")
    fun testMissingAuthHeaderUnauthorized() {
        val requestId = UUID.randomUUID().toString()

        val payload = """
            {
              "data": {
                "systemId": "APP_CHANGER",
                "requestId": "$requestId",
                "account": ["40817810000000000001"]
              },
              "status": "Success"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/get-datasoure-by-account")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value("error"))
            .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"))

        val savedTrans = transRepository.findById(UUID.fromString(requestId)).orElse(null)
        assertNotNull(savedTrans)
        assertEquals("ERROR", savedTrans?.status)
    }

    @Test
    @DisplayName("ошибка валидации 400 при некорректном формате uuid")
    fun testInvalidUuidBadRequest() {
        val authHeader = "Basic " + Base64.getEncoder().encodeToString("APP_CHANGER:12345678".toByteArray())

        val payload = """
            {
              "data": {
                "systemId": "APP_CHANGER",
                "requestId": "not-a-uuid",
                "account": ["40817810000000000001"]
              },
              "status": "Success"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/get-datasoure-by-account")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", authHeader)
                .content(payload)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value("error"))
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
    }
}
