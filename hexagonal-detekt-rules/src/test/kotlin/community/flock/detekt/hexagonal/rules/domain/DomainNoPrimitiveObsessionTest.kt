package community.flock.detekt.hexagonal.rules.domain

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DomainNoPrimitiveObsessionTest {

    private val rule = DomainNoPrimitiveObsession(Config.empty)

    @Test
    fun `reports data class with primitive String property in domain package`() {
        val code = """
            package com.example.domain.user

            data class User(
                val id: String,
                val name: String
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(2, findings.size)
    }

    @Test
    fun `reports data class with primitive Int property in domain package`() {
        val code = """
            package com.example.domain.user

            data class User(
                val id: String,
                val age: Int
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(2, findings.size)
    }

    @Test
    fun `does not report data class with value class properties in domain package`() {
        val code = """
            package com.example.domain.user

            @JvmInline
            value class UserId(val value: String)

            @JvmInline
            value class UserName(val value: String)

            data class User(
                val id: UserId,
                val name: UserName
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report data class outside domain package`() {
        val code = """
            package com.example.api.dto

            data class UserDto(
                val id: String,
                val name: String
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report value class itself`() {
        val code = """
            package com.example.domain.user

            @JvmInline
            value class UserId(val value: String)
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `reports nullable primitive types`() {
        val code = """
            package com.example.domain.user

            data class User(
                val nickname: String?
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not report non-data class in domain`() {
        val code = """
            package com.example.domain.user

            class UserService(
                val id: String
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report data class matching exclude pattern`() {
        val ruleWithExclusion = DomainNoPrimitiveObsession(
            TestConfig("excludeClassNamePatterns" to listOf(".*Error$"))
        )

        val code = """
            package com.example.domain.error

            data class ValidationError(
                val message: String,
                val code: Int
            )
        """.trimIndent()

        val findings = ruleWithExclusion.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `reports data class not matching exclude pattern`() {
        val ruleWithExclusion = DomainNoPrimitiveObsession(
            TestConfig("excludeClassNamePatterns" to listOf(".*Error$"))
        )

        val code = """
            package com.example.domain.user

            data class User(
                val id: String,
                val name: String
            )
        """.trimIndent()

        val findings = ruleWithExclusion.lint(code)
        assertEquals(2, findings.size)
    }

    @Test
    fun `supports multiple exclude patterns`() {
        val ruleWithExclusion = DomainNoPrimitiveObsession(
            TestConfig("excludeClassNamePatterns" to listOf(".*Error$", ".*Exception$"))
        )

        val errorCode = """
            package com.example.domain.error

            data class NotFoundError(val message: String)
        """.trimIndent()

        val exceptionCode = """
            package com.example.domain.error

            data class ValidationException(val reason: String)
        """.trimIndent()

        assertEquals(0, ruleWithExclusion.lint(errorCode).size)
        assertEquals(0, ruleWithExclusion.lint(exceptionCode).size)
    }

    @Test
    fun `does not report nested data classes inside excluded sealed interface`() {
        val ruleWithExclusion = DomainNoPrimitiveObsession(
            TestConfig("excludeClassNamePatterns" to listOf(".*Error$"))
        )

        val code = """
            package com.example.domain.error

            sealed interface AdvicePortError {
                data object AgentNoResponse : AdvicePortError
                data class AgentStuck(val reason: String) : AdvicePortError
                data class AgentFatal(val message: String) : AdvicePortError
            }
        """.trimIndent()

        val findings = ruleWithExclusion.lint(code)
        assertEquals(0, findings.size, "Expected no findings but got: ${findings.map { it.message }}")
    }

    @Test
    fun `does not report nested data classes inside excluded sealed class`() {
        val ruleWithExclusion = DomainNoPrimitiveObsession(
            TestConfig("excludeClassNamePatterns" to listOf(".*Error$"))
        )

        val code = """
            package com.example.domain.error

            sealed class GetAdviceError {
                data class DeclarationNotFound(val id: String) : GetAdviceError()
                data class AdviceGenerationFailed(val reason: String) : GetAdviceError()
            }
        """.trimIndent()

        val findings = ruleWithExclusion.lint(code)
        assertEquals(0, findings.size, "Expected no findings but got: ${findings.map { it.message }}")
    }
}
