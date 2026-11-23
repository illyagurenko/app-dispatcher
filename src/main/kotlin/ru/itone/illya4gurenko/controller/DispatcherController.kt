package ru.itone.illya4gurenko.controller

import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import ru.itone.illya4gurenko.dto.request.RequestBodyDto
import ru.itone.illya4gurenko.dto.response.ResponseBodyDto
import ru.itone.illya4gurenko.service.DispatcherService

/**
 * контроллер получения источников данных по счетам
 */
@RestController
class DispatcherController(private val dispatcherService: DispatcherService) {

    /**
     * эндпоинт получения источников данных по счетам
     */
    @PostMapping("/get-datasource-by-account", "/get-datasoure-by-account")
    fun getDatasourceByAccount(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) authHeader: String?,
        @RequestHeader(value = "Basic", required = false) basicHeader: String?,
        @Valid @RequestBody request: RequestBodyDto
    ): ResponseEntity<ResponseBodyDto> {
        val response = dispatcherService.processRequest(authHeader, basicHeader, request)
        return ResponseEntity.ok(response)
    }
}
