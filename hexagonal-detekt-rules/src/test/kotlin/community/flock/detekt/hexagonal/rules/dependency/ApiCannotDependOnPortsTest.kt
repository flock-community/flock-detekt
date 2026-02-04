package community.flock.detekt.hexagonal.rules.dependency

import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ApiCannotDependOnPortsTest {

    private val rule = ApiCannotDependOnPorts(Config.empty)

    @Test
    fun `reports controller with Repository dependency`() {
        val code = """
            package com.example.api

            class DeclarationsController(
                private val declarationRepository: DeclarationRepository
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("DeclarationRepository"))
        assert(findings[0].message.contains("domain service"))
    }

    @Test
    fun `reports controller with Gateway dependency`() {
        val code = """
            package com.example.api

            class PaymentController(
                private val paymentGateway: PaymentGateway
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("PaymentGateway"))
    }

    @Test
    fun `reports controller with Client dependency`() {
        val code = """
            package com.example.api

            class NotificationController(
                private val emailClient: EmailClient
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("EmailClient"))
    }

    @Test
    fun `reports controller with Port dependency`() {
        val code = """
            package com.example.api

            class UserController(
                private val userPort: UserPort
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("UserPort"))
    }

    @Test
    fun `reports multiple port dependencies in single class`() {
        val code = """
            package com.example.api

            class OrderController(
                private val orderRepository: OrderRepository,
                private val paymentGateway: PaymentGateway,
                private val notificationClient: NotificationClient
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(3, findings.size)
    }

    @Test
    fun `reports port dependency in nested api package`() {
        val code = """
            package com.example.api.user

            class UserController(
                private val userRepository: UserRepository
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `reports port dependency in controller package`() {
        val code = """
            package com.example.controller

            class UserController(
                private val userRepository: UserRepository
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `reports port dependency in rest package`() {
        val code = """
            package com.example.rest

            class UserEndpoint(
                private val userRepository: UserRepository
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not report controller with Service dependency`() {
        val code = """
            package com.example.api

            class AdviceController(
                private val adviceService: DeclarationAdviceService
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report class in domain package using Repository`() {
        val code = """
            package com.example.domain.service

            class DeclarationService(
                private val declarationRepository: DeclarationRepository
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report class outside api packages`() {
        val code = """
            package com.example.app

            class Application(
                private val userRepository: UserRepository
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report interface in api package`() {
        val code = """
            package com.example.api

            interface ControllerInterface(
                val repository: UserRepository
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report enum in api package`() {
        val code = """
            package com.example.api

            enum class Status {
                ACTIVE, INACTIVE
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `handles nullable port type`() {
        val code = """
            package com.example.api

            class UserController(
                private val userRepository: UserRepository?
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("UserRepository"))
    }

    @Test
    fun `handles generic port type`() {
        val code = """
            package com.example.api

            class UserController(
                private val repository: GenericRepository<User>
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("GenericRepository"))
    }

    @Test
    fun `does not report data class in api package`() {
        val code = """
            package com.example.api

            data class UserRequest(
                val name: String,
                val email: String
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }
}
