package ru.itone.illya4gurenko.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * сопоставление jdbc_url со списком привязанных счетов
 */
data class ResponseDatasourceDto(
    @JsonProperty("jdbc_url")
    val jdbcUrl: String? = null,
    val account: List<String>
)
