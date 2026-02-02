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
 * Rule that prevents domain layer from importing framework-specific classes.
 * The domain layer should remain pure and framework-agnostic.
 *
 * <noncompliant>
 * // In a domain package
 * package com.example.domain.user
 *
 * import org.springframework.stereotype.Service  // Framework import not allowed
 * import jakarta.persistence.Entity              // Framework import not allowed
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
 * import arrow.core.Either  // Allowed - not a framework
 *
 * class UserService {
 *     // ...
 * }
 * </compliant>
 */
class DomainNoFrameworkImports(config: Config) : Rule(
    config,
    "Domain layer should not import framework-specific classes to maintain domain purity."
) {

    private val domainPackages: List<String> by config(HexagonalConfig.DEFAULT_DOMAIN_PACKAGES)
    private val forbiddenImports: List<String> by config(HexagonalConfig.DEFAULT_FORBIDDEN_FRAMEWORK_IMPORTS)

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

        if (HexagonalConfig.isImportForbidden(importPath, forbiddenImports)) {
            report(
                Finding(
                    Entity.from(importDirective),
                    "Domain layer should not import '$importPath'. " +
                        "Framework-specific dependencies violate hexagonal architecture principles."
                )
            )
        }

        super.visitImportDirective(importDirective)
    }
}
