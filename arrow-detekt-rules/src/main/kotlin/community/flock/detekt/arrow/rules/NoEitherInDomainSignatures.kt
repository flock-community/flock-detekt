package community.flock.detekt.arrow.rules

import community.flock.detekt.arrow.ArrowConfig
import community.flock.detekt.arrow.ImportAwareTypeMatcher
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeReference

/**
 * Rule that prevents `Either` (or other configured result wrappers) in the public signatures of
 * domain code.
 *
 * Arrow's `Raise` DSL lets domain functions declare their typed errors with
 * `context(_: Raise<E>)` and return the plain success value, so callers compose them without
 * `bind()`/`flatMap` plumbing; `Either` belongs at the edges, where a caller folds the outcome.
 * This rule reports non-private functions whose declared return type is `Either`, and non-private
 * properties, constructor parameters and type aliases of type `Either`. Only the outermost type is
 * checked: `Either<E, A>` and `Either<E, A>?` are reported, `List<Either<E, A>>` is not.
 *
 * Unlike [DomainServiceMustUseRaise], which asks every public service function for a `Raise`
 * context, this rule only flags functions that already model failure, just with the wrong type.
 *
 * `forbiddenTypes` entries without a package match by simple name; fully qualified entries match
 * the type written qualified or imported. `kotlin.Result` is not forbidden by default because
 * domains commonly declare their own `Result` types; add `kotlin.Result` to opt in, which matches
 * an unqualified `Result` only when the file does not import or declare another `Result`.
 *
 * <noncompliant>
 * // In a domain package
 * package com.example.domain.order
 *
 * class OrderService(private val repository: OrderRepository) {
 *     fun place(order: Order): Either<PlaceOrderError, OrderId> = either {
 *         ensure(order.lines.isNotEmpty()) { PlaceOrderError.Empty }
 *         repository.insert(order)
 *     }
 * }
 * </noncompliant>
 *
 * <compliant>
 * // In a domain package
 * package com.example.domain.order
 *
 * class OrderService(private val repository: OrderRepository) {
 *     context(_: Raise<PlaceOrderError>)
 *     fun place(order: Order): OrderId {
 *         ensure(order.lines.isNotEmpty()) { PlaceOrderError.Empty }
 *         return repository.insert(order)
 *     }
 * }
 * </compliant>
 */
class NoEitherInDomainSignatures(config: Config) : Rule(
    config,
    "Domain signatures should declare typed errors with context(_: Raise<E>) instead of returning Either."
) {

    private val domainPackages: List<String> by config(ArrowConfig.DEFAULT_DOMAIN_PACKAGES)
    private val forbiddenTypes: List<String> by config(listOf("Either", "arrow.core.Either"))

    private var isInDomain = false
    private var typeMatcher: ImportAwareTypeMatcher? = null

    override fun visitKtFile(file: KtFile) {
        isInDomain = ArrowConfig.fileIsInDomain(file, domainPackages)
        typeMatcher = if (isInDomain) ImportAwareTypeMatcher(file) else null
        super.visitKtFile(file)
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        if (isInDomain && !function.isLocal && isExposed(function)) {
            reportIfForbidden(function, function.typeReference, "Function '${function.name}' returns")
        }
        super.visitNamedFunction(function)
    }

    override fun visitProperty(property: KtProperty) {
        if (isInDomain && !property.isLocal && isExposed(property)) {
            reportIfForbidden(property, property.typeReference, "Property '${property.name}' has type")
        }
        super.visitProperty(property)
    }

    override fun visitClass(klass: KtClass) {
        if (isInDomain) {
            val constructorIsPrivate = klass.primaryConstructor?.hasModifier(KtTokens.PRIVATE_KEYWORD) == true
            klass.primaryConstructorParameters
                .filter { parameter -> if (parameter.hasValOrVar()) isExposed(parameter) else !constructorIsPrivate }
                .forEach { parameter ->
                    val subject = "Constructor parameter '${parameter.name}' has type"
                    reportIfForbidden(parameter, parameter.typeReference, subject)
                }
        }
        super.visitClass(klass)
    }

    override fun visitTypeAlias(typeAlias: KtTypeAlias) {
        if (isInDomain && isExposed(typeAlias)) {
            reportIfForbidden(typeAlias, typeAlias.getTypeReference(), "Type alias '${typeAlias.name}' expands to")
        }
        super.visitTypeAlias(typeAlias)
    }

    private fun isExposed(declaration: KtModifierListOwner): Boolean =
        !declaration.hasModifier(KtTokens.PRIVATE_KEYWORD) && !declaration.hasModifier(KtTokens.OVERRIDE_KEYWORD)

    private fun reportIfForbidden(
        declaration: KtNamedDeclaration,
        typeReference: KtTypeReference?,
        subject: String,
    ) {
        val matcher = typeMatcher ?: return
        val userType = ImportAwareTypeMatcher.outermostUserType(typeReference) ?: return
        val typeName = ImportAwareTypeMatcher.writtenName(userType)

        if (matcher.matchesAny(typeName, forbiddenTypes)) {
            report(
                Finding(
                    Entity.atName(declaration),
                    "$subject '$typeName' in the domain. Declare the typed error with " +
                        "`context(_: Raise<E>)` and return the success value instead; " +
                        "convert to '$typeName' at the edges if a caller needs it."
                )
            )
        }
    }
}
