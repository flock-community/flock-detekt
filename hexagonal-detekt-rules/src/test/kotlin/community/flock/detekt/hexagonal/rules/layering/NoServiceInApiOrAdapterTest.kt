package community.flock.detekt.hexagonal.rules.layering

import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NoServiceInApiOrAdapterTest {

    private val rule = NoServiceInApiOrAdapter(Config.empty)

    @Test
    fun `reports UserService in api package`() {
        val code = """
            package com.example.api

            class UserService {
                fun createUser(name: String): String = name
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("UserService"))
        assert(findings[0].message.contains("API"))
    }

    @Test
    fun `reports OrderService in adapter package`() {
        val code = """
            package com.example.adapter.persistence

            class OrderService {
                fun getOrder(id: String): String = id
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("OrderService"))
        assert(findings[0].message.contains("adapter"))
    }

    @Test
    fun `reports PaymentService in controller package`() {
        val code = """
            package com.example.controller

            class PaymentService {
                fun processPayment(amount: Double): Boolean = true
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("PaymentService"))
    }

    @Test
    fun `reports Service in infrastructure package`() {
        val code = """
            package com.example.infrastructure.messaging

            class NotificationService {
                fun sendNotification(message: String): Unit {}
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("NotificationService"))
    }

    @Test
    fun `does not report UserService in domain package`() {
        val code = """
            package com.example.domain.user

            class UserService {
                fun createUser(name: String): String = name
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report UserService in core package`() {
        val code = """
            package com.example.core.user

            class UserService {
                fun createUser(name: String): String = name
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report UserController in api package`() {
        val code = """
            package com.example.api

            class UserController {
                fun handleRequest(): String = "OK"
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report interface named Service in api package`() {
        val code = """
            package com.example.api

            interface UserService {
                fun createUser(name: String): String
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report adapter class in adapter package`() {
        val code = """
            package com.example.adapter.persistence

            class UserRepositoryAdapter {
                fun save(user: String): Unit {}
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report Service in random package`() {
        val code = """
            package com.example.services

            class UserService {
                fun createUser(name: String): String = name
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }
}
