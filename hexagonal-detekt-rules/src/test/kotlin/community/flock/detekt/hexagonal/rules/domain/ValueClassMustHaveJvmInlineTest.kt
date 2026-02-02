package community.flock.detekt.hexagonal.rules.domain

import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ValueClassMustHaveJvmInlineTest {

    private val rule = ValueClassMustHaveJvmInline(Config.empty)

    @Test
    fun `reports value class without JvmInline annotation`() {
        val code = """
            package com.example.domain.user

            value class UserId(val value: String)
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("UserId"))
        assert(findings[0].message.contains("@JvmInline"))
    }

    @Test
    fun `does not report value class with JvmInline annotation`() {
        val code = """
            package com.example.domain.user

            @JvmInline
            value class UserId(val value: String)
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report data class`() {
        val code = """
            package com.example.domain.user

            data class User(val id: String)
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report regular class`() {
        val code = """
            package com.example.domain.user

            class UserService
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `reports multiple value classes without JvmInline`() {
        val code = """
            package com.example.domain.user

            value class UserId(val value: String)
            value class UserName(val value: String)
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(2, findings.size)
    }

    @Test
    fun `reports value class outside domain package by default`() {
        val code = """
            package com.example.api.dto

            value class UserId(val value: String)
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }
}
