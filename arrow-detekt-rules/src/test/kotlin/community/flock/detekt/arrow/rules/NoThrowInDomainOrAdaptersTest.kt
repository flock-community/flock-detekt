package community.flock.detekt.arrow.rules

import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NoThrowInDomainOrAdaptersTest {

    private val rule = NoThrowInDomainOrAdapters(Config.empty)

    @Test
    fun `reports throw in domain package`() {
        val code = """
            package com.example.domain.user

            class UserService {
                fun findUser(id: String): User {
                    throw UserNotFoundException(id)
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("Raise"))
    }

    @Test
    fun `reports throw in nested domain package`() {
        val code = """
            package com.example.domain.user.validation

            class UserValidator {
                fun validate(user: User) {
                    throw ValidationException("Invalid")
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `reports throw in adapter package`() {
        val code = """
            package com.example.adapter.persistence

            class UserAdapter {
                fun save(user: User) {
                    throw DatabaseException("Failed")
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `reports throw in infrastructure package`() {
        val code = """
            package com.example.infrastructure.messaging

            class MessageSender {
                fun send(message: String) {
                    throw MessagingException("Failed")
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `reports throw in nested lambda in domain`() {
        val code = """
            package com.example.domain.user

            class UserService {
                fun processUsers(users: List<User>) {
                    users.forEach { user ->
                        if (user.invalid) {
                            throw InvalidUserException(user.id)
                        }
                    }
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not report throw in api package`() {
        val code = """
            package com.example.api.user

            class UserController {
                fun getUser(id: String): User {
                    throw ResponseStatusException(HttpStatus.NOT_FOUND)
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report throw in controller package`() {
        val code = """
            package com.example.controller.user

            class UserController {
                fun getUser(id: String): User {
                    throw ResponseStatusException(HttpStatus.NOT_FOUND)
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report throw in rest package`() {
        val code = """
            package com.example.rest.user

            class UserEndpoint {
                fun getUser(id: String): User {
                    throw ResponseStatusException(HttpStatus.NOT_FOUND)
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report code without throw`() {
        val code = """
            package com.example.domain.user

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
    fun `reports multiple throw expressions`() {
        val code = """
            package com.example.domain.user

            class UserService {
                fun findUser(id: String): User {
                    if (id.isEmpty()) {
                        throw IllegalArgumentException("ID cannot be empty")
                    }
                    throw UserNotFoundException(id)
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(2, findings.size)
    }
}
