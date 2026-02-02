package community.flock.detekt.hexagonal.rules.port

import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PortNamingConventionTest {

    private val rule = PortNamingConvention(Config.empty)

    @Test
    fun `reports interface without allowed suffix in port package`() {
        val code = """
            package com.example.domain.port

            interface UserManager {
                fun findById(id: String): String?
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("UserManager"))
    }

    @Test
    fun `does not report interface with Port suffix`() {
        val code = """
            package com.example.domain.port

            interface UserPort {
                fun findById(id: String): String?
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report interface with Repository suffix`() {
        val code = """
            package com.example.domain.port

            interface UserRepository {
                fun findById(id: String): String?
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report interface with Gateway suffix`() {
        val code = """
            package com.example.domain.port

            interface PaymentGateway {
                fun processPayment(): Unit
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report interface with Client suffix`() {
        val code = """
            package com.example.domain.port

            interface HttpClient {
                fun get(url: String): String
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report class in port package`() {
        val code = """
            package com.example.domain.port

            class UserManager {
                fun findById(id: String): String? = null
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report interface outside port package`() {
        val code = """
            package com.example.domain.user

            interface UserManager {
                fun findById(id: String): String?
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }
}
