package community.flock.detekt.hexagonal.rules.domain

import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DomainNoFrameworkImportsTest {

    private val rule = DomainNoFrameworkImports(Config.empty)

    @Test
    fun `reports Spring import in domain package`() {
        val code = """
            package com.example.domain.user

            import org.springframework.stereotype.Service

            class UserService
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("org.springframework.stereotype.Service"))
    }

    @Test
    fun `reports Jakarta import in domain package`() {
        val code = """
            package com.example.domain.user

            import jakarta.persistence.Entity

            class UserEntity
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `reports Ktor import in domain package`() {
        val code = """
            package com.example.domain.user

            import io.ktor.server.application.Application

            class UserApp
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not report Arrow import in domain package`() {
        val code = """
            package com.example.domain.user

            import arrow.core.Either

            class UserService
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report Kotlin stdlib import in domain package`() {
        val code = """
            package com.example.domain.user

            import kotlin.collections.List

            class UserService
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report framework import outside domain package`() {
        val code = """
            package com.example.adapter.persistence

            import org.springframework.stereotype.Repository

            class UserRepositoryAdapter
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `reports multiple framework imports`() {
        val code = """
            package com.example.domain.user

            import org.springframework.stereotype.Service
            import jakarta.persistence.Entity

            class UserService
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(2, findings.size)
    }
}
