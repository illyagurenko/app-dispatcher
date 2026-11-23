package ru.itone.illya4gurenko.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import org.hibernate.validator.constraints.UUID

/**
 * блок данных входящего запроса
 */
data class RequestDataDto(
    @NotBlank(message = "Поле systemId обязательно")
    val systemId: String? = null,
    @NotBlank(message = "Поле requestId обязательно")
    @UUID(message = "Поле requestId должно быть в формате UUID")
    val requestId: String? = null,
    @NotEmpty(message = "Список account не должен быть пустым")
    val account: List<@NotBlank(message = "Элемент списка account не должен быть пустым") String?>? = null
)
