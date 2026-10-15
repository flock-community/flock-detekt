package community.flock.detekt.wirespec.rules

import community.flock.detekt.wirespec.WirespecConfig
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * Rule that ensures endpoint classes implement a Wirespec-generated Handler interface.
 *
 * In a Ktor application, Wirespec handlers are plain classes mounted with
 * `Route.wirespec(handler)`; there is no `@RestController` annotation to anchor
 * [ControllerMustImplementWirespecHandler] on. This rule uses the class name instead: concrete
 * classes in API packages whose name matches `endpointClassPatterns` (default `*Endpoints` and
 * `*Endpoint`) must implement at least one Wirespec handler. Interfaces, abstract, sealed, data,
 * value and enum classes are not checked.
 *
 * A class implements a handler when one of its supertypes ends with `Handler` (e.g.
 * `GetUsers.Handler`), or when it implements an interface declared in the same file whose
 * supertypes are all handlers. The latter is the common way to group many endpoints:
 * `interface UserApi : GetUsers.Handler, CreateUser.Handler` and
 * `class UserEndpoints(...) : UserApi`. Supertypes matching one of the `allowedSupertypes`
 * regular expressions also satisfy the rule.
 *
 * <noncompliant>
 * // In an API package
 * class UserEndpoints(private val userService: UserService) {
 *     suspend fun getUsers(): List<UserDto> = userService.findAll().map { it.toDto() }
 * }
 * </noncompliant>
 *
 * <compliant>
 * // In an API package
 * interface UserApi :
 *     GetUsers.Handler,
 *     CreateUser.Handler
 *
 * class UserEndpoints(private val userService: UserService) : UserApi {
 *     override suspend fun getUsers(request: GetUsers.Request): GetUsers.Response<*> = TODO()
 *     override suspend fun createUser(request: CreateUser.Request): CreateUser.Response<*> = TODO()
 * }
 * </compliant>
 */
class EndpointsMustImplementWirespecHandler(config: Config) : Rule(
    config,
    "Endpoint classes should implement a Wirespec-generated Handler interface for contract-first design."
) {

    private val apiPackages: List<String> by config(WirespecConfig.DEFAULT_API_PACKAGES)
    private val excludePackages: List<String> by config(WirespecConfig.DEFAULT_EXCLUDE_PACKAGES)
    private val endpointClassPatterns: List<Regex> by config(WirespecConfig.DEFAULT_ENDPOINT_CLASS_PATTERNS) {
        it.map(::Regex)
    }
    private val allowedSupertypes: List<Regex> by config(emptyList<String>()) { it.map(::Regex) }

    private var shouldCheck = false
    private var handlerAggregates: Set<String> = emptySet()

    override fun visitKtFile(file: KtFile) {
        shouldCheck = WirespecConfig.fileIsInApi(file, apiPackages) &&
            !WirespecConfig.fileIsInExcludedPackage(file, excludePackages)
        handlerAggregates = if (shouldCheck) findHandlerAggregates(file) else emptySet()
        super.visitKtFile(file)
    }

    override fun visitClass(klass: KtClass) {
        val className = klass.name
        if (shouldCheck && className != null && isConcreteEndpointClass(klass, className)) {
            val supertypes = supertypeNames(klass)
            val implementsHandler = supertypes.any { supertype ->
                isHandler(supertype) ||
                    supertype in handlerAggregates ||
                    allowedSupertypes.any { it.matches(supertype) }
            }

            if (!implementsHandler) {
                report(
                    Finding(
                        Entity.atName(klass),
                        "Endpoint class '$className' does not implement a Wirespec Handler interface. " +
                            "Implement the Handlers generated from the Wirespec specification and mount " +
                            "the class with wirespec(...) to keep the API contract-first."
                    )
                )
            }
        }

        super.visitClass(klass)
    }

    private fun isConcreteEndpointClass(klass: KtClass, className: String): Boolean =
        !klass.isInterface() &&
            !klass.hasModifier(KtTokens.ABSTRACT_KEYWORD) &&
            !klass.isSealed() &&
            !klass.isData() &&
            !klass.isValue() &&
            !klass.isEnum() &&
            !klass.isAnnotation() &&
            endpointClassPatterns.any { it.matches(className) }

    /**
     * Interfaces declared in [file] that only extend handlers (directly or through other such
     * interfaces in the same file).
     */
    private fun findHandlerAggregates(file: KtFile): Set<String> {
        val interfaces = file.collectDescendantsOfType<KtClass> { it.isInterface() && it.name != null }
            .associate { it.name!! to supertypeNames(it) }

        val aggregates = mutableSetOf<String>()
        var changed = true
        while (changed) {
            changed = false
            interfaces.forEach { (name, supertypes) ->
                val isAggregate = supertypes.isNotEmpty() &&
                    supertypes.all { isHandler(it) || it in aggregates }
                if (name !in aggregates && isAggregate) {
                    aggregates += name
                    changed = true
                }
            }
        }
        return aggregates
    }

    private fun supertypeNames(classOrObject: KtClassOrObject): List<String> =
        classOrObject.superTypeListEntries.mapNotNull { entry ->
            (entry.typeReference?.typeElement?.unwrapNullable() as? KtUserType)?.qualifiedName()
        }

    private fun isHandler(supertype: String): Boolean = supertype.endsWith("Handler")

    private fun KtTypeElement.unwrapNullable(): KtTypeElement? =
        if (this is KtNullableType) innerType?.unwrapNullable() else this

    private fun KtUserType.qualifiedName(): String {
        val name = referencedName.orEmpty()
        return qualifier?.let { "${it.qualifiedName()}.$name" } ?: name
    }
}
