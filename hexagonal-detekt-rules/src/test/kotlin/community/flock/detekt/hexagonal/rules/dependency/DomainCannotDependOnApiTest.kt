package community.flock.detekt.hexagonal.rules.dependency

import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DomainCannotDependOnApiTest {

    private val rule = DomainCannotDependOnApi(Config.empty)

    @Test
    fun `reports api import in domain package`() {
        val code = """
            package com.example.domain.user

            import com.example.api.UserController

            class UserService
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("api"))
    }

    @Test
    fun `reports controller import in domain package`() {
        val code = """
            package com.example.domain.user

            import com.example.controller.UserController

            class UserService
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `reports rest import in domain package`() {
        val code = """
            package com.example.domain.user

            import com.example.rest.UserEndpoint

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
    fun `does not report api import outside domain package`() {
        val code = """
            package com.example.adapter.persistence

            import com.example.api.UserController

            class SomeAdapter
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
}
