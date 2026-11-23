package ru.itone.illya4gurenko

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * точка входа сервиса app-dispatcher
 */
@SpringBootApplication
class AppDispatcherApplication

fun main(args: Array<String>) {
	runApplication<AppDispatcherApplication>(*args)
}
