package community.flock.detekt.wirespec.rules

import community.flock.detekt.wirespec.WirespecConfig
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Rule that prevents manual Spring mapping annotations on controller methods.
 * Mapping annotations should only appear on methods that override Wirespec Handler interfaces.
 *
 * <noncompliant>
 * @RestController
 * class UserController {
 *     @GetMapping("/users")
 *     fun getUsers(): List<User> = listOf()  // Manual annotation, not from Wirespec
 * }
 * </noncompliant>
 *
 * <compliant>
 * @RestController
 * class UserController : GetUsers.Handler {
 *     @GetMapping("/users")
 *     override fun getUsers(context: GetUsers.Context): ResponseEntity<List<User>> = TODO()
 * }
 * </compliant>
 */
class NoManualSpringMappingAnnotations(config: Config) : Rule(
    config,
    "Spring mapping annotations should only appear on override methods inherited from Wirespec Handlers."
) {

    private val apiPackages: List<String> by config(WirespecConfig.DEFAULT_API_PACKAGES)
    private val mappingAnnotations: List<String> by config(WirespecConfig.DEFAULT_MAPPING_ANNOTATIONS)
    private val excludePackages: List<String> by config(WirespecConfig.DEFAULT_EXCLUDE_PACKAGES)
    private val allowOnOverrides: Boolean by config(true)

    private var isInApiPackage = false
    private var isInExcludedPackage = false

    override fun visitKtFile(file: KtFile) {
        isInApiPackage = WirespecConfig.fileIsInApi(file, apiPackages)
        isInExcludedPackage = WirespecConfig.fileIsInExcludedPackage(file, excludePackages)
        super.visitKtFile(file)
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        // Skip if in excluded package
        if (isInExcludedPackage) {
            super.visitNamedFunction(function)
            return
        }

        // Skip if not in API package
        if (!isInApiPackage) {
            super.visitNamedFunction(function)
            return
        }

        val functionName = function.name ?: return

        // Check if method has any mapping annotation
        val mappingAnnotation = function.annotationEntries.find { annotation ->
            val annotationName = annotation.shortName?.asString()
            annotationName != null && mappingAnnotations.contains(annotationName)
        }

        if (mappingAnnotation == null) {
            super.visitNamedFunction(function)
            return
        }

        // Check if method has override modifier
        val hasOverrideModifier = function.hasModifier(KtTokens.OVERRIDE_KEYWORD)

        // If allowOnOverrides is true and the method is an override, it's OK
        if (allowOnOverrides && hasOverrideModifier) {
            super.visitNamedFunction(function)
            return
        }

        // Report violation: mapping annotation on non-override method
        val annotationName = mappingAnnotation.shortName?.asString() ?: "mapping"
        report(
            Finding(
                Entity.from(function),
                "Method '$functionName' has @$annotationName but is not an override method. " +
                    "Spring mapping annotations should only be used on methods that override " +
                    "Wirespec Handler interfaces to ensure contract-first design."
            )
        )

        super.visitNamedFunction(function)
    }
}
