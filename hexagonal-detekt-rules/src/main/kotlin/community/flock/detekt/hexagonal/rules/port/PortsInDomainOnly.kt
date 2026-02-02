package community.flock.detekt.hexagonal.rules.port

import community.flock.detekt.hexagonal.HexagonalConfig
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

/**
 * Rule that enforces port interfaces to be defined within the domain layer.
 * Ports are part of the domain's contract and should live in domain packages.
 *
 * <noncompliant>
 * // Port interface defined in adapter package - wrong!
 * package com.example.adapter.persistence
 *
 * interface UserRepository {
 *     fun findById(id: UserId): User?
 * }
 * </noncompliant>
 *
 * <compliant>
 * // Port interface defined in domain package - correct!
 * package com.example.domain.user
 *
 * interface UserRepository {
 *     fun findById(id: UserId): User?
 * }
 * </compliant>
 */
class PortsInDomainOnly(config: Config) : Rule(
    config,
    "Port interfaces should be defined in the domain layer, not in adapters or API layers."
) {

    private val domainPackages: List<String> by config(HexagonalConfig.DEFAULT_DOMAIN_PACKAGES)
    private val adapterPackages: List<String> by config(HexagonalConfig.DEFAULT_ADAPTER_PACKAGES)
    private val apiPackages: List<String> by config(HexagonalConfig.DEFAULT_API_PACKAGES)
    private val portSuffixes: List<String> by config(HexagonalConfig.DEFAULT_PORT_SUFFIXES)

    private var isInAdapter = false
    private var isInApi = false
    private var packageName = ""

    override fun visitKtFile(file: KtFile) {
        packageName = file.packageFqName.asString()
        isInAdapter = HexagonalConfig.fileIsInAdapter(file, adapterPackages)
        isInApi = HexagonalConfig.fileIsInApi(file, apiPackages)
        super.visitKtFile(file)
    }

    override fun visitClass(klass: KtClass) {
        // Only check interfaces
        if (!klass.isInterface()) {
            super.visitClass(klass)
            return
        }

        val className = klass.name ?: return

        // Check if this looks like a port interface
        val looksLikePort = portSuffixes.any { suffix -> className.endsWith(suffix) }

        if (!looksLikePort) {
            super.visitClass(klass)
            return
        }

        // Port-like interface found outside domain
        if (isInAdapter) {
            report(
                Finding(
                    Entity.from(klass),
                    "Port interface '$className' is defined in adapter package '$packageName'. " +
                        "Ports should be defined in the domain layer."
                )
            )
        } else if (isInApi) {
            report(
                Finding(
                    Entity.from(klass),
                    "Port interface '$className' is defined in API package '$packageName'. " +
                        "Ports should be defined in the domain layer."
                )
            )
        }

        super.visitClass(klass)
    }
}
