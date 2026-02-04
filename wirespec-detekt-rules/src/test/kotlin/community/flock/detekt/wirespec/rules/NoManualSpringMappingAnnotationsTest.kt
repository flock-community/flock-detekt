package community.flock.detekt.wirespec.rules

import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NoManualSpringMappingAnnotationsTest {

    private val rule = NoManualSpringMappingAnnotations(Config.empty)

    @Test
    fun `reports GetMapping on non-override method`() {
        val code = """
            package com.example.api

            @RestController
            class UserController {
                @GetMapping("/users")
                fun getUsers(): List<String> = listOf()
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("getUsers"))
        assert(findings[0].message.contains("@GetMapping"))
    }

    @Test
    fun `reports PostMapping on non-override method`() {
        val code = """
            package com.example.controller

            @RestController
            class UserController {
                @PostMapping("/users")
                fun createUser(user: String): String = user
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("createUser"))
        assert(findings[0].message.contains("@PostMapping"))
    }

    @Test
    fun `reports PutMapping on non-override method`() {
        val code = """
            package com.example.api

            class UserController {
                @PutMapping("/users/{id}")
                fun updateUser(id: String, user: String): String = user
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `reports DeleteMapping on non-override method`() {
        val code = """
            package com.example.api

            class UserController {
                @DeleteMapping("/users/{id}")
                fun deleteUser(id: String): Unit = Unit
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `reports PatchMapping on non-override method`() {
        val code = """
            package com.example.api

            class UserController {
                @PatchMapping("/users/{id}")
                fun patchUser(id: String, patch: String): String = patch
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `reports RequestMapping on non-override method`() {
        val code = """
            package com.example.rest

            class UserController {
                @RequestMapping("/users")
                fun handleUsers(): String = "OK"
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not report GetMapping on override method`() {
        val code = """
            package com.example.api

            interface GetUsers {
                interface Handler {
                    fun getUsers(): List<String>
                }
            }

            @RestController
            class UserController : GetUsers.Handler {
                @GetMapping("/users")
                override fun getUsers(): List<String> = listOf()
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report PostMapping on override method`() {
        val code = """
            package com.example.api

            interface UserApi {
                fun createUser(user: String): String
            }

            @RestController
            class UserController : UserApi {
                @PostMapping("/users")
                override fun createUser(user: String): String = user
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report method in excluded package (actuator)`() {
        val code = """
            package com.example.actuator

            @RestController
            class HealthController {
                @GetMapping("/health")
                fun health(): String = "OK"
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report method in excluded package (health)`() {
        val code = """
            package com.example.health

            @RestController
            class HealthController {
                @GetMapping("/check")
                fun check(): String = "OK"
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report method in excluded package (management)`() {
        val code = """
            package com.example.management

            @RestController
            class AdminController {
                @GetMapping("/status")
                fun status(): String = "OK"
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report method in excluded package (test)`() {
        val code = """
            package com.example.test

            @RestController
            class TestController {
                @GetMapping("/test")
                fun test(): String = "OK"
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report method without mapping annotation`() {
        val code = """
            package com.example.api

            @RestController
            class UserController {
                fun getUsers(): List<String> = listOf()
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report method outside API packages`() {
        val code = """
            package com.example.domain

            class UserService {
                @GetMapping("/users")
                fun getUsers(): List<String> = listOf()
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `reports multiple mapping annotations on different methods`() {
        val code = """
            package com.example.api

            @RestController
            class UserController {
                @GetMapping("/users")
                fun getUsers(): List<String> = listOf()

                @PostMapping("/users")
                fun createUser(user: String): String = user
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(2, findings.size)
    }
}
