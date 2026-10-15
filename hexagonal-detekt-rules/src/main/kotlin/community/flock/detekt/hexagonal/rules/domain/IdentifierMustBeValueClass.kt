package community.flock.detekt.hexagonal.rules.domain

import community.flock.detekt.hexagonal.HexagonalConfig
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

/**
 * Rule that requires identifiers in the domain to be typed with a value class instead of a raw
 * `String`, `Int`, `Long` or `UUID`.
 *
 * Raw identifiers let ids of different entities be mixed up silently
 * (`assign(userId = order.id)` compiles when both are `UUID`). This rule only looks at properties
 * and constructor properties whose *name* marks them as an identifier (default: `id` or ending in
 * `Id`), which makes it a low-noise alternative to [DomainNoPrimitiveObsession]: it targets the
 * primitive obsession that causes real bugs and leaves other primitives alone.
 *
 * The underlying property of a value class (`value class UserId(val value: UUID)`) is never
 * reported, nor are `override` declarations (the overridden declaration is reported instead).
 * Set `checkIdCollections: true` to also check the element type of collections named like
 * `userIds: List<String>`.
 *
 * <noncompliant>
 * // In a domain package
 * data class Order(
 *     val id: UUID,           // raw identifier
 *     val customerId: String, // raw identifier
 *     val parentId: Long?     // nullable raw identifier
 * )
 * </noncompliant>
 *
 * <compliant>
 * // In a domain package
 * @JvmInline
 * value class OrderId(val value: UUID)
 *
 * @JvmInline
 * value class CustomerId(val value: String)
 *
 * data class Order(
 *     val id: OrderId,
 *     val customerId: CustomerId,
 *     val parentId: OrderId?
 * )
 * </compliant>
 */
class IdentifierMustBeValueClass(config: Config) : Rule(
    config,
    "Domain identifiers should be value classes instead of raw String, Int, Long or UUID types."
) {

    private val domainPackages: List<String> by config(HexagonalConfig.DEFAULT_DOMAIN_PACKAGES)
    private val identifierNamePattern: Regex by config("^id$|.*Id$") { it.toRegex() }
    private val rawTypes: List<String> by config(DEFAULT_RAW_TYPES)
    private val checkIdCollections: Boolean by config(false)
    private val idCollectionNamePattern: Regex by config("^ids$|.*Ids$") { it.toRegex() }

    private var isInDomain = false

    override fun visitKtFile(file: KtFile) {
        isInDomain = HexagonalConfig.fileIsInDomain(file, domainPackages)
        super.visitKtFile(file)
    }

    override fun visitClass(klass: KtClass) {
        if (isInDomain && !klass.isValue() && !klass.isInline() && !klass.isAnnotation()) {
            klass.primaryConstructorParameters
                .filter { it.hasValOrVar() && !it.hasModifier(KtTokens.OVERRIDE_KEYWORD) }
                .forEach { checkDeclaration(it, it.typeReference, klass) }
        }
        super.visitClass(klass)
    }

    override fun visitProperty(property: KtProperty) {
        if (isInDomain && !property.isLocal && !property.hasModifier(KtTokens.OVERRIDE_KEYWORD)) {
            checkDeclaration(property, property.typeReference, property.containingClassOrObject)
        }
        super.visitProperty(property)
    }

    private fun checkDeclaration(
        declaration: KtNamedDeclaration,
        typeReference: KtTypeReference?,
        owner: KtClassOrObject?,
    ) {
        val name = declaration.name ?: return
        val typeElement = typeReference?.typeElement ?: return

        val rawType = when {
            identifierNamePattern.matches(name) -> rawTypeOf(typeElement)
            checkIdCollections && idCollectionNamePattern.matches(name) -> rawTypeArgumentOf(typeElement)
            else -> null
        } ?: return

        val location = owner?.name?.let { " in '$it'" }.orEmpty()
        report(
            Finding(
                Entity.atName(declaration),
                "Identifier '$name'$location uses raw type '$rawType'. " +
                    "Use a @JvmInline value class (e.g. '${suggestedValueClassName(name, owner)}') " +
                    "so identifiers of different entities cannot be mixed up."
            )
        )
    }

    private fun rawTypeOf(typeElement: KtTypeElement): String? {
        val userType = typeElement.unwrapNullable() as? KtUserType ?: return null
        if (userType.typeArguments.isNotEmpty()) return null
        return userType.qualifiedName().takeIf { it in rawTypes }
    }

    private fun rawTypeArgumentOf(typeElement: KtTypeElement): String? {
        val userType = typeElement.unwrapNullable() as? KtUserType ?: return null
        return userType.typeArguments
            .mapNotNull { it.typeReference?.typeElement }
            .firstNotNullOfOrNull { rawTypeOf(it) }
    }

    private fun KtTypeElement.unwrapNullable(): KtTypeElement? {
        var element: KtTypeElement? = this
        while (element is KtNullableType) {
            element = element.innerType
        }
        return element
    }

    private fun KtUserType.qualifiedName(): String {
        val name = referencedName.orEmpty()
        return qualifier?.let { "${it.qualifiedName()}.$name" } ?: name
    }

    private fun suggestedValueClassName(name: String, owner: KtClassOrObject?): String {
        val singular = if (idCollectionNamePattern.matches(name) && !identifierNamePattern.matches(name)) {
            name.removeSuffix("s")
        } else {
            name
        }
        return if (singular.equals("id", ignoreCase = true)) {
            "${owner?.name.orEmpty()}Id"
        } else {
            singular.replaceFirstChar { it.uppercaseChar() }
        }
    }

    companion object {
        val DEFAULT_RAW_TYPES = listOf(
            "String",
            "Int",
            "Long",
            "UUID",
            "java.util.UUID",
            "Uuid",
            "kotlin.uuid.Uuid",
            "kotlin.String",
            "kotlin.Int",
            "kotlin.Long",
        )
    }
}
