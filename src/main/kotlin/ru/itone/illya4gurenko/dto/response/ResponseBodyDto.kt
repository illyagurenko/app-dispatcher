package ru.itone.illya4gurenko.dto.response

import ru.itone.illya4gurenko.dto.ErrorDto

/**
 * корневой dto ответа от сервиса диспетчера
 */
data class ResponseBodyDto(
    val requestId: String? = null,
    val systemId: String = "APP_DISPATCHER",
    val status: String,
    val error: ErrorDto? = null,
    val data: ResponseDataDto? = null
)
