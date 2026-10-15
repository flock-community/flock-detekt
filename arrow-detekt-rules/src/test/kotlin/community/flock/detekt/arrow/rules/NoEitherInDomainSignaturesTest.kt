package community.flock.detekt.arrow.rules

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NoEitherInDomainSignaturesTest {

    private val rule = NoEitherInDomainSignatures(Config.empty)

    @Test
    fun `reports public function returning Either`() {
        val code = """
            package com.example.domain.order

            import arrow.core.Either

            class OrderService {
                fun place(order: Order): Either<PlaceOrderError, OrderId> = TODO()
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("place"))
        assert(findings[0].message.contains("Raise"))
    }

    @Test
    fun `reports nullable, fully qualified and suspend Either return types`() {
        val code = """
            package com.example.domain.order

            interface OrderRepository {
                fun find(id: OrderId): Either<FindError, Order>?
                suspend fun insert(order: Order): arrow.core.Either<InsertError, Unit>
            }

            internal fun parse(raw: String): Either<ParseError, Order> = TODO()
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(3, findings.size)
    }

    @Test
    fun `reports properties, constructor parameters and type aliases of type Either`() {
        val code = """
            package com.example.domain.order

            typealias OrderResult = Either<OrderError, Order>

            data class Attempt(val outcome: Either<OrderError, Order>)

            class Validator(initial: Either<OrderError, Order>) {
                val last: Either<OrderError, Order> = initial
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(4, findings.size)
    }

    @Test
    fun `does not report Raise-based functions`() {
        val code = """
            package com.example.domain.order

            class OrderService {
                context(_: Raise<PlaceOrderError>)
                fun place(order: Order): OrderId = TODO()

                fun count(): Int = 0
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report private, local, override or nested Either usages`() {
        val code = """
            package com.example.domain.order

            class OrderService(private val cache: Either<CacheError, Cache>) : Placer {
                private fun attempt(): Either<PlaceOrderError, OrderId> = TODO()

                override fun place(order: Order): Either<PlaceOrderError, OrderId> = TODO()

                fun all(): List<Either<PlaceOrderError, OrderId>> = emptyList()

                fun run() {
                    val local: Either<PlaceOrderError, OrderId> = attempt()
                    fun nested(): Either<PlaceOrderError, OrderId> = local
                }
            }

            class Parser private constructor(input: Either<ParseError, String>)

            private typealias Internal = Either<OrderError, Order>
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report Result by default`() {
        val code = """
            package com.example.domain.run

            data class NewRun(val result: Result) {
                sealed interface Result
            }

            fun parse(raw: String): Result<Int> = runCatching { raw.toInt() }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `reports kotlin Result when opted in without matching a domain Result type`() {
        val rule = NoEitherInDomainSignatures(
            TestConfig("forbiddenTypes" to listOf("Either", "kotlin.Result"))
        )
        val stdlibResult = """
            package com.example.domain.parse

            fun parse(raw: String): Result<Int> = runCatching { raw.toInt() }
        """.trimIndent()
        val ownResult = """
            package com.example.domain.run

            data class NewRun(val result: Result) {
                sealed interface Result
            }
        """.trimIndent()
        val importedResult = """
            package com.example.domain.run

            import com.example.domain.shared.Result

            fun outcome(): Result = TODO()
        """.trimIndent()

        assertEquals(1, rule.lint(stdlibResult).size)
        assertEquals(0, rule.lint(ownResult).size)
        assertEquals(0, rule.lint(importedResult).size)
    }

    @Test
    fun `does not report outside domain packages`() {
        val code = """
            package com.example.api.order

            class OrderEndpoints {
                fun place(order: OrderDto): Either<ApiError, OrderDto> = TODO()
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `supports fully qualified domain package prefix`() {
        val rule = NoEitherInDomainSignatures(
            TestConfig("domainPackages" to listOf("com.acme.platform.domain"))
        )
        val inDomain = """
            package com.acme.platform.domain.services

            fun place(): Either<PlaceOrderError, OrderId> = TODO()
        """.trimIndent()
        val outsideDomain = """
            package com.acme.platform.api.domain

            fun place(): Either<PlaceOrderError, OrderId> = TODO()
        """.trimIndent()

        assertEquals(1, rule.lint(inDomain).size)
        assertEquals(0, rule.lint(outsideDomain).size)
    }
}
