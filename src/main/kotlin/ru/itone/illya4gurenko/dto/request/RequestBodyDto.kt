package ru.itone.illya4gurenko.dto.request

import ru.itone.illya4gurenko.dto.ErrorDto

data class RequestBodyDto(
    val requestId: String,
    val systemId: String = "APP_DISPATCHER",
    val status: String,
    val error: ErrorDto? = null,
    val data: RequestDataDto? = null
)
