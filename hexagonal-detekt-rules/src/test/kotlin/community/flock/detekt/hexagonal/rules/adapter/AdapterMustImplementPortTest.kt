package community.flock.detekt.hexagonal.rules.adapter

import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AdapterMustImplementPortTest {

    private val rule = AdapterMustImplementPort(Config.empty)

    @Test
    fun `reports adapter class without interface implementation`() {
        val code = """
            package com.example.adapter.persistence

            class UserRepositoryAdapter {
                fun findById(id: String): String? = null
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("UserRepositoryAdapter"))
    }

    @Test
    fun `reports Mock class without interface implementation`() {
        val code = """
            package com.example.adapter.persistence

            class MockUserRepository {
                fun findById(id: String): String? = null
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not report adapter class with interface implementation`() {
        val code = """
            package com.example.adapter.persistence

            interface UserRepository {
                fun findById(id: String): String?
            }

            class UserRepositoryAdapter : UserRepository {
                override fun findById(id: String): String? = null
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report interface in adapter package`() {
        val code = """
            package com.example.adapter.persistence

            interface DatabaseAdapter {
                fun connect(): Unit
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report data class in adapter package`() {
        val code = """
            package com.example.adapter.persistence

            data class UserAdapter(val id: String)
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report non-adapter named class in adapter package`() {
        val code = """
            package com.example.adapter.persistence

            class DatabaseConnection {
                fun connect(): Unit = Unit
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report adapter class outside adapter package`() {
        val code = """
            package com.example.domain.user

            class UserRepositoryAdapter {
                fun findById(id: String): String? = null
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }
}
