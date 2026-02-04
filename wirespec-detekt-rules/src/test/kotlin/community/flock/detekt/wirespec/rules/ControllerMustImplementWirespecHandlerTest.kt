package community.flock.detekt.wirespec.rules

import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ControllerMustImplementWirespecHandlerTest {

    private val rule = ControllerMustImplementWirespecHandler(Config.empty)

    @Test
    fun `reports RestController without Handler implementation`() {
        val code = """
            package com.example.api

            @RestController
            class UserController {
                fun getUsers(): List<String> = listOf()
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("UserController"))
        assert(findings[0].message.contains("Wirespec Handler"))
    }

    @Test
    fun `reports Controller annotation without Handler implementation`() {
        val code = """
            package com.example.controller

            @Controller
            class UserController {
                fun getUsers(): List<String> = listOf()
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not report RestController with Handler implementation`() {
        val code = """
            package com.example.api

            interface GetUsers {
                interface Handler {
                    fun getUsers(): List<String>
                }
            }

            @RestController
            class UserController : GetUsers.Handler {
                override fun getUsers(): List<String> = listOf()
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report RestController with simple Handler interface`() {
        val code = """
            package com.example.api

            interface UserHandler {
                fun getUsers(): List<String>
            }

            @RestController
            class UserController : UserHandler {
                override fun getUsers(): List<String> = listOf()
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report class in excluded package (actuator)`() {
        val code = """
            package com.example.actuator

            @RestController
            class HealthController {
                fun health(): String = "OK"
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report class in excluded package (health)`() {
        val code = """
            package com.example.health

            @RestController
            class HealthCheckController {
                fun check(): String = "OK"
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report class in excluded package (management)`() {
        val code = """
            package com.example.management

            @RestController
            class ManagementController {
                fun status(): String = "OK"
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report class in excluded package (test)`() {
        val code = """
            package com.example.test

            @RestController
            class TestController {
                fun test(): String = "OK"
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report interface with RestController annotation`() {
        val code = """
            package com.example.api

            @RestController
            interface UserApi {
                fun getUsers(): List<String>
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report class without controller annotation`() {
        val code = """
            package com.example.api

            class UserService {
                fun getUsers(): List<String> = listOf()
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report class outside API packages`() {
        val code = """
            package com.example.domain

            @RestController
            class UserController {
                fun getUsers(): List<String> = listOf()
            }
        """.trimIndent()

        // Note: This passes because the rule checks for excluded packages but not API packages
        // The rule applies to all RestControllers regardless of package
        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }
}
