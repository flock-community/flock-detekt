package community.flock.detekt.hexagonal.rules.layering

import community.flock.detekt.hexagonal.HexagonalConfig
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

/**
 * Rule that enforces service classes to be defined only in the domain (or application) layer.
 * Services contain business logic and should not exist in API or adapter layers.
 *
 * <noncompliant>
 * // Service class defined in API package - wrong!
 * package com.example.api.user
 *
 * class UserService {
 *     fun createUser(name: String): User = ...
 * }
 * </noncompliant>
 *
 * <compliant>
 * // Service class defined in domain package - correct!
 * package com.example.domain.user
 *
 * class UserService {
 *     fun createUser(name: String): User = ...
 * }
 * </compliant>
 */
class NoServiceInApiOrAdapter(config: Config) : Rule(
    config,
    "Service classes should not be in API or adapter layers."
) {

    private val apiPackages: List<String> by config(HexagonalConfig.DEFAULT_API_PACKAGES)
    private val adapterPackages: List<String> by config(HexagonalConfig.DEFAULT_ADAPTER_PACKAGES)
    private val serviceSuffixes: List<String> by config(listOf("Service"))

    private var isInApi = false
    private var isInAdapter = false
    private var packageName = ""

    override fun visitKtFile(file: KtFile) {
        packageName = file.packageFqName.asString()
        isInApi = HexagonalConfig.fileIsInApi(file, apiPackages)
        isInAdapter = HexagonalConfig.fileIsInAdapter(file, adapterPackages)
        super.visitKtFile(file)
    }

    override fun visitClass(klass: KtClass) {
        // Only check classes, not interfaces
        if (klass.isInterface()) {
            super.visitClass(klass)
            return
        }

        if (!isInApi && !isInAdapter) {
            super.visitClass(klass)
            return
        }

        val className = klass.name ?: return

        val isService = serviceSuffixes.any { suffix -> className.endsWith(suffix) }

        if (isService) {
            val layer = if (isInApi) "API" else "adapter"
            report(
                Finding(
                    Entity.from(klass),
                    "Service class '$className' is defined in $layer package '$packageName'. " +
                        "Services should be in the domain layer."
                )
            )
        }

        super.visitClass(klass)
    }
}
