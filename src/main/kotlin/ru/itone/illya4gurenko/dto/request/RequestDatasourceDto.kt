package ru.itone.illya4gurenko.dto.request

import com.fasterxml.jackson.annotation.JsonProperty

data class RequestDatasourceDto(
    val jdbc_url: String? = null,
    val account: List<String>
)
