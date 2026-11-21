package ru.itone.illya4gurenko.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class ResponseDataDto(
    val systemId: String,
    val requestId: String,
    val account: List<String>)
