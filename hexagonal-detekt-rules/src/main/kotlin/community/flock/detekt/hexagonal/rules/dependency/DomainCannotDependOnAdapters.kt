package community.flock.detekt.hexagonal.rules.dependency

import community.flock.detekt.hexagonal.HexagonalConfig
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective

/**
 * Rule that prevents domain layer from importing adapter layer classes.
 * This is a core hexagonal architecture constraint - the domain should be independent of adapters.
 *
 * <noncompliant>
 * // In domain package
 * package com.example.domain.user
 *
 * import com.example.adapter.persistence.UserEntity  // Domain importing adapter!
 *
 * class UserService {
 *     fun convert(entity: UserEntity): User = TODO()  // Wrong!
 * }
 * </noncompliant>
 *
 * <compliant>
 * // In domain package
 * package com.example.domain.user
 *
 * class UserService {
 *     fun findUser(id: UserId): User = TODO()  // Uses only domain types
 * }
 * </compliant>
 */
class DomainCannotDependOnAdapters(config: Config) : Rule(
    config,
    "Domain layer must not depend on adapter layer. This violates hexagonal architecture principles."
) {

    private val domainPackages: List<String> by config(HexagonalConfig.DEFAULT_DOMAIN_PACKAGES)
    private val adapterPackages: List<String> by config(HexagonalConfig.DEFAULT_ADAPTER_PACKAGES)

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

        if (isAdapterImport(importPath)) {
            report(
                Finding(
                    Entity.from(importDirective),
                    "Domain layer cannot import from adapter layer: '$importPath'. " +
                        "The domain should be independent of infrastructure concerns."
                )
            )
        }

        super.visitImportDirective(importDirective)
    }

    private fun isAdapterImport(importPath: String): Boolean {
        return adapterPackages.any { pattern ->
            importPath.contains(".$pattern.") ||
            importPath.startsWith("$pattern.") ||
            importPath.endsWith(".$pattern")
        }
    }
}
