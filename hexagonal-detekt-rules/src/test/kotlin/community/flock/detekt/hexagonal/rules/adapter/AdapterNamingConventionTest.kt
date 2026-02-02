package community.flock.detekt.hexagonal.rules.adapter

import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AdapterNamingConventionTest {

    private val rule = AdapterNamingConvention(Config.empty)

    @Test
    fun `reports adapter implementation with non-standard name`() {
        val code = """
            package com.example.adapter.persistence

            interface UserRepository {
                fun findById(id: String): String?
            }

            class UserPersistence : UserRepository {
                override fun findById(id: String): String? = null
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("UserPersistence"))
    }

    @Test
    fun `does not report class with Adapter suffix`() {
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
    fun `does not report class with Mock prefix`() {
        val code = """
            package com.example.adapter.persistence

            interface UserRepository {
                fun findById(id: String): String?
            }

            class MockUserRepository : UserRepository {
                override fun findById(id: String): String? = null
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report class with Impl suffix`() {
        val code = """
            package com.example.adapter.persistence

            interface UserRepository {
                fun findById(id: String): String?
            }

            class UserRepositoryImpl : UserRepository {
                override fun findById(id: String): String? = null
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report class not implementing port interface`() {
        val code = """
            package com.example.adapter.persistence

            class UserPersistence {
                fun findById(id: String): String? = null
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report interface in adapter package`() {
        val code = """
            package com.example.adapter.persistence

            interface UserPersistence {
                fun findById(id: String): String?
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report data class in adapter package`() {
        val code = """
            package com.example.adapter.persistence

            interface UserRepository

            data class UserPersistence(val id: String) : UserRepository
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }
}
