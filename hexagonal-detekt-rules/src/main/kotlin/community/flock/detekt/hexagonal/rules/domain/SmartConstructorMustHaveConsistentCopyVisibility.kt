package community.flock.detekt.hexagonal.rules.domain

import community.flock.detekt.hexagonal.HexagonalConfig
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

/**
 * Rule that requires domain data classes with a non-public primary constructor to be annotated
 * with `@ConsistentCopyVisibility`.
 *
 * A non-public primary constructor is how a smart constructor
 * (`companion object { operator fun invoke(...) }`) forces every instance through its validation
 * and derived-field computation. The `copy()` function the compiler generates for a data class is
 * public by default, though, so `instance.copy(...)` creates an instance that never went through
 * the smart constructor. `@ConsistentCopyVisibility` gives `copy()` the same visibility as the
 * primary constructor. `@ExposedCopyVisibility` deliberately keeps the public `copy()` and
 * therefore does not satisfy this rule.
 *
 * Only data classes are checked: value classes and regular classes have no generated `copy()`.
 * Modules compiled with `-Xconsistent-data-class-copy-visibility` already get consistent `copy()`
 * visibility for every data class and can leave this rule disabled.
 *
 * <noncompliant>
 * // In a domain package
 * data class Email private constructor(val value: String, val domain: String) {
 *     companion object {
 *         operator fun invoke(value: String): Email =
 *             Email(value, value.substringAfter("@"))
 *     }
 * }
 *
 * // Bypasses the smart constructor: 'domain' no longer matches 'value'
 * val broken = Email("a@example.com").copy(value = "b@other.org")
 * </noncompliant>
 *
 * <compliant>
 * // In a domain package
 * @ConsistentCopyVisibility
 * data class Email private constructor(val value: String, val domain: String) {
 *     companion object {
 *         operator fun invoke(value: String): Email =
 *             Email(value, value.substringAfter("@"))
 *     }
 * }
 * </compliant>
 */
class SmartConstructorMustHaveConsistentCopyVisibility(config: Config) : Rule(
    config,
    "Domain data classes with a non-public constructor must be annotated with @ConsistentCopyVisibility " +
        "so the generated copy() cannot bypass the smart constructor."
) {

    private val domainPackages: List<String> by config(HexagonalConfig.DEFAULT_DOMAIN_PACKAGES)

    private var isInDomain = false

    override fun visitKtFile(file: KtFile) {
        isInDomain = HexagonalConfig.fileIsInDomain(file, domainPackages)
        super.visitKtFile(file)
    }

    override fun visitClass(klass: KtClass) {
        if (isInDomain && klass.isData() && copyBypassesConstructor(klass) && !hasConsistentCopyVisibility(klass)) {
            report(
                Finding(
                    Entity.atName(klass),
                    "Data class '${klass.name}' has a non-public primary constructor, but its generated " +
                        "copy() is as visible as the class and bypasses the smart constructor. " +
                        "Annotate the class with @ConsistentCopyVisibility."
                )
            )
        }

        super.visitClass(klass)
    }

    /**
     * The generated `copy()` has the visibility of the class, so it only bypasses the constructor
     * when the constructor is less visible than the class itself.
     */
    private fun copyBypassesConstructor(klass: KtClass): Boolean {
        val constructor = klass.primaryConstructor ?: return false
        return when {
            constructor.hasModifier(KtTokens.PRIVATE_KEYWORD) -> true
            constructor.hasModifier(KtTokens.PROTECTED_KEYWORD) -> true
            constructor.hasModifier(KtTokens.INTERNAL_KEYWORD) ->
                !klass.hasModifier(KtTokens.INTERNAL_KEYWORD) && !klass.hasModifier(KtTokens.PRIVATE_KEYWORD)
            else -> false
        }
    }

    private fun hasConsistentCopyVisibility(klass: KtClass): Boolean =
        klass.annotationEntries.any { it.shortName?.asString() == "ConsistentCopyVisibility" }
}
