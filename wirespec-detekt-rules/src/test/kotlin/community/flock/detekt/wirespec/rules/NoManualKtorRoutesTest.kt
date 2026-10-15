package community.flock.detekt.wirespec.rules

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NoManualKtorRoutesTest {

    private val rule = NoManualKtorRoutes(Config.empty)

    @Test
    fun `reports hand-written routes with literal paths`() {
        val code = """
            package com.example.api

            fun Application.configureRouting() {
                routing {
                    get("/users") { call.respond(emptyList<String>()) }
                    post("/users") { call.respond(HttpStatusCode.Created) }
                    route("/admin") {
                        delete("/cache") { call.respond(HttpStatusCode.NoContent) }
                    }
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(4, findings.size)
        assert(findings[0].message.contains("get(\"/users\")"))
        assert(findings[0].message.contains("Wirespec"))
    }

    @Test
    fun `reports sse, webSocket and templated paths`() {
        val code = """
            package com.example.api.frontend

            fun Route.events(prefix: String) {
                sse("/agents/{agentId}/events") { send(ServerSentEvent("ping")) }
                webSocket("/ws") { }
                get("${'$'}prefix/users") { }
                this.put("/users/{id}") { }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(4, findings.size)
    }

    @Test
    fun `does not report qualified calls`() {
        val code = """
            package com.example.api

            suspend fun fetch(client: HttpClient, call: ApplicationCall) {
                client.get("/users")
                client.post("/users") { setBody("x") }
                val id = call.request.headers.get("X-Id")
                val agentId = call.parameters["agentId"]
                cache?.delete("key")
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report routes without a literal path`() {
        val code = """
            package com.example.api.wirespec

            fun Route.wirespec(api: Wirespec.Handler) {
                route(instance.pathTemplate, HttpMethod.parse(instance.method)) {
                    handle { }
                }
                get { call.respond("root") }
                route(path = pathOf(api)) { }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report wirespec mounting`() {
        val code = """
            package com.example.api

            fun Application.configureRouting(userEndpoints: UserEndpoints) {
                routing {
                    authenticate {
                        wirespec(userEndpoints)
                    }
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report allowed paths`() {
        val rule = NoManualKtorRoutes(
            TestConfig("allowedPaths" to listOf("/agents/{agentId}/events"))
        )
        val code = """
            package com.example.api

            fun Route.events() {
                sse("/agents/{agentId}/events") { }
                sse("/agents/{agentId}/other") { }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not report routes outside API packages or in excluded packages`() {
        val platform = """
            package com.example.platform

            fun Application.module() {
                routing { get("/") { call.respondText("ok") } }
            }
        """.trimIndent()
        val health = """
            package com.example.api.health

            fun Route.health() {
                get("/health") { call.respondText("ok") }
            }
        """.trimIndent()

        assertEquals(0, rule.lint(platform).size)
        assertEquals(0, rule.lint(health).size)
    }

    @Test
    fun `supports custom route builders and fully qualified api packages`() {
        val rule = NoManualKtorRoutes(
            TestConfig(
                "apiPackages" to listOf("com.acme.platform.api"),
                "routeBuilders" to listOf("staticResources"),
            )
        )
        val code = """
            package com.acme.platform.api.frontend

            fun Route.assets() {
                staticResources("/static", "web")
                get("/users") { }
            }
        """.trimIndent()
        val outsideApi = """
            package com.acme.platform.platform

            fun Route.assets() {
                staticResources("/static", "web")
            }
        """.trimIndent()

        assertEquals(1, rule.lint(code).size)
        assertEquals(0, rule.lint(outsideApi).size)
    }
}
