package community.flock.detekt.hexagonal.rules.domain

import community.flock.detekt.hexagonal.HexagonalConfig
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtParameter

/**
 * Rule that enforces domain data classes to use value classes instead of primitives.
 *
 * <noncompliant>
 * // In a domain package
 * data class User(
 *     val id: String,           // Primitive obsession - should use value class
 *     val email: String,        // Primitive obsession - should use value class
 *     val age: Int              // Primitive obsession - should use value class
 * )
 * </noncompliant>
 *
 * <compliant>
 * @JvmInline
 * value class UserId(val value: String)
 *
 * @JvmInline
 * value class Email(val value: String)
 *
 * @JvmInline
 * value class Age(val value: Int)
 *
 * data class User(
 *     val id: UserId,
 *     val email: Email,
 *     val age: Age
 * )
 * </compliant>
 */
class DomainNoPrimitiveObsession(config: Config) : Rule(
    config,
    "Domain data classes should use value classes instead of primitives to prevent primitive obsession."
) {

    private val domainPackages: List<String> by config(HexagonalConfig.DEFAULT_DOMAIN_PACKAGES)
    private val allowedPrimitives: List<String> by config(emptyList())
    private val excludeClassNamePatterns: List<String> by config(emptyList())
    private val primitiveTypes = setOf(
        "String", "Int", "Long", "Double", "Float", "Boolean", "Byte", "Short", "Char",
        "kotlin.String", "kotlin.Int", "kotlin.Long", "kotlin.Double", "kotlin.Float",
        "kotlin.Boolean", "kotlin.Byte", "kotlin.Short", "kotlin.Char"
    )

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

        // Only check data classes (not value classes themselves)
        if (klass.isData() && !isExcluded(klass)) {
            checkDataClassForPrimitives(klass)
        }

        super.visitClass(klass)
    }

    private fun checkDataClassForPrimitives(klass: KtClass) {
        val primaryConstructor = klass.primaryConstructor ?: return

        primaryConstructor.valueParameters.forEach { param ->
            checkParameterForPrimitive(param, klass)
        }
    }

    private fun checkParameterForPrimitive(param: KtParameter, klass: KtClass) {
        val typeReference = param.typeReference ?: return
        val typeName = typeReference.text

        // Extract the base type (handle nullability and generics)
        val baseType = typeName
            .removeSuffix("?")
            .substringBefore("<")
            .trim()

        if (isPrimitiveType(baseType) && !isAllowed(baseType)) {
            report(
                Finding(
                    Entity.from(param),
                    "Property '${param.name}' in data class '${klass.name}' uses primitive type '$baseType'. " +
                        "Consider using a value class to make the domain model more expressive."
                )
            )
        }
    }

    private fun isPrimitiveType(typeName: String): Boolean {
        return primitiveTypes.any { primitive ->
            typeName == primitive || typeName == primitive.substringAfterLast(".")
        }
    }

    private fun isAllowed(typeName: String): Boolean {
        return allowedPrimitives.any { allowed ->
            typeName == allowed || typeName == allowed.substringAfterLast(".")
        }
    }

    private fun isExcluded(klass: KtClass): Boolean {
        val className = klass.name ?: ""
        if (matchesPatterns(className)) return true

        // Also check if nested inside a class/interface that matches patterns
        // The structure is: KtClass -> KtClassBody -> KtClass (nested)
        // So we need to skip KtClassBody to find the parent KtClass
        var parent = klass.parent
        while (parent != null) {
            when (parent) {
                is KtClass -> {
                    val parentName = parent.name ?: ""
                    if (matchesPatterns(parentName)) return true
                }
                is KtClassBody -> {
                    // Continue traversing, the KtClass is the parent of KtClassBody
                }
            }
            parent = parent.parent
        }
        return false
    }

    private fun matchesPatterns(name: String): Boolean =
        excludeClassNamePatterns.any { name.matches(Regex(it)) }
}
