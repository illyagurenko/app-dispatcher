package ru.itone.illya4gurenko.service

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import ru.itone.illya4gurenko.entity.AppDispatcherTrans
import ru.itone.illya4gurenko.exception.DispatcherException
import ru.itone.illya4gurenko.repository.TransRepository
import tools.jackson.databind.ObjectMapper
import java.time.ZonedDateTime
import java.util.UUID

/**
 * сервис логирования и аудита транзакций диспетчера в базе данных
 */
@Service
class TransactionService(
    private val transRepository: TransRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * создание начальной записи в транзакционной таблице со статусом PROCESSING
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun createTransaction(requestId: String, systemId: String?, requestBody: Any?): AppDispatcherTrans {
        val uuid = parseUuid(requestId)

        if (transRepository.existsById(uuid)) {
            log.warn("transaction already exists: requestId={}", requestId)
            throw DispatcherException(
                requestId = requestId,
                errorCode = "DUPLICATE_REQUEST_ID",
                message = "Транзакция с requestId=$requestId уже существует",
                httpStatus = HttpStatus.CONFLICT
            )
        }

        val jsonReq = serializeToJson(requestBody)

        val trans = AppDispatcherTrans().apply {
            id = uuid
            protocolType = "REST"
            this.systemId = systemId
            reqData = jsonReq
            status = "PROCESSING"
            insTs = ZonedDateTime.now()
            updTs = ZonedDateTime.now()
        }

        return transRepository.save(trans)
    }

    /**
     * фиксация успешного ответа в транзакционной таблице
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun recordSuccess(requestId: String, responseBody: Any?): AppDispatcherTrans {
        val uuid = parseUuid(requestId)
        val jsonResp = serializeToJson(responseBody)

        val trans = transRepository.findById(uuid).orElseGet {
            AppDispatcherTrans().apply {
                id = uuid
                protocolType = "REST"
                insTs = ZonedDateTime.now()
            }
        }

        trans.status = "SUCCESS"
        trans.respCode = "0"
        trans.respDesc = "SUCCESS"
        trans.respData = jsonResp
        trans.updTs = ZonedDateTime.now()

        return transRepository.save(trans)
    }

    /**
     * фиксация ошибки обработки в транзакционной таблице
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun recordError(
        requestId: String?,
        errorCode: String,
        errorDesc: String,
        errorBody: Any?,
        systemId: String? = null,
        reqBody: Any? = null
    ): AppDispatcherTrans? {
        val uuid = requestId?.let {
            try {
                UUID.fromString(it.trim())
            } catch (e: Exception) {
                null
            }
        } ?: return null

        val jsonResp = serializeToJson(errorBody)
        val jsonReq = if (reqBody != null) serializeToJson(reqBody) else null

        val trans = transRepository.findById(uuid).orElseGet {
            AppDispatcherTrans().apply {
                id = uuid
                protocolType = "REST"
                this.systemId = systemId
                if (jsonReq != null) {
                    reqData = jsonReq
                }
                insTs = ZonedDateTime.now()
            }
        }

        trans.status = "ERROR"
        trans.respCode = errorCode.take(16)
        trans.respDesc = errorDesc.take(256)
        trans.respData = jsonResp
        trans.updTs = ZonedDateTime.now()

        return transRepository.save(trans)
    }

    /**
     * валидация и парсинг строкового requestId в формат uuid
     */
    private fun parseUuid(requestId: String): UUID {
        return try {
            UUID.fromString(requestId.trim())
        } catch (e: Exception) {
            throw DispatcherException(
                requestId = requestId,
                errorCode = "INVALID_REQUEST_ID",
                message = "Поле requestId не является валидным UUID: $requestId",
                httpStatus = HttpStatus.BAD_REQUEST
            )
        }
    }

    /**
     * сериализация объекта в json-строку
     */
    private fun serializeToJson(obj: Any?): String? {
        if (obj == null) return null
        if (obj is String) return obj
        return try {
            objectMapper.writeValueAsString(obj)
        } catch (e: Exception) {
            log.error("failed to serialize object to json: {}", e.message)
            obj.toString()
        }
    }
}
