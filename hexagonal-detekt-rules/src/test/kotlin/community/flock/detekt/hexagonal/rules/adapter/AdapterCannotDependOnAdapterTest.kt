package community.flock.detekt.hexagonal.rules.adapter

import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AdapterCannotDependOnAdapterTest {

    private val rule = AdapterCannotDependOnAdapter(Config.empty)

    @Test
    fun `reports import from different adapter package`() {
        val code = """
            package com.example.adapter.persistence

            import com.example.adapter.http.HttpClient

            class UserRepositoryAdapter {
                private val client: Any = Any()
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("adapter.http"))
    }

    @Test
    fun `does not report import from same adapter package`() {
        val code = """
            package com.example.adapter.persistence

            import com.example.adapter.persistence.DatabaseConnection

            class UserRepositoryAdapter {
                private val connection: Any = Any()
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report import from domain package`() {
        val code = """
            package com.example.adapter.persistence

            import com.example.domain.user.User

            class UserRepositoryAdapter {
                fun findById(id: String): Any? = null
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report import from kotlin stdlib`() {
        val code = """
            package com.example.adapter.persistence

            import kotlin.collections.List

            class UserRepositoryAdapter {
                fun findAll(): List<Any> = emptyList()
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report cross-adapter import outside adapter package`() {
        val code = """
            package com.example.domain.user

            import com.example.adapter.persistence.UserEntity

            class UserService
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `reports multiple cross-adapter imports`() {
        val code = """
            package com.example.adapter.persistence

            import com.example.adapter.http.HttpClient
            import com.example.adapter.messaging.MessageQueue

            class UserRepositoryAdapter
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(2, findings.size)
    }
}
