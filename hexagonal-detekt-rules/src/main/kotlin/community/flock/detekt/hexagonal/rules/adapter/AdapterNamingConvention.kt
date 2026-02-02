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
 * Rule that enforces naming conventions for adapter classes.
 * Adapter classes should follow patterns like *Adapter, Mock*, or *Impl.
 *
 * <noncompliant>
 * // In an adapter package
 * package com.example.adapter.persistence
 *
 * class UserPersistence : UserRepository {  // Should be named UserRepositoryAdapter
 *     override fun findById(id: UserId): User? = TODO()
 * }
 * </noncompliant>
 *
 * <compliant>
 * // In an adapter package
 * package com.example.adapter.persistence
 *
 * class UserRepositoryAdapter : UserRepository {  // Proper naming
 *     override fun findById(id: UserId): User? = TODO()
 * }
 *
 * class MockUserRepository : UserRepository {  // Also valid
 *     override fun findById(id: UserId): User? = TODO()
 * }
 * </compliant>
 */
class AdapterNamingConvention(config: Config) : Rule(
    config,
    "Adapter classes should follow naming conventions (*Adapter, Mock*, *Impl)."
) {

    private val adapterPackages: List<String> by config(HexagonalConfig.DEFAULT_ADAPTER_PACKAGES)
    private val allowedPatterns: List<String> by config(HexagonalConfig.DEFAULT_ADAPTER_PATTERNS)
    private val portSuffixes: List<String> by config(HexagonalConfig.DEFAULT_PORT_SUFFIXES)

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

        // Skip interfaces, data classes, value classes, sealed classes, enums
        if (klass.isInterface() || klass.isData() || klass.isValue() || klass.isSealed() || klass.isEnum()) {
            super.visitClass(klass)
            return
        }

        // Check if class implements a port (has supertype entries)
        val implementsPort = klass.superTypeListEntries.any { superType ->
            val typeName = superType.text
            portSuffixes.any { suffix -> typeName.contains(suffix) }
        }

        // Only check naming convention if the class implements a port-like interface
        if (!implementsPort) {
            super.visitClass(klass)
            return
        }

        val matchesPattern = HexagonalConfig.classNameMatchesPattern(className, allowedPatterns)

        if (!matchesPattern) {
            report(
                Finding(
                    Entity.from(klass),
                    "Adapter class '$className' does not follow naming conventions. " +
                        "Consider renaming to match one of: ${allowedPatterns.joinToString(", ")}."
                )
            )
        }

        super.visitClass(klass)
    }
}
