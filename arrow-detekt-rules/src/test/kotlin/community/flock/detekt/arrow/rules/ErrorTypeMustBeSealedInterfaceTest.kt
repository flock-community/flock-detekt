package community.flock.detekt.arrow.rules

import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ErrorTypeMustBeSealedInterfaceTest {

    private val rule = ErrorTypeMustBeSealedInterface(Config.empty)

    @Test
    fun `reports non-sealed interface with Error suffix`() {
        val code = """
            package com.example.domain.user

            interface UserError {
                data class NotFound(val id: String) : UserError
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("sealed interface"))
        assert(findings[0].message.contains("non-sealed interface"))
    }

    @Test
    fun `reports sealed class with Error suffix`() {
        val code = """
            package com.example.domain.payment

            sealed class PaymentError {
                data class Declined(val reason: String) : PaymentError()
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("sealed interface"))
        assert(findings[0].message.contains("sealed class"))
    }

    @Test
    fun `reports open class with Error suffix`() {
        val code = """
            package com.example.domain.order

            open class OrderError {
                data class NotFound(val id: String) : OrderError()
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("sealed interface"))
        assert(findings[0].message.contains("open class"))
    }

    @Test
    fun `reports regular class with Error suffix`() {
        val code = """
            package com.example.domain.order

            class OrderError
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("sealed interface"))
    }

    @Test
    fun `does not report sealed interface with Error suffix`() {
        val code = """
            package com.example.domain.user

            sealed interface UserError {
                data class NotFound(val id: String) : UserError
                data class InvalidInput(val field: String) : UserError
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report sealed interface with Failure suffix`() {
        val code = """
            package com.example.domain.payment

            sealed interface PaymentFailure {
                data class InsufficientFunds(val amount: Double) : PaymentFailure
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report classes without error suffix`() {
        val code = """
            package com.example.domain.user

            interface User {
                val id: String
                val name: String
            }

            class UserService {
                fun getUser(id: String): User? = null
            }

            sealed class UserState {
                object Loading : UserState()
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report error types outside domain packages`() {
        val code = """
            package com.example.api.user

            interface UserError {
                data class NotFound(val id: String) : UserError
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report error types in adapter packages`() {
        val code = """
            package com.example.adapter.persistence

            interface RepositoryError {
                data class ConnectionFailed(val reason: String) : RepositoryError
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `reports error types in core package`() {
        val code = """
            package com.example.core.error

            interface ApplicationError {
                data class Unknown(val message: String) : ApplicationError
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `reports error types in nested domain package`() {
        val code = """
            package com.example.domain.user.errors

            interface ValidationError {
                data class InvalidEmail(val email: String) : ValidationError
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `reports multiple non-compliant error types`() {
        val code = """
            package com.example.domain.payment

            interface PaymentError {
                data class Declined(val reason: String) : PaymentError
            }

            sealed class TransactionFailure {
                data class Timeout(val duration: Long) : TransactionFailure()
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(2, findings.size)
    }
}
