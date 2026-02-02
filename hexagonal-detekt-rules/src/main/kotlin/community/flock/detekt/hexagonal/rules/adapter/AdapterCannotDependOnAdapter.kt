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
 * Rule that prevents adapter packages from importing other adapter packages.
 * Adapters should communicate through the domain layer, not directly with each other.
 *
 * <noncompliant>
 * // In persistence adapter
 * package com.example.adapter.persistence
 *
 * import com.example.adapter.http.HttpClient  // Cross-adapter dependency!
 *
 * class UserRepositoryAdapter : UserRepository {
 *     private val httpClient = HttpClient()  // Wrong!
 * }
 * </noncompliant>
 *
 * <compliant>
 * // In persistence adapter
 * package com.example.adapter.persistence
 *
 * import com.example.domain.user.UserRepository  // Domain dependency - OK
 *
 * class UserRepositoryAdapter : UserRepository {
 *     // Uses only domain types and its own adapter infrastructure
 * }
 * </compliant>
 */
class AdapterCannotDependOnAdapter(config: Config) : Rule(
    config,
    "Adapters should not depend on other adapters. Communicate through the domain layer."
) {

    private val adapterPackages: List<String> by config(HexagonalConfig.DEFAULT_ADAPTER_PACKAGES)

    private var isInAdapter = false
    private var currentAdapterPackage: String? = null

    override fun visitKtFile(file: KtFile) {
        val packageName = file.packageFqName.asString()
        isInAdapter = HexagonalConfig.fileIsInAdapter(file, adapterPackages)

        // Extract the specific adapter package this file belongs to
        currentAdapterPackage = if (isInAdapter) {
            extractAdapterSubPackage(packageName)
        } else {
            null
        }

        super.visitKtFile(file)
    }

    override fun visitImportDirective(importDirective: KtImportDirective) {
        if (!isInAdapter) {
            super.visitImportDirective(importDirective)
            return
        }

        val importPath = importDirective.importPath?.pathStr ?: return

        // Check if the import is from another adapter package
        if (isFromDifferentAdapterPackage(importPath)) {
            report(
                Finding(
                    Entity.from(importDirective),
                    "Adapter should not import from another adapter package: '$importPath'. " +
                        "Adapters should communicate through the domain layer."
                )
            )
        }

        super.visitImportDirective(importDirective)
    }

    private fun extractAdapterSubPackage(packageName: String): String? {
        // Find which adapter package pattern matches and extract the specific adapter
        for (adapterPattern in adapterPackages) {
            val adapterIndex = packageName.indexOf(".$adapterPattern.")
            if (adapterIndex >= 0) {
                val afterAdapter = packageName.substring(adapterIndex + adapterPattern.length + 2)
                val subPackage = afterAdapter.substringBefore(".")
                return "$adapterPattern.$subPackage"
            }

            // Check if package ends with adapter pattern
            if (packageName.endsWith(".$adapterPattern")) {
                return adapterPattern
            }

            // Check for pattern at start
            if (packageName.startsWith("$adapterPattern.")) {
                val subPackage = packageName.substringAfter("$adapterPattern.").substringBefore(".")
                return if (subPackage.isNotEmpty()) "$adapterPattern.$subPackage" else adapterPattern
            }
        }
        return null
    }

    private fun isFromDifferentAdapterPackage(importPath: String): Boolean {
        // Check if import is from any adapter package
        val isAdapterImport = adapterPackages.any { pattern ->
            importPath.contains(".$pattern.") ||
            importPath.startsWith("$pattern.") ||
            importPath.endsWith(".$pattern")
        }

        if (!isAdapterImport) {
            return false
        }

        // Check if it's from the same adapter sub-package
        val importAdapterPackage = extractAdapterSubPackage(importPath)

        return importAdapterPackage != null && importAdapterPackage != currentAdapterPackage
    }
}
