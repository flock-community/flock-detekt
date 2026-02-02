package community.flock.detekt.hexagonal.rules.port

import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PortsInDomainOnlyTest {

    private val rule = PortsInDomainOnly(Config.empty)

    @Test
    fun `reports port interface in adapter package`() {
        val code = """
            package com.example.adapter.persistence

            interface UserRepository {
                fun findById(id: String): String?
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("adapter"))
    }

    @Test
    fun `reports port interface in api package`() {
        val code = """
            package com.example.api

            interface UserRepository {
                fun findById(id: String): String?
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("API"))
    }

    @Test
    fun `does not report port interface in domain package`() {
        val code = """
            package com.example.domain.user

            interface UserRepository {
                fun findById(id: String): String?
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report non-port interface in adapter package`() {
        val code = """
            package com.example.adapter.persistence

            interface DatabaseConnection {
                fun connect(): Unit
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report class in adapter package`() {
        val code = """
            package com.example.adapter.persistence

            class UserRepository {
                fun findById(id: String): String? = null
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `reports Gateway port in adapter package`() {
        val code = """
            package com.example.adapter.http

            interface PaymentGateway {
                fun process(): Unit
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }
}
