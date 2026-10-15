package community.flock.detekt.hexagonal.rules.domain

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SmartConstructorMustHaveConsistentCopyVisibilityTest {

    private val rule = SmartConstructorMustHaveConsistentCopyVisibility(Config.empty)

    @Test
    fun `reports data class with private constructor without annotation`() {
        val code = """
            package com.example.domain.user

            data class Email private constructor(val value: String, val domain: String) {
                companion object {
                    operator fun invoke(value: String): Email = Email(value, value.substringAfter("@"))
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("Email"))
        assert(findings[0].message.contains("@ConsistentCopyVisibility"))
    }

    @Test
    fun `reports data class with internal constructor without annotation`() {
        val code = """
            package com.example.domain.user

            data class Email internal constructor(val value: String)
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `reports constructor declared on its own line`() {
        val code = """
            package com.example.domain.user

            data class Email
            private constructor(
                val value: String,
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `reports nested data class with private constructor`() {
        val code = """
            package com.example.domain.order

            sealed interface Order {
                data class Placed private constructor(val total: Long) : Order
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not accept ExposedCopyVisibility`() {
        val code = """
            package com.example.domain.user

            @ExposedCopyVisibility
            data class Email private constructor(val value: String)
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not report data class annotated with ConsistentCopyVisibility`() {
        val code = """
            package com.example.domain.user

            @ConsistentCopyVisibility
            data class Email private constructor(val value: String)
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report fully qualified ConsistentCopyVisibility annotation`() {
        val code = """
            package com.example.domain.user

            @kotlin.ConsistentCopyVisibility
            data class Email private constructor(val value: String)
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report data class with public constructor`() {
        val code = """
            package com.example.domain.user

            data class User(val id: UserId)

            data class Account public constructor(val id: AccountId)
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report internal data class with internal constructor`() {
        val code = """
            package com.example.domain.user

            internal data class Email internal constructor(val value: String)
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report value class or regular class with private constructor`() {
        val code = """
            package com.example.domain.user

            @JvmInline
            value class UserId private constructor(val value: String)

            class Password private constructor(val hash: String)
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report outside domain package`() {
        val code = """
            package com.example.adapter.persistence

            data class UserEntity private constructor(val id: String)
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `supports fully qualified domain package prefix`() {
        val rule = SmartConstructorMustHaveConsistentCopyVisibility(
            TestConfig("domainPackages" to listOf("com.acme.platform.domain"))
        )
        val inDomain = """
            package com.acme.platform.domain.model

            data class Email private constructor(val value: String)
        """.trimIndent()
        val outsideDomain = """
            package com.acme.platform.api.domain

            data class EmailDto private constructor(val value: String)
        """.trimIndent()

        assertEquals(1, rule.lint(inDomain).size)
        assertEquals(0, rule.lint(outsideDomain).size)
    }
}
