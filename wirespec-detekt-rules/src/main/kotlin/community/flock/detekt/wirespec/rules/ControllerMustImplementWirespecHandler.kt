package community.flock.detekt.wirespec.rules

import community.flock.detekt.wirespec.WirespecConfig
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

/**
 * Rule that ensures every @RestController class implements a Wirespec-generated Handler interface.
 * This enforces contract-first design where REST endpoints are defined in Wirespec specifications.
 *
 * <noncompliant>
 * // Controller without Wirespec Handler
 * @RestController
 * class UserController {
 *     @GetMapping("/users")
 *     fun getUsers(): List<User> = listOf()
 * }
 * </noncompliant>
 *
 * <compliant>
 * // Controller implementing Wirespec Handler
 * @RestController
 * class UserController : GetUsers.Handler {
 *     override fun getUsers(context: GetUsers.Context): ResponseEntity<List<User>> = TODO()
 * }
 * </compliant>
 */
class ControllerMustImplementWirespecHandler(config: Config) : Rule(
    config,
    "REST controllers should implement a Wirespec-generated Handler interface for contract-first design."
) {

    private val apiPackages: List<String> by config(WirespecConfig.DEFAULT_API_PACKAGES)
    private val controllerAnnotations: List<String> by config(WirespecConfig.DEFAULT_CONTROLLER_ANNOTATIONS)
    private val excludePackages: List<String> by config(WirespecConfig.DEFAULT_EXCLUDE_PACKAGES)

    private var isInApiPackage = false
    private var isInExcludedPackage = false

    override fun visitKtFile(file: KtFile) {
        isInApiPackage = WirespecConfig.fileIsInApi(file, apiPackages)
        isInExcludedPackage = WirespecConfig.fileIsInExcludedPackage(file, excludePackages)
        super.visitKtFile(file)
    }

    override fun visitClass(klass: KtClass) {
        // Skip if in excluded package
        if (isInExcludedPackage) {
            super.visitClass(klass)
            return
        }

        // Skip interfaces
        if (klass.isInterface()) {
            super.visitClass(klass)
            return
        }

        val className = klass.name ?: return

        // Check if class has a controller annotation
        val hasControllerAnnotation = klass.annotationEntries.any { annotation ->
            val annotationName = annotation.shortName?.asString()
            annotationName != null && controllerAnnotations.contains(annotationName)
        }

        if (!hasControllerAnnotation) {
            super.visitClass(klass)
            return
        }

        // Check if the class implements any Handler interface
        val superTypeList = klass.superTypeListEntries
        val implementsHandler = superTypeList.any { superType ->
            val typeText = superType.text
            typeText.endsWith(".Handler") || typeText.endsWith("Handler")
        }

        if (!implementsHandler) {
            report(
                Finding(
                    Entity.from(klass),
                    "Controller '$className' does not implement a Wirespec Handler interface. " +
                        "REST controllers should implement a Handler generated from Wirespec specifications " +
                        "to ensure contract-first design."
                )
            )
        }

        super.visitClass(klass)
    }
}
