package ru.itone.illya4gurenko.dto.response

data class ResponseDatasourceDto(
    val jdbcUrl: String? = null,
    val account: List<String>
)
