package community.flock.detekt.hexagonal.rules.adapter

import community.flock.detekt.hexagonal.HexagonalConfig
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

/**
 * Rule that suggests adapter classes should implement a port interface.
 * This ensures adapters are properly connected to the domain through ports.
 *
 * <noncompliant>
 * // In an adapter package
 * package com.example.adapter.persistence
 *
 * class UserRepositoryAdapter {  // Should implement UserRepository port
 *     fun findById(id: UserId): User? = TODO()
 * }
 * </noncompliant>
 *
 * <compliant>
 * // In an adapter package
 * package com.example.adapter.persistence
 *
 * class UserRepositoryAdapter : UserRepository {  // Implements port interface
 *     override fun findById(id: UserId): User? = TODO()
 * }
 * </compliant>
 */
class AdapterMustImplementPort(config: Config) : Rule(
    config,
    "Adapter classes should implement a port interface from the domain layer."
) {

    private val adapterPackages: List<String> by config(HexagonalConfig.DEFAULT_ADAPTER_PACKAGES)
    private val adapterPatterns: List<String> by config(HexagonalConfig.DEFAULT_ADAPTER_PATTERNS)

    private var isInAdapter = false

    override fun visitKtFile(file: KtFile) {
        isInAdapter = HexagonalConfig.fileIsInAdapter(file, adapterPackages)
        super.visitKtFile(file)
    }

    override fun visitClass(klass: KtClass) {
        if (!isInAdapter) {
            super.visitClass(klass)
            return
        }

        val className = klass.name ?: return

        // Skip interfaces, abstract classes, data classes, value classes, enums
        if (klass.isInterface() || klass.isData() || klass.isValue() || klass.isEnum()) {
            super.visitClass(klass)
            return
        }

        // Check if class name matches adapter pattern
        val isAdapterClass = HexagonalConfig.classNameMatchesPattern(className, adapterPatterns)

        if (!isAdapterClass) {
            super.visitClass(klass)
            return
        }

        // Check if the class implements any interface (supertype entries)
        val superTypeList = klass.superTypeListEntries

        if (superTypeList.isEmpty()) {
            report(
                Finding(
                    Entity.from(klass),
                    "Adapter class '$className' does not implement any interface. " +
                        "Adapters should implement a port interface from the domain layer."
                )
            )
        }

        super.visitClass(klass)
    }
}
