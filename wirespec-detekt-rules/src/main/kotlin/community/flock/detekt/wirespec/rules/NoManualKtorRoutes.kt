package community.flock.detekt.wirespec.rules

import community.flock.detekt.wirespec.WirespecConfig
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtThisExpression

/**
 * Rule that prevents hand-written Ktor routes in API packages.
 *
 * With Wirespec's Ktor integration, endpoints are generated from the contract and mounted by
 * passing a `Wirespec.Handler` implementation to `Route.wirespec(handler)`. A route declared by
 * hand (`get("/users") { ... }`) bypasses the contract: it is missing from the specification,
 * from the generated clients and from contract tests. This is the Ktor counterpart of
 * [NoManualSpringMappingAnnotations].
 *
 * Reported are calls with an implicit receiver to one of the `routeBuilders` (Ktor's routing DSL)
 * whose first argument is a string literal or template, i.e. a hard-coded path. Qualified calls
 * such as `client.get("/users")` or `headers.get("X-Id")` are not routing DSL calls and are
 * ignored, as are calls whose path is not a literal, such as
 * `route(endpoint.pathTemplate, method)` inside the glue code that mounts Wirespec handlers.
 * Paths that Wirespec cannot model (e.g. a Server-Sent Events stream) can be listed verbatim in
 * `allowedPaths`.
 *
 * <noncompliant>
 * // In an API package
 * fun Application.configureRouting() {
 *     routing {
 *         get("/users") {  // hand-written route, not in the Wirespec contract
 *             call.respond(userService.findAll())
 *         }
 *     }
 * }
 * </noncompliant>
 *
 * <compliant>
 * // In an API package
 * class UserEndpoints(private val userService: UserService) : GetUsers.Handler {
 *     override suspend fun getUsers(request: GetUsers.Request): GetUsers.Response<*> =
 *         GetUsers.Response200(userService.findAll().map { it.toDto() })
 * }
 *
 * fun Application.configureRouting() {
 *     routing {
 *         wirespec(UserEndpoints(userService))
 *     }
 * }
 * </compliant>
 */
class NoManualKtorRoutes(config: Config) : Rule(
    config,
    "Ktor routes in API packages should be generated from Wirespec contracts, not declared by hand."
) {

    private val apiPackages: List<String> by config(WirespecConfig.DEFAULT_API_PACKAGES)
    private val excludePackages: List<String> by config(WirespecConfig.DEFAULT_EXCLUDE_PACKAGES)
    private val routeBuilders: List<String> by config(WirespecConfig.DEFAULT_KTOR_ROUTE_BUILDERS)
    private val allowedPaths: List<String> by config(emptyList<String>())

    private var shouldCheck = false

    override fun visitKtFile(file: KtFile) {
        shouldCheck = WirespecConfig.fileIsInApi(file, apiPackages) &&
            !WirespecConfig.fileIsInExcludedPackage(file, excludePackages)
        super.visitKtFile(file)
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        if (shouldCheck) {
            val builder = expression.calleeExpression?.text
            val path = expression.valueArguments.firstOrNull()?.getArgumentExpression() as? KtStringTemplateExpression

            if (builder != null && builder in routeBuilders && path != null && hasImplicitReceiver(expression)) {
                val pathText = path.entries.joinToString("") { it.text }
                if (pathText !in allowedPaths) {
                    report(
                        Finding(
                            Entity.from(expression),
                            "Hand-written Ktor route $builder(\"$pathText\") is not part of the Wirespec contract. " +
                                "Define the endpoint in a Wirespec specification and mount its Handler with " +
                                "wirespec(...), or add the path to allowedPaths if Wirespec cannot model it."
                        )
                    )
                }
            }
        }

        super.visitCallExpression(expression)
    }

    private fun hasImplicitReceiver(expression: KtCallExpression): Boolean {
        val qualified = expression.parent as? KtQualifiedExpression ?: return true
        if (qualified.selectorExpression != expression) return true
        return qualified.receiverExpression is KtThisExpression
    }
}
