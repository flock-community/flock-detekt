package community.flock.detekt.arrow

import org.jetbrains.kotlin.psi.KtFile

/**
 * Shared configuration and utility functions for Arrow typed error handling rules.
 */
object ArrowConfig {

    val DEFAULT_DOMAIN_PACKAGES = listOf("domain", "core")
    val DEFAULT_ADAPTER_PACKAGES = listOf("adapter", "adapters", "infrastructure")
    val DEFAULT_API_PACKAGES = listOf("api", "controller", "controllers", "rest")
    val DEFAULT_SERVICE_SUFFIXES = listOf("Service", "UseCase", "Handler")

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
     * Checks if a class name ends with one of the given suffixes.
     */
    fun classNameHasSuffix(className: String, suffixes: List<String>): Boolean {
        return suffixes.any { suffix -> className.endsWith(suffix) }
    }
}
