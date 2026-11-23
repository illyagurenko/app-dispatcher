package ru.itone.illya4gurenko.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import ru.itone.illya4gurenko.entity.AppDispatcherPermission
import ru.itone.illya4gurenko.exception.DispatcherException
import ru.itone.illya4gurenko.repository.PermissionRepository
import java.util.Base64

@DisplayName("Тестирование AuthService")
class AuthServiceTest {

    private val permissionRepository = Mockito.mock(PermissionRepository::class.java)
    private val passwordEncoder = BCryptPasswordEncoder()
    private lateinit var authService: AuthService

    private val testLogin = "APP_CHANGER"
    private val testPassword = "SecretPassword123"
    private val hashedPass = passwordEncoder.encode(testPassword)

    @BeforeEach
    fun setUp() {
        authService = AuthService(permissionRepository, passwordEncoder)
    }

    private fun buildHeader(login: String, pass: String): String {
        val token = Base64.getEncoder().encodeToString("$login:$pass".toByteArray())
        return "Basic $token"
    }

    @Test
    @DisplayName("Успешная авторизация через заголовок Authorization")
    fun testAuthenticateSuccessAuthorizationHeader() {
        val permission = AppDispatcherPermission().apply {
            login = testLogin
            passBcrypt = hashedPass
            this.permission = 1
        }
        Mockito.`when`(permissionRepository.findByLogin(testLogin)).thenReturn(permission)

        val header = buildHeader(testLogin, testPassword)
        val result = authService.authenticate(header, null, testLogin)

        assertNotNull(result)
        assertEquals(testLogin, result.login)
    }

    @Test
    @DisplayName("Успешная авторизация через заголовок Basic")
    fun testAuthenticateSuccessBasicHeader() {
        val permission = AppDispatcherPermission().apply {
            login = testLogin
            passBcrypt = hashedPass
            this.permission = 1
        }
        Mockito.`when`(permissionRepository.findByLogin(testLogin)).thenReturn(permission)

        val header = buildHeader(testLogin, testPassword)
        val result = authService.authenticate(null, header, testLogin)

        assertNotNull(result)
        assertEquals(testLogin, result.login)
    }

    @Test
    @DisplayName("Ошибка: отсутствует заголовок авторизации")
    fun testAuthenticateMissingHeader() {
        val ex = assertThrows(DispatcherException::class.java) {
            authService.authenticate(null, null, testLogin)
        }
        assertEquals(HttpStatus.UNAUTHORIZED, ex.httpStatus)
        assertEquals("ACCESS_DENIED", ex.errorCode)
    }

    @Test
    @DisplayName("Ошибка: невалидный Base64")
    fun testAuthenticateInvalidBase64() {
        val ex = assertThrows(DispatcherException::class.java) {
            authService.authenticate("Basic !!!not-base-64!!!", null, testLogin)
        }
        assertEquals(HttpStatus.UNAUTHORIZED, ex.httpStatus)
    }

    @Test
    @DisplayName("Ошибка: отсутствует двоеточие в учетных данных")
    fun testAuthenticateNoColon() {
        val token = Base64.getEncoder().encodeToString("nocolonhere".toByteArray())
        val ex = assertThrows(DispatcherException::class.java) {
            authService.authenticate("Basic $token", null, testLogin)
        }
        assertEquals(HttpStatus.UNAUTHORIZED, ex.httpStatus)
    }

    @Test
    @DisplayName("Ошибка: пользователь не найден в БД")
    fun testAuthenticateUserNotFound() {
        Mockito.`when`(permissionRepository.findByLogin("UNKNOWN")).thenReturn(null)
        val header = buildHeader("UNKNOWN", testPassword)

        val ex = assertThrows(DispatcherException::class.java) {
            authService.authenticate(header, null, "UNKNOWN")
        }
        assertEquals(HttpStatus.FORBIDDEN, ex.httpStatus)
        assertEquals("ACCESS_DENIED", ex.errorCode)
    }

    @Test
    @DisplayName("Ошибка: неверный пароль")
    fun testAuthenticateWrongPassword() {
        val permission = AppDispatcherPermission().apply {
            login = testLogin
            passBcrypt = hashedPass
            this.permission = 1
        }
        Mockito.`when`(permissionRepository.findByLogin(testLogin)).thenReturn(permission)

        val header = buildHeader(testLogin, "WrongPassword")
        val ex = assertThrows(DispatcherException::class.java) {
            authService.authenticate(header, null, testLogin)
        }
        assertEquals(HttpStatus.FORBIDDEN, ex.httpStatus)
    }

    @Test
    @DisplayName("Ошибка: permission != 1")
    fun testAuthenticatePermissionDisabled() {
        val permission = AppDispatcherPermission().apply {
            login = testLogin
            passBcrypt = hashedPass
            this.permission = 0
        }
        Mockito.`when`(permissionRepository.findByLogin(testLogin)).thenReturn(permission)

        val header = buildHeader(testLogin, testPassword)
        val ex = assertThrows(DispatcherException::class.java) {
            authService.authenticate(header, null, testLogin)
        }
        assertEquals(HttpStatus.FORBIDDEN, ex.httpStatus)
    }

    @Test
    @DisplayName("Ошибка: systemId не совпадает с логином")
    fun testAuthenticateSystemIdMismatch() {
        val permission = AppDispatcherPermission().apply {
            login = testLogin
            passBcrypt = hashedPass
            this.permission = 1
        }
        Mockito.`when`(permissionRepository.findByLogin(testLogin)).thenReturn(permission)

        val header = buildHeader(testLogin, testPassword)
        val ex = assertThrows(DispatcherException::class.java) {
            authService.authenticate(header, null, "OTHER_SYSTEM")
        }
        assertEquals(HttpStatus.FORBIDDEN, ex.httpStatus)
    }
}
