package ru.itone.illya4gurenko.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import ru.itone.illya4gurenko.dto.ErrorDto
import ru.itone.illya4gurenko.dto.request.RequestBodyDto
import ru.itone.illya4gurenko.dto.response.ResponseBodyDto
import ru.itone.illya4gurenko.service.TransactionService

/**
 * глобальный обработчик исключений и ошибок валидации
 */
@RestControllerAdvice
class GlobalExceptionHandler(
    private val transactionService: TransactionService? = null
) {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val SYSTEM_ID = "APP_DISPATCHER"
    }

    /**
     * обработка ошибок spring validation (некорректные или отсутствующие поля)
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ResponseBodyDto> {
        val requestTarget = ex.bindingResult.target as? RequestBodyDto
        val requestId = requestTarget?.data?.requestId

        val fieldsMessage = ex.bindingResult.fieldErrors
            .joinToString("; ") { error -> "${error.field}: ${error.defaultMessage}" }

        val response = buildErrorResponse(
            requestId = requestId,
            code = "VALIDATION_ERROR",
            message = "Ошибка валидации полей: $fieldsMessage"
        )

        if (requestId != null) {
            try {
                transactionService?.recordError(
                    requestId = requestId,
                    errorCode = "VALIDATION_ERROR",
                    errorDesc = "Ошибка валидации: $fieldsMessage",
                    errorBody = response,
                    systemId = requestTarget.data.systemId,
                    reqBody = requestTarget
                )
            } catch (ignored: Exception) {
                log.debug("failed to save transaction for invalid request: {}", ignored.message)
            }
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    /**
     * обработка некорректного json или пустого тела запроса
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadableException(ex: HttpMessageNotReadableException): ResponseEntity<ResponseBodyDto> {
        log.warn("failed to read request body: {}", ex.message)

        val response = buildErrorResponse(
            requestId = null,
            code = "INVALID_REQUEST_BODY",
            message = "Некорректный формат JSON или отсутствует тело запроса"
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    /**
     * обработка бизнес-ошибок сервиса диспетчера
     */
    @ExceptionHandler(DispatcherException::class)
    fun handleDispatcherException(ex: DispatcherException): ResponseEntity<ResponseBodyDto> {
        log.warn(
            "dispatcher error [requestId={}, code={}]: {}",
            ex.requestId, ex.errorCode, ex.message
        )

        val response = buildErrorResponse(
            requestId = ex.requestId,
            code = ex.errorCode,
            message = ex.message
        )

        return ResponseEntity.status(ex.httpStatus).body(response)
    }

    /**
     * обработка ситуации отсутствия запрошенного эндпоинта (404)
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException::class)
    fun handleNoResourceFound(ex: org.springframework.web.servlet.resource.NoResourceFoundException): ResponseEntity<ResponseBodyDto> {
        val response = buildErrorResponse(
            requestId = null,
            code = "NOT_FOUND",
            message = "Эндпоинт '${ex.resourcePath}' не найден"
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response)
    }

    /**
     * обработка всех остальных непредвиденных исключений
     */
    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(ex: Exception): ResponseEntity<ResponseBodyDto> {
        log.error("unexpected server error", ex)

        val response = buildErrorResponse(
            requestId = null,
            code = "INTERNAL_ERROR",
            message = "Внутренняя ошибка сервиса"
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }

    /**
     * построение стандартизированного тела ошибки
     */
    private fun buildErrorResponse(
        requestId: String?,
        code: String,
        message: String
    ): ResponseBodyDto = ResponseBodyDto(
        requestId = requestId,
        systemId = SYSTEM_ID,
        status = "error",
        error = ErrorDto(code = code, message = message),
        data = null
    )
}
