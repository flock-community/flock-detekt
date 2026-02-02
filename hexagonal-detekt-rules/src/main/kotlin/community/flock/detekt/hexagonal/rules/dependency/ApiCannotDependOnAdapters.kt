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
 * Rule that prevents API layer from importing adapter implementations directly.
 * The API layer should depend on domain interfaces (ports), not concrete adapters.
 *
 * <noncompliant>
 * // In API package
 * package com.example.api
 *
 * import com.example.adapter.persistence.UserRepositoryAdapter  // API importing adapter!
 *
 * class UserController(
 *     private val repository: UserRepositoryAdapter  // Direct adapter dependency!
 * ) {
 *     // ...
 * }
 * </noncompliant>
 *
 * <compliant>
 * // In API package
 * package com.example.api
 *
 * import com.example.domain.user.UserRepository  // Import port interface
 *
 * class UserController(
 *     private val repository: UserRepository  // Depends on port, not adapter
 * ) {
 *     // ...
 * }
 * </compliant>
 */
class ApiCannotDependOnAdapters(config: Config) : Rule(
    config,
    "API layer should not depend on adapter implementations. Use port interfaces from the domain."
) {

    private val apiPackages: List<String> by config(HexagonalConfig.DEFAULT_API_PACKAGES)
    private val adapterPackages: List<String> by config(HexagonalConfig.DEFAULT_ADAPTER_PACKAGES)

    private var isInApi = false

    override fun visitKtFile(file: KtFile) {
        isInApi = HexagonalConfig.fileIsInApi(file, apiPackages)
        super.visitKtFile(file)
    }

    override fun visitImportDirective(importDirective: KtImportDirective) {
        if (!isInApi) {
            super.visitImportDirective(importDirective)
            return
        }

        val importPath = importDirective.importPath?.pathStr ?: return

        if (isAdapterImport(importPath)) {
            report(
                Finding(
                    Entity.from(importDirective),
                    "API layer should not import from adapter layer: '$importPath'. " +
                        "Use port interfaces from the domain layer instead."
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
