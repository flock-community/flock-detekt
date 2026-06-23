package community.flock.detekt.hexagonal.rules.domain

import community.flock.detekt.hexagonal.HexagonalConfig
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective

/**
 * Rule that enforces a self-contained domain model by allow-listing the packages a
 * domain class may import from. Anything outside the allow-list (e.g. a third-party or
 * shared client model package) is flagged, so the domain cannot leak foreign data types
 * into its core model.
 *
 * This is the inverse of [DomainNoFrameworkImports]: instead of denying a fixed set of
 * framework prefixes, it forbids everything that is not explicitly allowed. It is a
 * stricter, opt-in posture (disabled by default) for teams ready to introduce a real
 * anti-corruption boundary around their domain.
 *
 * <noncompliant>
 * // In a domain package
 * package com.example.domain.user
 *
 * import com.ahold.gambit.sp.mediapartner.data.GambitBrand  // Foreign model not allowed
 *
 * class UserService {
 *     // ...
 * }
 * </noncompliant>
 *
 * <compliant>
 * // In a domain package
 * package com.example.domain.user
 *
 * import com.example.domain.Brand  // Own domain model
 * import kotlin.collections.List   // stdlib
 *
 * class UserService {
 *     // ...
 * }
 * </compliant>
 */
class DomainModelMustBeStandalone(config: Config) : Rule(
    config,
    "Domain layer must use a standalone model and only import from allowed domain packages."
) {

    private val domainPackages: List<String> by config(HexagonalConfig.DEFAULT_DOMAIN_PACKAGES)
    private val allowedPackages: List<String> by config(HexagonalConfig.DEFAULT_DOMAIN_ALLOWED_PACKAGES)
    private val additionalAllowedPackages: List<String> by config(emptyList<String>())

    private var isInDomain = false

    override fun visitKtFile(file: KtFile) {
        isInDomain = HexagonalConfig.fileIsInDomain(file, domainPackages)
        super.visitKtFile(file)
    }

    override fun visitImportDirective(importDirective: KtImportDirective) {
        if (!isInDomain) {
            super.visitImportDirective(importDirective)
            return
        }

        val importPath = importDirective.importPath?.pathStr ?: return

        val allowed = HexagonalConfig.isImportAllowed(
            importPath,
            allowedPackages + additionalAllowedPackages,
            domainPackages
        )

        if (!allowed) {
            report(
                Finding(
                    Entity.from(importDirective),
                    "Domain layer must use a standalone model: import '$importPath' " +
                        "is outside the allowed domain packages."
                )
            )
        }

        super.visitImportDirective(importDirective)
    }
}
