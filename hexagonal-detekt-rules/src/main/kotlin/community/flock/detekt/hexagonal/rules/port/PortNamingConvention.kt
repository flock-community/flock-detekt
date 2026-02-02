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
 * Rule that enforces naming conventions for port interfaces.
 * Port interfaces should have a suffix that clearly identifies them as ports.
 *
 * <noncompliant>
 * // In a port package
 * package com.example.domain.port
 *
 * interface UserManager {  // Should end with Port, Repository, Gateway, or Client
 *     fun findById(id: UserId): User?
 * }
 * </noncompliant>
 *
 * <compliant>
 * // In a port package
 * package com.example.domain.port
 *
 * interface UserRepository {  // Proper naming
 *     fun findById(id: UserId): User?
 * }
 *
 * interface PaymentGateway {  // Proper naming
 *     fun processPayment(payment: Payment): PaymentResult
 * }
 * </compliant>
 */
class PortNamingConvention(config: Config) : Rule(
    config,
    "Port interfaces should follow naming conventions (end with Port, Repository, Gateway, or Client)."
) {

    private val portPackages: List<String> by config(HexagonalConfig.DEFAULT_PORT_PACKAGES)
    private val allowedSuffixes: List<String> by config(HexagonalConfig.DEFAULT_PORT_SUFFIXES)

    private var isInPort = false

    override fun visitKtFile(file: KtFile) {
        isInPort = HexagonalConfig.fileIsInPort(file, portPackages)
        super.visitKtFile(file)
    }

    override fun visitClass(klass: KtClass) {
        if (!isInPort) {
            super.visitClass(klass)
            return
        }

        // Only check interfaces
        if (!klass.isInterface()) {
            super.visitClass(klass)
            return
        }

        val className = klass.name ?: return

        val hasAllowedSuffix = allowedSuffixes.any { suffix ->
            className.endsWith(suffix)
        }

        if (!hasAllowedSuffix) {
            report(
                Finding(
                    Entity.from(klass),
                    "Port interface '$className' does not follow naming convention. " +
                        "Consider renaming to end with one of: ${allowedSuffixes.joinToString(", ")}."
                )
            )
        }

        super.visitClass(klass)
    }
}
