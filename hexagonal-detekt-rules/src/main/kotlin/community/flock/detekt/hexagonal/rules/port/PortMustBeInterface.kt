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
 * Rule that enforces port types to be interfaces.
 * Ports define contracts and should not contain implementation.
 *
 * <noncompliant>
 * // In a port package
 * package com.example.domain.port
 *
 * class UserRepository {  // Should be an interface
 *     fun findById(id: UserId): User? = null
 * }
 * </noncompliant>
 *
 * <compliant>
 * // In a port package
 * package com.example.domain.port
 *
 * interface UserRepository {
 *     fun findById(id: UserId): User?
 * }
 * </compliant>
 */
class PortMustBeInterface(config: Config) : Rule(
    config,
    "Port types must be interfaces. Ports define contracts without implementation."
) {

    private val portPackages: List<String> by config(HexagonalConfig.DEFAULT_PORT_PACKAGES)
    private val portSuffixes: List<String> by config(HexagonalConfig.DEFAULT_PORT_SUFFIXES)

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

        val className = klass.name ?: return

        // Skip if it's already an interface
        if (klass.isInterface()) {
            super.visitClass(klass)
            return
        }

        // Skip value classes, data classes, sealed classes, enums - these are valid domain types
        if (klass.isValue() || klass.isData() || klass.isSealed() || klass.isEnum()) {
            super.visitClass(klass)
            return
        }

        // Check if the class has a port suffix (indicating it should be a port)
        val hasPortSuffix = portSuffixes.any { suffix -> className.endsWith(suffix) }

        if (hasPortSuffix) {
            report(
                Finding(
                    Entity.from(klass),
                    "Class '$className' appears to be a port but is not an interface. " +
                        "Ports should be interfaces to define contracts without implementation."
                )
            )
        }

        super.visitClass(klass)
    }
}
