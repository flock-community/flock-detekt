package community.flock.detekt.hexagonal.rules.dependency

import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ApiCannotDependOnAdaptersTest {

    private val rule = ApiCannotDependOnAdapters(Config.empty)

    @Test
    fun `reports adapter import in api package`() {
        val code = """
            package com.example.api

            import com.example.adapter.persistence.UserRepositoryAdapter

            class UserController
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("adapter.persistence"))
    }

    @Test
    fun `reports infrastructure import in api package`() {
        val code = """
            package com.example.api

            import com.example.infrastructure.database.ConnectionPool

            class UserController
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not report domain import in api package`() {
        val code = """
            package com.example.api

            import com.example.domain.user.UserRepository

            class UserController
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report adapter import outside api package`() {
        val code = """
            package com.example.app

            import com.example.adapter.persistence.UserRepositoryAdapter

            class Application
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `reports adapter import in controller package`() {
        val code = """
            package com.example.controller

            import com.example.adapter.persistence.UserRepositoryAdapter

            class UserController
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `reports adapter import in rest package`() {
        val code = """
            package com.example.rest

            import com.example.adapter.http.HttpClientAdapter

            class UserEndpoint
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not report kotlin stdlib import in api package`() {
        val code = """
            package com.example.api

            import kotlin.collections.List

            class UserController
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }
}
