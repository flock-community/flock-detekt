package community.flock.detekt.arrow.rules

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CatchAllMustRethrowCancellationTest {

    private val rule = CatchAllMustRethrowCancellation(Config.empty)

    @Test
    fun `reports catch-all in suspend function`() {
        val code = """
            package com.example.domain.user

            suspend fun loadProfile(id: UserId): Profile? =
                try {
                    client.fetchProfile(id)
                } catch (e: Exception) {
                    null
                }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("'Exception'"))
        assert(findings[0].message.contains("CancellationException"))
    }

    @Test
    fun `reports every catch-all type in a suspend lambda inside a suspend function`() {
        val code = """
            package com.example.adapter.http

            class ProfileClient {
                suspend fun load() {
                    withContext(Dispatchers.IO) {
                        try { fetch() } catch (t: Throwable) { log(t) }
                        try { fetch() } catch (e: RuntimeException) { log(e) }
                        try { fetch() } catch (e: IllegalStateException) { log(e) }
                        try { fetch() } catch (e: java.lang.Exception) { log(e) }
                    }
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(4, findings.size)
    }

    @Test
    fun `reports CancellationException clause that does not rethrow`() {
        val code = """
            package com.example.domain.user

            suspend fun run() {
                try {
                    work()
                } catch (e: CancellationException) {
                    null
                } catch (e: Throwable) {
                    log(e)
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("not rethrown"))
    }

    @Test
    fun `reports conditional rethrow that is not about cancellation`() {
        val code = """
            package com.example.domain.user

            suspend fun run() {
                try {
                    work()
                } catch (e: Exception) {
                    if (e is IOException) throw e
                    if (e !is CancellationException) log(e)
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not report catch-all preceded by rethrowing CancellationException clause`() {
        val code = """
            package com.example.domain.user

            import kotlin.coroutines.cancellation.CancellationException

            suspend fun describe(): Description? =
                try {
                    describer.describe()
                } catch (e: NotFound) {
                    null
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }

            suspend fun describeQualified(): Description? =
                try {
                    describer.describe()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    logger.info("cancelled")
                    throw e
                } catch (e: Exception) {
                    null
                }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report catch-all that rethrows cancellation itself`() {
        val code = """
            package com.example.domain.user

            suspend fun a() {
                try { work() } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    log(e)
                }
            }

            suspend fun b() {
                try { work() } catch (e: Throwable) {
                    when (e) {
                        is CancellationException -> throw e
                        else -> log(e)
                    }
                }
            }

            suspend fun c() {
                try { work() } catch (e: Exception) {
                    if (e is CancellationException || e is Error) {
                        throw e
                    }
                }
            }

            suspend fun d() {
                try { work() } catch (e: Exception) {
                    log(e)
                    throw e
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report catch-all that checks for cancellation`() {
        val code = """
            package com.example.domain.user

            suspend fun a() {
                try { work() } catch (e: Exception) {
                    currentCoroutineContext().ensureActive()
                    log(e)
                }
            }

            suspend fun b(): Outcome =
                try { work() } catch (e: Throwable) {
                    Outcome.Failed(e.nonFatalOrThrow())
                }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report specific exception types`() {
        val code = """
            package com.example.domain.user

            suspend fun run() {
                try { work() } catch (e: IOException) { log(e) }
                try { work() } catch (e: IllegalArgumentException) { log(e) }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report catch-all outside suspend functions`() {
        val code = """
            package com.example.domain.user

            fun parse(raw: String): Int? =
                try { raw.toInt() } catch (e: Exception) { null }

            suspend fun outer() {
                fun inner(): Int? = try { compute() } catch (e: Exception) { null }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `supports custom catch-all types and check calls`() {
        val rule = CatchAllMustRethrowCancellation(
            TestConfig(
                "catchAllTypes" to listOf("Exception"),
                "cancellationCheckCalls" to listOf("rethrowIfCancelled"),
            )
        )
        val code = """
            package com.example.domain.user

            suspend fun a() {
                try { work() } catch (t: Throwable) { log(t) }
            }

            suspend fun b() {
                try { work() } catch (e: Exception) { e.rethrowIfCancelled() }
            }

            suspend fun c() {
                try { work() } catch (e: Exception) { currentCoroutineContext().ensureActive() }
            }
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }
}
