package community.flock.detekt.hexagonal.rules.domain

import community.flock.detekt.hexagonal.HexagonalConfig
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty

/**
 * Rule that enforces immutability in domain classes by requiring `val` instead of `var`.
 *
 * <noncompliant>
 * // In a domain package
 * data class User(
 *     val id: UserId,
 *     var name: UserName  // Mutable property not allowed
 * )
 *
 * class UserService {
 *     var currentUser: User? = null  // Mutable property not allowed
 * }
 * </noncompliant>
 *
 * <compliant>
 * // In a domain package
 * data class User(
 *     val id: UserId,
 *     val name: UserName  // Immutable
 * )
 *
 * class UserService {
 *     val users: List<User> = emptyList()  // Immutable
 * }
 * </compliant>
 */
class DomainMustBeImmutable(config: Config) : Rule(
    config,
    "Domain classes must be immutable. Use 'val' instead of 'var'."
) {

    private val domainPackages: List<String> by config(HexagonalConfig.DEFAULT_DOMAIN_PACKAGES)

    private var isInDomain = false

    override fun visitKtFile(file: KtFile) {
        isInDomain = HexagonalConfig.fileIsInDomain(file, domainPackages)
        super.visitKtFile(file)
    }

    override fun visitClass(klass: KtClass) {
        if (!isInDomain) {
            super.visitClass(klass)
            return
        }

        // Check primary constructor parameters
        klass.primaryConstructor?.valueParameters?.forEach { param ->
            if (param.isMutable) {
                report(
                    Finding(
                        Entity.from(param),
                        "Property '${param.name}' in class '${klass.name}' uses 'var'. " +
                            "Domain classes should be immutable - use 'val' instead."
                    )
                )
            }
        }

        super.visitClass(klass)
    }

    override fun visitProperty(property: KtProperty) {
        if (!isInDomain) {
            super.visitProperty(property)
            return
        }

        // Check if the property is mutable (uses 'var')
        if (property.isVar) {
            report(
                Finding(
                    Entity.from(property),
                    "Property '${property.name}' uses 'var'. " +
                        "Domain classes should be immutable - use 'val' instead."
                )
            )
        }

        super.visitProperty(property)
    }
}
