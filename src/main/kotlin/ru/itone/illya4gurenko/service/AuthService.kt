package ru.itone.illya4gurenko.service

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import ru.itone.illya4gurenko.entity.AppDispatcherPermission
import ru.itone.illya4gurenko.exception.DispatcherException
import ru.itone.illya4gurenko.repository.PermissionRepository
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * сервис проверки прав и аутентификации вызывающих систем
 */
@Service
class AuthService(
    private val permissionRepository: PermissionRepository,
    private val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * проверка доступа вызывающей системы по заголовку basic auth
     * извлекает логин и пароль, проверяет наличие в таблице прав,
     * валидирует bcrypt-хеш и признак активного доступа
     */
    fun authenticate(
        authHeader: String?,
        basicHeader: String?,
        expectedSystemId: String?
    ): AppDispatcherPermission {
        val rawHeader = selectAuthHeader(authHeader, basicHeader)
            ?: run {
                log.warn("missing auth header")
                throw DispatcherException(
                    requestId = null,
                    errorCode = "ACCESS_DENIED",
                    message = "Отсутствует заголовок авторизации (Authorization или Basic)",
                    httpStatus = HttpStatus.UNAUTHORIZED
                )
            }

        val (login, password) = parseCredentials(rawHeader)

        val permission = permissionRepository.findByLogin(login)
            ?: run {
                log.warn("user not found: {}", login)
                throw DispatcherException(
                    requestId = null,
                    errorCode = "ACCESS_DENIED",
                    message = "Неверный логин, пароль или отсутствует разрешение",
                    httpStatus = HttpStatus.FORBIDDEN
                )
            }

        val passBcrypt = permission.passBcrypt
        if (passBcrypt.isNullOrBlank() || !passwordEncoder.matches(password, passBcrypt)) {
            log.warn("invalid password for user: {}", login)
            throw DispatcherException(
                requestId = null,
                errorCode = "ACCESS_DENIED",
                message = "Неверный логин, пароль или отсутствует разрешение",
                httpStatus = HttpStatus.FORBIDDEN
            )
        }

        if (permission.permission?.toInt() != 1) {
            log.warn("user permission is not active: login={}, permission={}", login, permission.permission)
            throw DispatcherException(
                requestId = null,
                errorCode = "ACCESS_DENIED",
                message = "Неверный логин, пароль или отсутствует разрешение",
                httpStatus = HttpStatus.FORBIDDEN
            )
        }

        if (!expectedSystemId.isNullOrBlank() && !login.equals(expectedSystemId, ignoreCase = true)) {
            log.warn("login '{}' does not match systemId '{}'", login, expectedSystemId)
            throw DispatcherException(
                requestId = null,
                errorCode = "ACCESS_DENIED",
                message = "Идентификатор системы '$expectedSystemId' не соответствует учетной записи '$login'",
                httpStatus = HttpStatus.FORBIDDEN
            )
        }

        log.debug("user authenticated: {}", login)
        return permission
    }

    /**
     * выбор заголовка авторизации из Authorization или Basic
     */
    private fun selectAuthHeader(authHeader: String?, basicHeader: String?): String? {
        val auth = authHeader?.trim()
        if (!auth.isNullOrBlank()) return auth
        val basic = basicHeader?.trim()
        if (!basic.isNullOrBlank()) return basic
        return null
    }

    /**
     * парсинг и декодирование учетных данных login:password из base64
     */
    private fun parseCredentials(rawHeader: String): Pair<String, String> {
        val token = if (rawHeader.startsWith("Basic ", ignoreCase = true)) {
            rawHeader.substring(6).trim()
        } else {
            rawHeader.trim()
        }

        val decoded = try {
            String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8)
        } catch (e: IllegalArgumentException) {
            log.warn("failed to decode base64 auth header: {}", e.message)
            throw DispatcherException(
                requestId = null,
                errorCode = "ACCESS_DENIED",
                message = "Некорректный формат Base64 в заголовке авторизации",
                httpStatus = HttpStatus.UNAUTHORIZED
            )
        }

        val colonIndex = decoded.indexOf(':')
        if (colonIndex == -1) {
            log.warn("auth header missing ':' separator")
            throw DispatcherException(
                requestId = null,
                errorCode = "ACCESS_DENIED",
                message = "Некорректный формат учетных данных (ожидается login:password)",
                httpStatus = HttpStatus.UNAUTHORIZED
            )
        }

        val login = decoded.substring(0, colonIndex).trim()
        val password = decoded.substring(colonIndex + 1)
        return Pair(login, password)
    }
}
