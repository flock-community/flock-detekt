package community.flock.detekt.hexagonal.rules.port

import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PortMustBeInterfaceTest {

    private val rule = PortMustBeInterface(Config.empty)

    @Test
    fun `reports class with Port suffix in port package`() {
        val code = """
            package com.example.domain.port

            class UserPort {
                fun findById(id: String): String? = null
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("UserPort"))
    }

    @Test
    fun `reports class with Repository suffix in port package`() {
        val code = """
            package com.example.domain.port

            class UserRepository {
                fun findById(id: String): String? = null
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
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
    fun `does not report class without port suffix in port package`() {
        val code = """
            package com.example.domain.port

            class SomeHelper {
                fun help(): Unit = Unit
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report data class in port package`() {
        val code = """
            package com.example.domain.port

            data class UserRepository(val id: String)
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report value class in port package`() {
        val code = """
            package com.example.domain.port

            @JvmInline
            value class UserRepository(val value: String)
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report class outside port package`() {
        val code = """
            package com.example.domain.user

            class UserRepository {
                fun findById(id: String): String? = null
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }
}
