package ru.itone.illya4gurenko.dto.response

/**
 * блок сгруппированных источников данных в ответе диспетчера
 */
data class ResponseDataDto(
    val datasource: List<ResponseDatasourceDto>
)
