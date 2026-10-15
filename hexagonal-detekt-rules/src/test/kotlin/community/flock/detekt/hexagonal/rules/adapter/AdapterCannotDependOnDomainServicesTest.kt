package community.flock.detekt.hexagonal.rules.adapter

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AdapterCannotDependOnDomainServicesTest {

    private val rule = AdapterCannotDependOnDomainServices(Config.empty)

    @Test
    fun `reports import from domain service package`() {
        val code = """
            package com.example.adapter.persistence

            import com.example.domain.service.PricingService

            class OrderRepositoryAdapter(private val pricing: PricingService)
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(1, findings.size)
        assert(findings[0].message.contains("com.example.domain.service.PricingService"))
    }

    @Test
    fun `reports functions, star imports and non-suffixed types from a services package`() {
        val code = """
            package com.example.adapter.billing

            import com.example.domain.services.TokenPricing
            import com.example.domain.services.calculateCost
            import com.example.domain.order.services.*

            class InvoiceAdapter
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(3, findings.size)
    }

    @Test
    fun `reports domain types whose name ends with Service`() {
        val code = """
            package com.example.infrastructure.http

            import com.example.domain.billing.InvoiceService
            import com.example.domain.billing.InvoiceService.Result

            class InvoiceClientAdapter
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(2, findings.size)
    }

    @Test
    fun `does not report ports and models`() {
        val code = """
            package com.example.adapter.persistence

            import com.example.domain.order.Order
            import com.example.domain.order.OrderRepository
            import com.example.domain.port.PaymentGateway

            class OrderRepositoryAdapter : OrderRepository
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report service types from outside the domain`() {
        val code = """
            package com.example.adapter.messaging

            import java.util.concurrent.ExecutorService
            import com.example.adapter.messaging.service.RetryService

            class QueueAdapter(private val executor: ExecutorService)
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not report domain service imports outside adapter packages`() {
        val code = """
            package com.example.api.order

            import com.example.domain.service.PricingService

            class OrderController(private val pricing: PricingService)
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `supports fully qualified package prefixes`() {
        val rule = AdapterCannotDependOnDomainServices(
            TestConfig(
                "adapterPackages" to listOf("com.acme.database", "com.acme.agents"),
                "domainPackages" to listOf("com.acme.platform.domain"),
            )
        )
        val code = """
            package com.acme.agents.fix

            import com.acme.platform.domain.services.RunCostService
            import com.acme.platform.domain.model.Run
            import com.acme.platform.domain.repository.RunRepository

            class CandidateExecutorLive
        """.trimIndent()
        val notAnAdapter = """
            package com.acme.platform.api

            import com.acme.platform.domain.services.RunCostService

            class RunEndpoints
        """.trimIndent()

        assertEquals(1, rule.lint(code).size)
        assertEquals(0, rule.lint(notAnAdapter).size)
    }

    @Test
    fun `supports custom service packages and suffixes`() {
        val rule = AdapterCannotDependOnDomainServices(
            TestConfig(
                "servicePackages" to listOf("usecase"),
                "serviceSuffixes" to listOf("UseCase"),
            )
        )
        val code = """
            package com.example.adapter.persistence

            import com.example.domain.usecase.PlaceOrder
            import com.example.domain.order.CancelOrderUseCase
            import com.example.domain.service.PricingService

            class OrderRepositoryAdapter
        """.trimIndent()

        val findings = rule.lint(code)
        assertEquals(2, findings.size)
    }
}
