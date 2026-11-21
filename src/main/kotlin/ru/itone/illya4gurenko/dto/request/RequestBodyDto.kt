package ru.itone.illya4gurenko.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import ru.itone.illya4gurenko.dto.ErrorDto

data class RequestBodyDto(
    @NotNull(message = "Поле data обязательно")
    @Valid
    val data: RequestDataDto?,
    val status: String?,
    val error: ErrorDto?
)
