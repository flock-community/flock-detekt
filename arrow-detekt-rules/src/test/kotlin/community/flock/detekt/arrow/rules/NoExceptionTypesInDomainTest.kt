package community.flock.detekt.arrow.rules

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NoExceptionTypesInDomainTest {

    private val rule = NoExceptionTypesInDomain(Config.empty)

    @Test
    fun `reports domain class extending a built-in exception`() {
        val code = """
            package com.example.domain.repository

            class OrderAlreadyExists(val id: OrderId) : IllegalStateException("Order ${'$'}id already exists")

            interface OrderRepository {
                fun insert(order: Order)
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("OrderAlreadyExists"))
        assert(findings[0].message.contains("IllegalStateException"))
        assert(findings[0].message.contains("Raise"))
    }

    @Test
    fun `reports every built-in throwable supertype`() {
        val code = """
            package com.example.domain.order

            class A : Throwable()
            class B : Exception("b")
            class C : RuntimeException()
            class D : Error()
            class E : IllegalArgumentException()
            class F : kotlin.IllegalStateException()
            class G : java.lang.RuntimeException()
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(7, findings.size)
    }

    @Test
    fun `reports types whose name ends with Exception`() {
        val code = """
            package com.example.domain.order

            import com.example.shared.BaseDomainException

            class InvalidOrder : BaseDomainException()

            object OrderNotFound : NoSuchElementException()

            class Timeout : java.util.concurrent.TimeoutException()
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(3, findings.size)
    }

    @Test
    fun `reports exception with secondary constructors only`() {
        val code = """
            package com.example.domain.order

            class InvalidOrder : Exception {
                constructor(message: String) : super(message)
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not report sealed error hierarchies`() {
        val code = """
            package com.example.domain.order

            sealed interface PlaceOrderError {
                data object Empty : PlaceOrderError
                data class Duplicate(val id: OrderId) : PlaceOrderError
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report a domain interface or class named like a built-in`() {
        val ownInterface = """
            package com.example.domain.order

            sealed interface Error

            data object NotFound : Error
        """.trimIndent()
        val ownClass = """
            package com.example.domain.order

            sealed class Error {
                data object Empty : Error()
            }
        """.trimIndent()

        assertEquals(0, rule.lint(ownInterface).size)
        assertEquals(0, rule.lint(ownClass).size)
    }

    @Test
    fun `does not report built-in names imported from another package`() {
        val code = """
            package com.example.domain.order

            import com.example.domain.shared.Error

            class NotFound : Error()
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report exception types outside domain packages`() {
        val code = """
            package com.example.adapter.persistence

            class DatabaseUnavailable : RuntimeException()

            class ConnectionException : Exception()
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `supports configured exception types and suffixes`() {
        val rule = NoExceptionTypesInDomain(
            TestConfig(
                "exceptionTypes" to listOf("com.example.shared.Failure"),
                "exceptionSuffixes" to listOf("Fault"),
            )
        )
        val code = """
            package com.example.domain.order

            import com.example.shared.Failure

            class InvalidOrder : Failure()
            class Rejected : PaymentFault()
            class Plain : RuntimeException()
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(2, findings.size)
    }

    @Test
    fun `supports fully qualified domain package prefix`() {
        val rule = NoExceptionTypesInDomain(
            TestConfig("domainPackages" to listOf("com.acme.platform.domain"))
        )
        val inDomain = """
            package com.acme.platform.domain.repository

            class HashConflict : IllegalStateException()
        """.trimIndent()
        val outsideDomain = """
            package com.acme.database.domain

            class HashConflict : IllegalStateException()
        """.trimIndent()

        assertEquals(1, rule.lint(inDomain).size)
        assertEquals(0, rule.lint(outsideDomain).size)
    }
}
