package community.flock.detekt.arrow.rules

import community.flock.detekt.arrow.ArrowConfig
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

/**
 * Rule that enforces error types to be sealed interfaces for exhaustive when matching.
 * Error types used with Arrow's Raise should be sealed interfaces to ensure
 * the compiler can verify all error cases are handled.
 *
 * <noncompliant>
 * // Non-sealed interface - not exhaustive
 * package com.example.domain.user
 *
 * interface UserError {  // VIOLATION - missing 'sealed'
 *     data class NotFound(val id: String) : UserError
 * }
 *
 * // Sealed class instead of sealed interface
 * sealed class PaymentError {  // VIOLATION - should be 'sealed interface'
 *     data class Declined(val reason: String) : PaymentError()
 * }
 * </noncompliant>
 *
 * <compliant>
 * // Sealed interface - exhaustive when matching
 * package com.example.domain.user
 *
 * sealed interface UserError {
 *     data class NotFound(val id: String) : UserError
 *     data class InvalidInput(val field: String) : UserError
 * }
 * </compliant>
 */
class ErrorTypeMustBeSealedInterface(config: Config) : Rule(
    config,
    "Error types should be sealed interfaces for exhaustive when matching."
) {

    private val domainPackages: List<String> by config(ArrowConfig.DEFAULT_DOMAIN_PACKAGES)
    private val errorSuffixes: List<String> by config(DEFAULT_ERROR_SUFFIXES)

    private var isInDomain = false

    override fun visitKtFile(file: KtFile) {
        isInDomain = ArrowConfig.fileIsInDomain(file, domainPackages)
        super.visitKtFile(file)
    }

    override fun visitClass(klass: KtClass) {
        if (!isInDomain) {
            super.visitClass(klass)
            return
        }

        val className = klass.name ?: ""

        // Check if class name ends with error suffix
        if (!ArrowConfig.classNameHasSuffix(className, errorSuffixes)) {
            super.visitClass(klass)
            return
        }

        // Error type should be a sealed interface
        val isSealed = klass.isSealed()
        val isInterface = klass.isInterface()

        if (!isSealed || !isInterface) {
            val issue = when {
                !isSealed && !isInterface -> "should be a sealed interface (currently an open class)"
                !isSealed -> "should be a sealed interface (currently a non-sealed interface)"
                else -> "should be a sealed interface (currently a sealed class)"
            }
            report(
                Finding(
                    Entity.from(klass),
                    "Error type '$className' $issue for exhaustive when matching."
                )
            )
        }

        super.visitClass(klass)
    }

    companion object {
        val DEFAULT_ERROR_SUFFIXES = listOf("Error", "Failure")
    }
}
