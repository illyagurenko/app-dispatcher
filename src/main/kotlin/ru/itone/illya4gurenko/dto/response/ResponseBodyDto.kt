package ru.itone.illya4gurenko.dto.response

import ru.itone.illya4gurenko.dto.ErrorDto

data class ResponseBodyDto(
    val data: ResponseDataDto?,
    val status: String,
    val error: ErrorDto?) {}
