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
 * Rule that prevents domain layer from importing API layer classes.
 * The domain should be independent of how it is exposed (REST, GraphQL, etc.).
 *
 * <noncompliant>
 * // In domain package
 * package com.example.domain.user
 *
 * import com.example.api.UserController  // Domain importing API!
 * import com.example.api.dto.UserRequest  // Domain importing DTO!
 *
 * class UserService {
 *     fun handleRequest(request: UserRequest): User = TODO()  // Wrong!
 * }
 * </noncompliant>
 *
 * <compliant>
 * // In domain package
 * package com.example.domain.user
 *
 * class UserService {
 *     fun createUser(name: UserName, email: Email): User = TODO()  // Uses only domain types
 * }
 * </compliant>
 */
class DomainCannotDependOnApi(config: Config) : Rule(
    config,
    "Domain layer must not depend on API layer. This violates hexagonal architecture principles."
) {

    private val domainPackages: List<String> by config(HexagonalConfig.DEFAULT_DOMAIN_PACKAGES)
    private val apiPackages: List<String> by config(HexagonalConfig.DEFAULT_API_PACKAGES)

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

        if (isApiImport(importPath)) {
            report(
                Finding(
                    Entity.from(importDirective),
                    "Domain layer cannot import from API layer: '$importPath'. " +
                        "The domain should be independent of how it is exposed."
                )
            )
        }

        super.visitImportDirective(importDirective)
    }

    private fun isApiImport(importPath: String): Boolean {
        return apiPackages.any { pattern ->
            importPath.contains(".$pattern.") ||
            importPath.startsWith("$pattern.") ||
            importPath.endsWith(".$pattern")
        }
    }
}
