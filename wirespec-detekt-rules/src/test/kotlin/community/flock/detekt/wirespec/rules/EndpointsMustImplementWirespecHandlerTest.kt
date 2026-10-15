package community.flock.detekt.wirespec.rules

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EndpointsMustImplementWirespecHandlerTest {

    private val rule = EndpointsMustImplementWirespecHandler(Config.empty)

    @Test
    fun `reports endpoint class without Handler`() {
        val code = """
            package com.example.api

            class UserEndpoints(private val userService: UserService) {
                suspend fun getUsers(): List<UserDto> = emptyList()
            }

            class HealthEndpoint : Runnable {
                override fun run() = Unit
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(2, findings.size)
        assert(findings[0].message.contains("UserEndpoints"))
        assert(findings[0].message.contains("Wirespec Handler"))
    }

    @Test
    fun `does not report endpoint class implementing Handlers directly`() {
        val code = """
            package com.example.api

            class UserEndpoints(private val userService: UserService) :
                GetUsers.Handler,
                CreateUser.Handler {
                override suspend fun getUsers(request: GetUsers.Request): GetUsers.Response<*> = TODO()
                override suspend fun createUser(request: CreateUser.Request): CreateUser.Response<*> = TODO()
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report endpoint class implementing a Handler aggregate interface`() {
        val code = """
            package com.example.api

            interface UserApi :
                GetUsers.Handler,
                CreateUser.Handler

            interface AdminApi : UserApi, DeleteUser.Handler

            class UserEndpoints(private val userService: UserService) : UserApi {
                override suspend fun getUsers(request: GetUsers.Request): GetUsers.Response<*> = TODO()
                override suspend fun createUser(request: CreateUser.Request): CreateUser.Response<*> = TODO()
            }

            class AdminEndpoints : AdminApi
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `reports endpoint class implementing an interface that is not a Handler aggregate`() {
        val code = """
            package com.example.api

            interface UserApi : GetUsers.Handler, AutoCloseable

            interface Empty

            class UserEndpoints : UserApi

            class OtherEndpoints : Empty
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(2, findings.size)
    }

    @Test
    fun `does not report non-matching names or non-concrete classes`() {
        val code = """
            package com.example.api

            class UserMapper

            interface UserEndpoints

            abstract class BaseEndpoints

            data class WebhookEndpoint(val url: String)

            enum class Endpoint { A, B }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report outside API packages or in excluded packages`() {
        val domain = """
            package com.example.domain

            class UserEndpoints
        """.trimIndent()
        val actuator = """
            package com.example.api.actuator

            class StatusEndpoints
        """.trimIndent()

        assertEquals(0, rule.lint(domain).size)
        assertEquals(0, rule.lint(actuator).size)
    }

    @Test
    fun `supports allowed supertypes`() {
        val rule = EndpointsMustImplementWirespecHandler(
            TestConfig("allowedSupertypes" to listOf("io\\.ktor\\..*", "Legacy.*"))
        )
        val code = """
            package com.example.api

            class LegacyEndpoints : LegacyApi
            class PluginEndpoints : io.ktor.server.application.BaseApplicationPlugin<Unit, Unit, Unit>
            class OtherEndpoints : Runnable
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("OtherEndpoints"))
    }

    @Test
    fun `supports custom class patterns and fully qualified api packages`() {
        val rule = EndpointsMustImplementWirespecHandler(
            TestConfig(
                "apiPackages" to listOf("com.acme.platform.api"),
                "endpointClassPatterns" to listOf(".*Routes"),
            )
        )
        val code = """
            package com.acme.platform.api.frontend

            class UserRoutes
            class UserEndpoints
        """.trimIndent()
        val outsideApi = """
            package com.acme.platform.platform

            class UserRoutes
        """.trimIndent()

        assertEquals(1, rule.lint(code).size)
        assertEquals(0, rule.lint(outsideApi).size)
    }
}
