package community.flock.detekt.arrow.rules

import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DomainServiceMustUseRaiseTest {

    private val rule = DomainServiceMustUseRaise(Config.empty)

    @Test
    fun `reports function without Raise context in Service class`() {
        val code = """
            package com.example.domain.user

            class UserService {
                fun createUser(name: String): User {
                    return User(name)
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("createUser"))
        assert(findings[0].message.contains("Raise"))
    }

    @Test
    fun `reports function without Raise context in UseCase class`() {
        val code = """
            package com.example.domain.user

            class CreateUserUseCase {
                fun execute(name: String): User {
                    return User(name)
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `reports function without Raise context in Handler class`() {
        val code = """
            package com.example.domain.user

            class UserCommandHandler {
                fun handle(command: CreateUserCommand): User {
                    return User(command.name)
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not report function with Raise context receiver`() {
        val code = """
            package com.example.domain.user

            import arrow.core.raise.Raise

            class UserService {
                context(Raise<UserError>)
                fun createUser(name: String): User {
                    return User(name)
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report function with named Raise context receiver`() {
        val code = """
            package com.example.domain.user

            import arrow.core.raise.Raise

            class UserService {
                context(raise: Raise<UserError>)
                fun createUser(name: String): User {
                    return User(name)
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report private functions`() {
        val code = """
            package com.example.domain.user

            class UserService {
                private fun validateName(name: String): Boolean {
                    return name.isNotEmpty()
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report interface functions`() {
        val code = """
            package com.example.domain.user

            interface UserService {
                fun createUser(name: String): User
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report functions in non-service classes`() {
        val code = """
            package com.example.domain.user

            data class User(val name: String) {
                fun validate(): Boolean {
                    return name.isNotEmpty()
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report functions outside domain package`() {
        val code = """
            package com.example.api.user

            class UserService {
                fun createUser(name: String): User {
                    return User(name)
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report functions in adapter package`() {
        val code = """
            package com.example.adapter.persistence

            class UserPersistenceService {
                fun saveUser(user: User) {
                    // save to database
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `reports multiple functions without Raise context`() {
        val code = """
            package com.example.domain.user

            class UserService {
                fun createUser(name: String): User {
                    return User(name)
                }

                fun updateUser(id: String, name: String): User {
                    return User(name)
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(2, findings.size)
    }

    @Test
    fun `does not report abstract functions in abstract class`() {
        val code = """
            package com.example.domain.user

            abstract class BaseUserService {
                abstract fun createUser(name: String): User
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `reports concrete functions in abstract class without Raise context`() {
        val code = """
            package com.example.domain.user

            abstract class BaseUserService {
                fun validateUser(user: User): Boolean {
                    return user.name.isNotEmpty()
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }
}
