package community.flock.detekt.hexagonal.rules.domain

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DomainModelMustBeStandaloneTest {

    private val rule = DomainModelMustBeStandalone(Config.empty)

    @Test
    fun `reports foreign model import in domain package`() {
        val code = """
            package com.example.domain.user

            import com.ahold.gambit.sp.mediapartner.data.GambitBrand

            class UserService
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("com.ahold.gambit.sp.mediapartner.data.GambitBrand"))
        assert(findings[0].message.contains("standalone model"))
    }

    @Test
    fun `does not report own domain model import`() {
        val code = """
            package com.example.domain.user

            import com.example.domain.Brand

            class UserService
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report kotlin stdlib import`() {
        val code = """
            package com.example.domain.user

            import kotlin.collections.List

            class UserService
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report kotlinx import`() {
        val code = """
            package com.example.domain.user

            import kotlinx.coroutines.flow.Flow

            class UserService
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report javax annotation import`() {
        val code = """
            package com.example.domain.user

            import javax.annotation.Nullable

            class UserService
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not allow javax servlet via java prefix`() {
        val code = """
            package com.example.domain.user

            import javax.servlet.http.HttpServletRequest

            class UserService
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not report import outside domain package`() {
        val code = """
            package com.example.adapter.persistence

            import com.ahold.gambit.sp.mediapartner.data.GambitBrand

            class UserRepositoryAdapter
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `matches star imports on their package prefix`() {
        val code = """
            package com.example.domain.user

            import com.ahold.gambit.sp.mediapartner.data.*

            class UserService
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not report star import of own domain package`() {
        val code = """
            package com.example.domain.user

            import com.example.domain.model.*

            class UserService
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `reports multiple foreign imports`() {
        val code = """
            package com.example.domain.user

            import com.ahold.gambit.sp.mediapartner.data.GambitBrand
            import com.thirdparty.other.Thing

            class UserService
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(2, findings.size)
    }

    @Test
    fun `does not report import allowed via additionalAllowedPackages`() {
        val config = TestConfig(
            "additionalAllowedPackages" to listOf("com.example.shared")
        )
        val code = """
            package com.example.domain.user

            import com.example.shared.value.Money

            class UserService
        """.trimIndent()

        val findings = DomainModelMustBeStandalone(config).lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report same-package references without imports`() {
        val code = """
            package com.example.domain.user

            class UserService(val brand: Brand)
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }
}
