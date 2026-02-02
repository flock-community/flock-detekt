package community.flock.detekt.hexagonal.rules.dependency

import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DomainCannotDependOnAdaptersTest {

    private val rule = DomainCannotDependOnAdapters(Config.empty)

    @Test
    fun `reports adapter import in domain package`() {
        val code = """
            package com.example.domain.user

            import com.example.adapter.persistence.UserEntity

            class UserService
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("adapter.persistence"))
    }

    @Test
    fun `reports infrastructure import in domain package`() {
        val code = """
            package com.example.domain.user

            import com.example.infrastructure.database.Connection

            class UserService
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not report domain import in domain package`() {
        val code = """
            package com.example.domain.user

            import com.example.domain.common.DomainEvent

            class UserService
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report kotlin stdlib import in domain package`() {
        val code = """
            package com.example.domain.user

            import kotlin.collections.List

            class UserService
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report adapter import outside domain package`() {
        val code = """
            package com.example.api

            import com.example.adapter.persistence.UserEntity

            class UserController
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `reports multiple adapter imports in domain`() {
        val code = """
            package com.example.domain.user

            import com.example.adapter.persistence.UserEntity
            import com.example.adapter.http.HttpClient

            class UserService
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(2, findings.size)
    }
}
