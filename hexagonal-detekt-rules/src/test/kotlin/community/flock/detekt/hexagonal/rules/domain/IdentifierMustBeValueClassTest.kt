package community.flock.detekt.hexagonal.rules.domain

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class IdentifierMustBeValueClassTest {

    private val rule = IdentifierMustBeValueClass(Config.empty)

    @Test
    fun `reports raw identifier types in domain data class`() {
        val code = """
            package com.example.domain.order

            import java.util.UUID

            data class Order(
                val id: UUID,
                val customerId: String,
                val sequenceId: Long,
                val lineId: Int,
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(4, findings.size)
        assert(findings[0].message.contains("'id'"))
        assert(findings[0].message.contains("OrderId"))
        assert(findings[1].message.contains("CustomerId"))
    }

    @Test
    fun `reports nullable and fully qualified raw identifier types`() {
        val code = """
            package com.example.domain.order

            data class Order(
                val parentId: String?,
                val traceId: java.util.UUID,
                val externalId: kotlin.uuid.Uuid?,
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(3, findings.size)
    }

    @Test
    fun `reports raw identifier properties in class bodies and interfaces`() {
        val code = """
            package com.example.domain.order

            interface HasOwner {
                val ownerId: String
            }

            class OrderService {
                val defaultTenantId: String = "acme"
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(2, findings.size)
    }

    @Test
    fun `does not report value class identifiers`() {
        val code = """
            package com.example.domain.order

            @JvmInline
            value class OrderId(val value: String)

            data class Order(
                val id: OrderId,
                val customerId: CustomerId?,
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report the underlying property of a value class`() {
        val code = """
            package com.example.domain.order

            @JvmInline
            value class OrderId(val id: String)
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report non-identifier names or non-raw types`() {
        val code = """
            package com.example.domain.order

            data class Order(
                val name: String,
                val paid: Boolean,
                val valid: Boolean,
                val ids: List<String>,
                val customerIds: Set<String>,
                val createdById: UserId,
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report overrides, locals or plain constructor parameters`() {
        val code = """
            package com.example.domain.order

            class Order(rawId: String) : HasId {
                override val id: String = rawId

                fun lookup() {
                    val localId: String = "x"
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report outside domain package`() {
        val code = """
            package com.example.api.dto

            data class OrderDto(val id: String, val customerId: String)
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `checks id collections when enabled`() {
        val rule = IdentifierMustBeValueClass(TestConfig("checkIdCollections" to true))
        val code = """
            package com.example.domain.order

            data class Batch(
                val ids: List<String>,
                val customerIds: Set<java.util.UUID>?,
                val orderIds: List<OrderId>,
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(2, findings.size)
        assert(findings[1].message.contains("CustomerId"))
    }

    @Test
    fun `supports custom identifier pattern and raw types`() {
        val rule = IdentifierMustBeValueClass(
            TestConfig(
                "identifierNamePattern" to ".*Key$",
                "rawTypes" to listOf("String"),
            )
        )
        val code = """
            package com.example.domain.order

            data class Order(
                val idempotencyKey: String,
                val customerId: String,
                val sequenceKey: Long,
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("idempotencyKey"))
    }

    @Test
    fun `supports fully qualified domain package prefix`() {
        val rule = IdentifierMustBeValueClass(
            TestConfig("domainPackages" to listOf("com.acme.platform.domain"))
        )
        val inDomain = """
            package com.acme.platform.domain.model

            data class Order(val id: String)
        """.trimIndent()
        val outsideDomain = """
            package com.acme.platform.database.domain

            data class OrderRow(val id: String)
        """.trimIndent()

        assertEquals(1, rule.lint(inDomain).size)
        assertEquals(0, rule.lint(outsideDomain).size)
    }
}
