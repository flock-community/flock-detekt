package community.flock.detekt.arrow.rules

import community.flock.detekt.arrow.ArrowConfig
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Rule that enforces domain service functions to use Arrow's `context(Raise<E>)`
 * for typed error handling.
 *
 * <noncompliant>
 * // In a domain package
 * package com.example.domain.user
 *
 * class UserService {
 *     fun createUser(name: String): User {  // VIOLATION - missing Raise context
 *         return User(name)
 *     }
 * }
 * </noncompliant>
 *
 * <compliant>
 * // In a domain package with Arrow Raise context
 * package com.example.domain.user
 *
 * class UserService {
 *     context(Raise<UserError>)
 *     fun createUser(name: String): User {
 *         return User(name)
 *     }
 * }
 * </compliant>
 */
class DomainServiceMustUseRaise(config: Config) : Rule(
    config,
    "Domain service functions should use Arrow's Raise context for typed error handling."
) {

    private val domainPackages: List<String> by config(ArrowConfig.DEFAULT_DOMAIN_PACKAGES)
    private val serviceSuffixes: List<String> by config(ArrowConfig.DEFAULT_SERVICE_SUFFIXES)

    private var isInDomain = false
    private var isInServiceClass = false
    private var currentClassName: String? = null

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
        val wasInServiceClass = isInServiceClass
        val previousClassName = currentClassName

        // Check if this is a service class (not an interface)
        isInServiceClass = !klass.isInterface() &&
            ArrowConfig.classNameHasSuffix(className, serviceSuffixes)
        currentClassName = className

        super.visitClass(klass)

        // Restore state after visiting nested classes
        isInServiceClass = wasInServiceClass
        currentClassName = previousClassName
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        if (!isInDomain || !isInServiceClass) {
            super.visitNamedFunction(function)
            return
        }

        // Skip private functions
        if (function.hasModifier(org.jetbrains.kotlin.lexer.KtTokens.PRIVATE_KEYWORD)) {
            super.visitNamedFunction(function)
            return
        }

        // Skip abstract functions (interface declarations)
        if (function.hasModifier(org.jetbrains.kotlin.lexer.KtTokens.ABSTRACT_KEYWORD)) {
            super.visitNamedFunction(function)
            return
        }

        // Check if function has a Raise context receiver
        if (!hasRaiseContextReceiver(function)) {
            report(
                Finding(
                    Entity.from(function),
                    "Function '${function.name}' in service class '$currentClassName' should use " +
                        "`context(Raise<E>)` for typed error handling."
                )
            )
        }

        super.visitNamedFunction(function)
    }

    private fun hasRaiseContextReceiver(function: KtNamedFunction): Boolean {
        // Check for context receivers using the PSI API
        val contextReceiverList = function.contextReceivers
        if (contextReceiverList.isNotEmpty()) {
            // Check if any context receiver contains "Raise"
            return contextReceiverList.any { receiver ->
                receiver.text.contains("Raise")
            }
        }

        // Also check for context parameters (newer syntax with named parameter: context(name: Type))
        // These may be in the modifierList or as a KtContextReceiverList child
        // Fall back to checking the function's text for context(...Raise...)
        val functionText = function.text
        val contextPattern = Regex("""context\s*\([^)]*Raise[^)]*\)""")
        if (contextPattern.containsMatchIn(functionText)) {
            return true
        }

        return false
    }
}
