package community.flock.detekt.hexagonal.rules.domain

import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DomainMustBeImmutableTest {

    private val rule = DomainMustBeImmutable(Config.empty)

    @Test
    fun `reports var property in domain data class`() {
        val code = """
            package com.example.domain.user

            data class User(
                val id: String,
                var name: String
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("name"))
    }

    @Test
    fun `reports var property in domain class`() {
        val code = """
            package com.example.domain.user

            class UserService {
                var currentUser: String? = null
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not report val property in domain data class`() {
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
    fun `does not report var outside domain package`() {
        val code = """
            package com.example.adapter.persistence

            class UserRepository {
                var cache: MutableMap<String, String> = mutableMapOf()
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `reports multiple var properties`() {
        val code = """
            package com.example.domain.user

            data class User(
                var id: String,
                var name: String
            )
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(2, findings.size)
    }

    @Test
    fun `does not report val in value class`() {
        val code = """
            package com.example.domain.user

            @JvmInline
            value class UserId(val value: String)
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }
}
