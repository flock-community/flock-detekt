package community.flock.detekt.hexagonal.rules.layering

import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DtoOnlyInAdaptersOrApiTest {

    private val rule = DtoOnlyInAdaptersOrApi(Config.empty)

    @Test
    fun `reports UserDto in domain package`() {
        val code = """
            package com.example.domain.user

            data class UserDto(
                val id: String,
                val name: String
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("UserDto"))
        assert(findings[0].message.contains("domain"))
    }

    @Test
    fun `reports CreateUserRequest in domain package`() {
        val code = """
            package com.example.domain

            data class CreateUserRequest(
                val name: String,
                val email: String
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("CreateUserRequest"))
    }

    @Test
    fun `reports UserResponse in domain package`() {
        val code = """
            package com.example.domain.user

            data class UserResponse(
                val id: String,
                val name: String
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("UserResponse"))
    }

    @Test
    fun `does not report UserDto in adapter package`() {
        val code = """
            package com.example.adapter.persistence

            data class UserDto(
                val id: String,
                val name: String
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report UserResponse in api package`() {
        val code = """
            package com.example.api

            data class UserResponse(
                val id: String,
                val name: String
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report UserDto in controller package`() {
        val code = """
            package com.example.controller.user

            data class UserDto(
                val id: String,
                val name: String
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report non-DTO class in domain`() {
        val code = """
            package com.example.domain.user

            data class User(
                val id: String,
                val name: String
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report domain entity ending in similar suffix`() {
        val code = """
            package com.example.domain.user

            data class UserDetails(
                val id: String,
                val email: String
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `reports DTO class in core package`() {
        val code = """
            package com.example.core.user

            data class UserDto(
                val id: String,
                val name: String
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not report DTO in infrastructure package`() {
        val code = """
            package com.example.infrastructure.http

            data class ApiRequest(
                val payload: String
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }
}
