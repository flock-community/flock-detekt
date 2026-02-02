package community.flock.detekt.arrow.rules

import community.flock.detekt.arrow.ArrowConfig
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtThrowExpression

/**
 * Rule that prevents using `throw` expressions in domain or adapter code.
 * Domain and adapter code should use Arrow's `Raise` for typed error handling instead.
 * The API layer is exempt because Spring uses response exceptions for HTTP error responses.
 *
 * <noncompliant>
 * // In a domain package
 * package com.example.domain.user
 *
 * class UserService {
 *     fun findUser(id: String): User {
 *         throw UserNotFoundException(id)  // VIOLATION - use Raise instead
 *     }
 * }
 *
 * // In an adapter package
 * package com.example.adapter.persistence
 *
 * class UserAdapter {
 *     fun save(user: User) {
 *         throw DatabaseException("Failed")  // VIOLATION - use Raise instead
 *     }
 * }
 * </noncompliant>
 *
 * <compliant>
 * // In a domain package with Arrow Raise
 * package com.example.domain.user
 *
 * class UserService {
 *     context(Raise<UserError>)
 *     fun findUser(id: String): User {
 *         raise(UserNotFound(id))  // OK - using typed error handling
 *     }
 * }
 *
 * // In API layer (exempt - Spring uses exceptions for HTTP responses)
 * package com.example.api.user
 *
 * class UserController {
 *     fun getUser(id: String): User {
 *         throw ResponseStatusException(HttpStatus.NOT_FOUND)  // OK - API layer
 *     }
 * }
 * </compliant>
 */
class NoThrowInDomainOrAdapters(config: Config) : Rule(
    config,
    "Domain and adapter code should use Arrow's Raise for typed error handling instead of throwing exceptions."
) {

    private val domainPackages: List<String> by config(ArrowConfig.DEFAULT_DOMAIN_PACKAGES)
    private val adapterPackages: List<String> by config(ArrowConfig.DEFAULT_ADAPTER_PACKAGES)

    private var shouldReport = false

    override fun visitKtFile(file: KtFile) {
        val isInDomain = ArrowConfig.fileIsInDomain(file, domainPackages)
        val isInAdapter = ArrowConfig.fileIsInAdapter(file, adapterPackages)
        shouldReport = isInDomain || isInAdapter
        super.visitKtFile(file)
    }

    override fun visitThrowExpression(expression: KtThrowExpression) {
        if (shouldReport) {
            report(
                Finding(
                    Entity.from(expression),
                    "Throwing exceptions in domain or adapter code violates typed error handling principles. " +
                        "Use Arrow's `Raise` context and `raise()` function instead."
                )
            )
        }
        super.visitThrowExpression(expression)
    }
}
