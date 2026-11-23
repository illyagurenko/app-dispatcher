package ru.itone.illya4gurenko.exception

import org.springframework.http.HttpStatus

/**
 * базовое исключение бизнес-логики сервиса диспетчера
 */
class DispatcherException(
    val requestId: String?,
    val errorCode: String,
    override val message: String,
    val httpStatus: HttpStatus
) : RuntimeException(message)
