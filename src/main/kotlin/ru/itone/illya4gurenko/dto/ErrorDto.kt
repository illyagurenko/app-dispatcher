package ru.itone.illya4gurenko.dto

/**
 * блок описания ошибки в ответе
 */
data class ErrorDto(
    val code: String? = null,
    val message: String? = null
)
