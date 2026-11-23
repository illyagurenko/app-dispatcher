package ru.itone.illya4gurenko.service

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import ru.itone.illya4gurenko.dto.ErrorDto
import ru.itone.illya4gurenko.dto.request.RequestBodyDto
import ru.itone.illya4gurenko.dto.response.ResponseBodyDto
import ru.itone.illya4gurenko.dto.response.ResponseDataDto
import ru.itone.illya4gurenko.dto.response.ResponseDatasourceDto
import ru.itone.illya4gurenko.exception.DispatcherException
import ru.itone.illya4gurenko.repository.DatasourceRepository

/**
 * сервис координации запросов роутинга и поиска источников данных по счетам
 */
@Service
class DispatcherService(
    private val authService: AuthService,
    private val transactionService: TransactionService,
    private val datasourceRepository: DatasourceRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val SYSTEM_ID = "APP_DISPATCHER"
    }

    /**
     * основной пайплайн обработки запроса:
     * 1. получение rest-запроса и валидация обязательных полей
     * 2. создание начальной записи в транзакционной таблице
     * 3. проверка прав и аутентификация вызывающей системы
     * 4. получение активных источников данных из базы
     * 5. группировка счетов по jdbc_url и сбор неразмеченных счетов
     * 6. фиксация успешного или ошибочного ответа в транзакционной таблице
     */
    fun processRequest(
        authHeader: String?,
        basicHeader: String?,
        request: RequestBodyDto
    ): ResponseBodyDto {
        val requestData = request.data ?: throw DispatcherException(
            requestId = null,
            errorCode = "VALIDATION_ERROR",
            message = "Поле data обязательно",
            httpStatus = HttpStatus.BAD_REQUEST
        )

        val requestId = requestData.requestId ?: throw DispatcherException(
            requestId = null,
            errorCode = "VALIDATION_ERROR",
            message = "Поле requestId обязательно",
            httpStatus = HttpStatus.BAD_REQUEST
        )

        val systemId = requestData.systemId ?: "UNKNOWN"
        val requestedAccounts = requestData.account?.filterNotNull() ?: emptyList()

        log.debug("processing request: requestId={}, systemId={}, accountsCount={}", requestId, systemId, requestedAccounts.size)

        // фиксация начальной записи транзакции в бд
        transactionService.createTransaction(requestId, systemId, request)

        try {
            // проверка прав вызывающей системы
            authService.authenticate(authHeader, basicHeader, systemId)

            // запрос активных источников данных по списку счетов
            val activeDatasources = datasourceRepository.findAllByAccountInAndStatus(requestedAccounts.toSet(), "ACTIVE")

            val accountToUrl = activeDatasources
                .filter { !it.jdbcUrl.isNullOrBlank() && !it.account.isNullOrBlank() }
                .associate { it.account!!.trim() to it.jdbcUrl!!.trim() }

            val urlToAccounts = linkedMapOf<String, MutableList<String>>()
            val unmappedAccounts = mutableListOf<String>()

            // группировка счетов по jdbc_url с сохранением исходного порядка
            for (acc in requestedAccounts.distinct()) {
                val url = accountToUrl[acc]
                if (url != null) {
                    urlToAccounts.computeIfAbsent(url) { mutableListOf() }.add(acc)
                } else {
                    unmappedAccounts.add(acc)
                }
            }

            val datasourceList = mutableListOf<ResponseDatasourceDto>()
            for ((url, accList) in urlToAccounts) {
                datasourceList.add(ResponseDatasourceDto(jdbcUrl = url, account = accList))
            }
            if (unmappedAccounts.isNotEmpty()) {
                // если для счета не найден jdbc_url, отдаем пустой url
                datasourceList.add(ResponseDatasourceDto(jdbcUrl = "", account = unmappedAccounts))
            }

            // формирование успешного ответа
            val response = ResponseBodyDto(
                requestId = requestId,
                systemId = SYSTEM_ID,
                status = "Success",
                error = null,
                data = ResponseDataDto(datasource = datasourceList)
            )

            // сохранение успешного ответа в транзакционную таблицу
            transactionService.recordSuccess(requestId, response)

            return response

        } catch (ex: DispatcherException) {
            // фиксация бизнес-ошибки в транзакционной таблице
            log.warn("request rejected [requestId={}]: {}", requestId, ex.message)
            val errorResponse = ResponseBodyDto(
                requestId = requestId,
                systemId = SYSTEM_ID,
                status = "error",
                error = ErrorDto(code = ex.errorCode, message = ex.message),
                data = null
            )
            transactionService.recordError(
                requestId = requestId,
                errorCode = ex.errorCode,
                errorDesc = ex.message,
                errorBody = errorResponse,
                systemId = systemId,
                reqBody = request
            )
            throw ex

        } catch (ex: Exception) {
            // фиксация внутренней ошибки в транзакционной таблице
            log.error("internal error processing request [requestId={}]: {}", requestId, ex.message, ex)
            val errorResponse = ResponseBodyDto(
                requestId = requestId,
                systemId = SYSTEM_ID,
                status = "error",
                error = ErrorDto(code = "INTERNAL_ERROR", message = "Внутренняя ошибка сервиса: ${ex.message}"),
                data = null
            )
            transactionService.recordError(
                requestId = requestId,
                errorCode = "INTERNAL_ERROR",
                errorDesc = ex.message ?: "Внутренняя ошибка",
                errorBody = errorResponse,
                systemId = systemId,
                reqBody = request
            )
            throw ex
        }
    }
}
