package community.flock.detekt.arrow.rules

import community.flock.detekt.arrow.ArrowConfig
import community.flock.detekt.arrow.ImportAwareTypeMatcher
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry

/**
 * Rule that prevents declaring exception types in the domain.
 *
 * [NoThrowInDomainOrAdapters] only sees `throw` sites, so it misses a common leak: a port declares
 * an exception type in the domain, an adapter throws it, and a domain service catches it to drive
 * control flow. That exception is an expected outcome and belongs in the port's signature as a
 * typed error (`context(_: Raise<E>)`), where the compiler makes callers handle it.
 *
 * A class or object is reported when it extends one of `exceptionTypes` (Kotlin/Java built-ins
 * such as `IllegalStateException`, matched unless the file declares or imports its own type of
 * that name; fully qualified names match a specific type) or any type whose name ends with one of
 * `exceptionSuffixes`. Only superclasses are considered, so implementing a domain interface named
 * `Error` is fine.
 *
 * <noncompliant>
 * // In a domain package
 * package com.example.domain.order
 *
 * class OrderAlreadyExists(val id: OrderId) : IllegalStateException("Order $id already exists")
 *
 * interface OrderRepository {
 *     fun insert(order: Order)  // adapter throws OrderAlreadyExists, service catches it
 * }
 * </noncompliant>
 *
 * <compliant>
 * // In a domain package
 * package com.example.domain.order
 *
 * sealed interface InsertOrderError {
 *     data class AlreadyExists(val id: OrderId) : InsertOrderError
 * }
 *
 * interface OrderRepository {
 *     context(_: Raise<InsertOrderError>)
 *     fun insert(order: Order)
 * }
 * </compliant>
 */
class NoExceptionTypesInDomain(config: Config) : Rule(
    config,
    "Domain code should model expected failures as typed errors, not as exception types."
) {

    private val domainPackages: List<String> by config(ArrowConfig.DEFAULT_DOMAIN_PACKAGES)
    private val exceptionTypes: List<String> by config(DEFAULT_EXCEPTION_TYPES)
    private val exceptionSuffixes: List<String> by config(listOf("Exception"))

    private var isInDomain = false
    private var typeMatcher: ImportAwareTypeMatcher? = null

    override fun visitKtFile(file: KtFile) {
        isInDomain = ArrowConfig.fileIsInDomain(file, domainPackages)
        typeMatcher = if (isInDomain) ImportAwareTypeMatcher(file) else null
        super.visitKtFile(file)
    }

    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        val matcher = typeMatcher
        if (isInDomain && matcher != null && classOrObject.name != null) {
            val exceptionSuperType = classOrObject.superTypeListEntries
                .filter { canBeSuperclass(classOrObject, it) }
                .mapNotNull { entry -> ImportAwareTypeMatcher.outermostUserType(entry.typeReference) }
                .map { ImportAwareTypeMatcher.writtenName(it) }
                .firstOrNull { isExceptionType(it, matcher) }

            if (exceptionSuperType != null) {
                report(
                    Finding(
                        Entity.atName(classOrObject),
                        "'${classOrObject.name}' extends '$exceptionSuperType' in the domain. " +
                            "Model expected failures as a sealed error type raised with " +
                            "`context(_: Raise<E>)` instead of an exception."
                    )
                )
            }
        }

        super.visitClassOrObject(classOrObject)
    }

    /**
     * Exceptions are classes, so they can only appear as a superclass: a constructor call
     * (`: Exception("...")`) or, in a class without a primary constructor whose secondary
     * constructors call `super(...)`, a plain type entry.
     */
    private fun canBeSuperclass(classOrObject: KtClassOrObject, entry: KtSuperTypeListEntry): Boolean =
        entry is KtSuperTypeCallEntry ||
            (classOrObject is KtClass &&
                !classOrObject.hasExplicitPrimaryConstructor() &&
                classOrObject.secondaryConstructors.isNotEmpty())

    private fun isExceptionType(writtenName: String, matcher: ImportAwareTypeMatcher): Boolean {
        val simpleName = writtenName.substringAfterLast('.')
        if (exceptionSuffixes.any { simpleName.endsWith(it) }) return true

        return exceptionTypes.any { typeName ->
            if ('.' in typeName) {
                matcher.refersTo(writtenName, typeName)
            } else {
                BUILT_IN_PACKAGES.any { matcher.refersTo(writtenName, "$it.$typeName") }
            }
        }
    }

    companion object {
        val DEFAULT_EXCEPTION_TYPES = listOf(
            "Throwable",
            "Exception",
            "RuntimeException",
            "Error",
            "IllegalStateException",
            "IllegalArgumentException",
        )

        private val BUILT_IN_PACKAGES = listOf("kotlin", "java.lang")
    }
}
