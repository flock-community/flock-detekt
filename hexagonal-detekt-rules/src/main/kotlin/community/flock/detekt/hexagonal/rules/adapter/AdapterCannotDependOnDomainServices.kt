package community.flock.detekt.hexagonal.rules.adapter

import community.flock.detekt.hexagonal.HexagonalConfig
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective

/**
 * Rule that prevents adapters from importing domain services.
 *
 * Driven adapters implement ports and translate between the outside world and the domain model;
 * they may depend on ports and models, but not on the services that orchestrate the domain. An
 * adapter that imports a domain service usually re-uses business logic outside the domain, which
 * hides a missing port or a computation that belongs in the domain model.
 *
 * An import counts as a domain service when it points into one of the `domainPackages` and
 * either passes through a package segment listed in `servicePackages`
 * (`com.example.domain.service.PricingService`) or names a type whose name ends with one of the
 * `serviceSuffixes` (`com.example.domain.billing.InvoiceService`). Imports from outside the
 * domain (e.g. `java.util.concurrent.ExecutorService`) are never reported.
 *
 * <noncompliant>
 * // In an adapter package
 * package com.example.adapter.persistence
 *
 * import com.example.domain.service.PricingService  // Domain service in an adapter
 *
 * class OrderRepositoryAdapter(private val pricing: PricingService) : OrderRepository
 * </noncompliant>
 *
 * <compliant>
 * // In an adapter package
 * package com.example.adapter.persistence
 *
 * import com.example.domain.order.Order            // Domain model - OK
 * import com.example.domain.order.OrderRepository  // Port - OK
 *
 * class OrderRepositoryAdapter : OrderRepository
 * </compliant>
 */
class AdapterCannotDependOnDomainServices(config: Config) : Rule(
    config,
    "Adapters should depend on domain ports and models only, not on domain services."
) {

    private val adapterPackages: List<String> by config(HexagonalConfig.DEFAULT_ADAPTER_PACKAGES)
    private val domainPackages: List<String> by config(HexagonalConfig.DEFAULT_DOMAIN_PACKAGES)
    private val servicePackages: List<String> by config(listOf("service", "services"))
    private val serviceSuffixes: List<String> by config(listOf("Service"))

    private var isInAdapter = false

    override fun visitKtFile(file: KtFile) {
        isInAdapter = HexagonalConfig.fileIsInAdapter(file, adapterPackages)
        super.visitKtFile(file)
    }

    override fun visitImportDirective(importDirective: KtImportDirective) {
        if (!isInAdapter) {
            super.visitImportDirective(importDirective)
            return
        }

        val importPath = importDirective.importedFqName?.asString() ?: return

        if (isDomainServiceImport(importPath)) {
            report(
                Finding(
                    Entity.from(importDirective),
                    "Adapter should not import domain service '$importPath'. " +
                        "Adapters may use ports and domain models only; move the logic behind a port " +
                        "or into the domain model."
                )
            )
        }

        super.visitImportDirective(importDirective)
    }

    private fun isDomainServiceImport(importPath: String): Boolean {
        val segments = HexagonalConfig.segmentsAfterPackage(importPath, domainPackages) ?: return false
        return segments.any { segment ->
            segment in servicePackages || serviceSuffixes.any { suffix -> segment.endsWith(suffix) }
        }
    }
}
