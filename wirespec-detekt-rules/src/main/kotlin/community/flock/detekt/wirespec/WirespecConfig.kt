package community.flock.detekt.wirespec

import org.jetbrains.kotlin.psi.KtFile

/**
 * Shared configuration and utility functions for Wirespec contract-first rules.
 */
object WirespecConfig {

    val DEFAULT_API_PACKAGES = listOf("api", "controller", "controllers", "rest")
    val DEFAULT_CONTROLLER_ANNOTATIONS = listOf("RestController", "Controller")
    val DEFAULT_MAPPING_ANNOTATIONS = listOf(
        "GetMapping",
        "PostMapping",
        "PutMapping",
        "DeleteMapping",
        "PatchMapping",
        "RequestMapping"
    )
    val DEFAULT_EXCLUDE_PACKAGES = listOf("actuator", "health", "management", "test")

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
     * Checks if the given file is in the API/controller layer.
     */
    fun fileIsInApi(file: KtFile, apiPackages: List<String> = DEFAULT_API_PACKAGES): Boolean {
        return fileIsInPackage(file, apiPackages)
    }

    /**
     * Checks if the given file is in an excluded package.
     */
    fun fileIsInExcludedPackage(file: KtFile, excludePackages: List<String> = DEFAULT_EXCLUDE_PACKAGES): Boolean {
        return fileIsInPackage(file, excludePackages)
    }
}
