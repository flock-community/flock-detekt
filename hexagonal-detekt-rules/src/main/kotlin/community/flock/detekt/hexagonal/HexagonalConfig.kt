package community.flock.detekt.hexagonal

import org.jetbrains.kotlin.psi.KtFile

/**
 * Shared configuration and utility functions for hexagonal architecture rules.
 */
object HexagonalConfig {

    val DEFAULT_DOMAIN_PACKAGES = listOf("domain", "core")
    val DEFAULT_PORT_PACKAGES = listOf("port", "ports")
    val DEFAULT_ADAPTER_PACKAGES = listOf("adapter", "adapters", "infrastructure")
    val DEFAULT_API_PACKAGES = listOf("api", "controller", "controllers", "rest")

    val DEFAULT_PORT_SUFFIXES = listOf("Port", "Repository", "Gateway", "Client")
    val DEFAULT_ADAPTER_PATTERNS = listOf(".*Adapter", "Mock.*", ".*Impl", ".*Client")

    val DEFAULT_FORBIDDEN_FRAMEWORK_IMPORTS = listOf(
        "org.springframework",
        "io.ktor",
        "jakarta",
        "javax.servlet",
        "io.micronaut",
        "io.quarkus",
        "org.hibernate",
        "org.jooq"
    )

    val DEFAULT_DOMAIN_ALLOWED_PACKAGES = listOf(
        "kotlin",
        "kotlinx",
        "java",
        "javax.annotation"
    )

    /**
     * Checks if the given file belongs to a package matching one of the patterns.
     */
    fun fileIsInPackage(file: KtFile, packagePatterns: List<String>): Boolean {
        val packageName = file.packageFqName.asString()
        return packagePatterns.any { pattern ->
            packageName.contains(".$pattern.") ||
                packageName.endsWith(".$pattern") ||
                packageName == pattern ||
                packageName.startsWith("$pattern.")
        }
    }

    /**
     * Checks if the given file is in the domain layer.
     */
    fun fileIsInDomain(file: KtFile, domainPackages: List<String> = DEFAULT_DOMAIN_PACKAGES): Boolean {
        return fileIsInPackage(file, domainPackages)
    }

    /**
     * Checks if the given file is in the adapter layer.
     */
    fun fileIsInAdapter(file: KtFile, adapterPackages: List<String> = DEFAULT_ADAPTER_PACKAGES): Boolean {
        return fileIsInPackage(file, adapterPackages)
    }

    /**
     * Checks if the given file is in the API layer.
     */
    fun fileIsInApi(file: KtFile, apiPackages: List<String> = DEFAULT_API_PACKAGES): Boolean {
        return fileIsInPackage(file, apiPackages)
    }

    /**
     * Checks if the given file is in a port package.
     */
    fun fileIsInPort(file: KtFile, portPackages: List<String> = DEFAULT_PORT_PACKAGES): Boolean {
        return fileIsInPackage(file, portPackages)
    }

    /**
     * Checks if an import statement matches a forbidden pattern.
     */
    fun isImportForbidden(importPath: String, forbiddenPatterns: List<String>): Boolean {
        return forbiddenPatterns.any { pattern ->
            importPath.startsWith(pattern) || importPath.startsWith("$pattern.")
        }
    }

    /**
     * Checks if an import statement is allowed for a standalone domain model.
     *
     * An import is allowed when its fully qualified name starts with one of the
     * [allowedPackages] prefixes (matched on package-segment boundaries), or when it
     * targets one of the [domainPackages] (the domain's own model is always allowed).
     *
     * This is the allow-list counterpart of [isImportForbidden]: everything that is not
     * explicitly allowed is considered foreign to the domain.
     */
    fun isImportAllowed(
        importPath: String,
        allowedPackages: List<String>,
        domainPackages: List<String>
    ): Boolean {
        val allowedByPrefix = allowedPackages.any { pattern ->
            importPath == pattern || importPath.startsWith("$pattern.")
        }
        if (allowedByPrefix) return true

        return domainPackages.any { pattern ->
            importPath.contains(".$pattern.") ||
                importPath.endsWith(".$pattern") ||
                importPath.startsWith("$pattern.") ||
                importPath == pattern
        }
    }

    /**
     * Checks if a class name matches one of the given patterns.
     */
    fun classNameMatchesPattern(className: String, patterns: List<String>): Boolean {
        return patterns.any { pattern ->
            val regex = pattern.replace("*", ".*").toRegex()
            regex.matches(className)
        }
    }
}
