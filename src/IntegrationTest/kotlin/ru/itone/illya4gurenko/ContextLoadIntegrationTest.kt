package ru.itone.illya4gurenko

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext

/**
 * интеграционный тест проверки поднятия контекста приложения
 */
@SpringBootTest
class ContextLoadIntegrationTest {

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    @DisplayName("проверка поднятия контекста")
    fun contextLoads() {
        assertNotNull(applicationContext)
    }
}
