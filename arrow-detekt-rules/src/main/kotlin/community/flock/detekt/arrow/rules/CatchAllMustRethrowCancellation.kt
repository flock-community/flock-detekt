package community.flock.detekt.arrow.rules

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import community.flock.detekt.arrow.ImportAwareTypeMatcher
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtWhenConditionIsPattern
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents

/**
 * Rule that requires catch-all clauses in suspend functions to let coroutine cancellation through.
 *
 * Coroutines are cancelled by throwing `CancellationException` at a suspension point. A
 * `catch (e: Exception)` (or `Throwable`, `RuntimeException`, `IllegalStateException`, all
 * supertypes of `CancellationException`) in a suspend function catches it too, so the coroutine
 * keeps running as if nothing happened: cancellation is swallowed and structured concurrency
 * breaks. Typed-error code is prone to this because such catch-alls are how exceptions from
 * libraries get converted into `Raise` errors.
 *
 * Inside `suspend fun` bodies, a catch-all clause is accepted when:
 * - an earlier clause of the same `try` catches `CancellationException` and rethrows it, or
 * - the catch-all itself rethrows its exception unconditionally, or when it
 *   `is CancellationException`, or
 * - the catch-all calls one of `cancellationCheckCalls` (default `ensureActive()` and Arrow's
 *   `nonFatalOrThrow()`, which rethrows `CancellationException`).
 *
 * A `CancellationException` clause in front of a catch-all is held to the same standard, so a
 * clause that catches `CancellationException` without rethrowing it is reported as well.
 *
 * Detekt's built-in `coroutines>SuspendFunSwallowedCancellation` checks the same problem with
 * type resolution: it only runs with the Analysis API (e.g. the `detektMain` Gradle tasks), but
 * then knows whether a `try` block actually calls suspend functions and also covers `runCatching`.
 * This rule works on syntax alone, so it runs in plain `detekt` tasks, at the cost of assuming
 * every `try` in a suspend function can be cancelled. It does not look at `runCatching`.
 *
 * <noncompliant>
 * suspend fun loadProfile(id: UserId): Profile? =
 *     try {
 *         client.fetchProfile(id)
 *     } catch (e: Exception) {  // also catches CancellationException
 *         null
 *     }
 * </noncompliant>
 *
 * <compliant>
 * suspend fun loadProfile(id: UserId): Profile? =
 *     try {
 *         client.fetchProfile(id)
 *     } catch (e: CancellationException) {
 *         throw e
 *     } catch (e: Exception) {
 *         null
 *     }
 * </compliant>
 */
class CatchAllMustRethrowCancellation(config: Config) : Rule(
    config,
    "Catch-all clauses in suspend functions must rethrow CancellationException " +
        "so coroutine cancellation is not swallowed."
) {

    private val catchAllTypes: List<String> by config(DEFAULT_CATCH_ALL_TYPES)
    private val cancellationExceptionTypes: List<String> by config(DEFAULT_CANCELLATION_EXCEPTION_TYPES)
    private val cancellationCheckCalls: List<String> by config(listOf("ensureActive", "nonFatalOrThrow"))

    private var typeMatcher: ImportAwareTypeMatcher? = null

    override fun visitKtFile(file: KtFile) {
        typeMatcher = ImportAwareTypeMatcher(file)
        super.visitKtFile(file)
    }

    override fun visitTryExpression(expression: KtTryExpression) {
        if (isInSuspendFunction(expression)) {
            checkTry(expression)
        }
        super.visitTryExpression(expression)
    }

    private fun isInSuspendFunction(expression: KtTryExpression): Boolean =
        expression.getStrictParentOfType<KtNamedFunction>()?.hasModifier(KtTokens.SUSPEND_KEYWORD) == true

    private fun checkTry(expression: KtTryExpression) {
        val clauses = expression.catchClauses
        val catchAllIndex = clauses.indexOfFirst { isCaughtType(it, catchAllTypes) }
        if (catchAllIndex < 0) return

        val catchAll = clauses[catchAllIndex]
        val cancellationClause = clauses.take(catchAllIndex)
            .firstOrNull { isCaughtType(it, cancellationExceptionTypes) }

        if (cancellationClause != null) {
            if (!handlesCancellation(cancellationClause)) {
                report(
                    Finding(
                        Entity.from(cancellationClause),
                        "CancellationException is caught but not rethrown, so coroutine cancellation is " +
                            "swallowed. Rethrow it (`throw e`) to let the coroutine complete its cancellation."
                    )
                )
            }
        } else if (!handlesCancellation(catchAll)) {
            report(
                Finding(
                    Entity.from(catchAll),
                    "Catching '${caughtTypeName(catchAll)}' in a suspend function also catches " +
                        "CancellationException, so coroutine cancellation is swallowed. Add " +
                        "`catch (e: CancellationException) { throw e }` before it, or rethrow it from this " +
                        "clause (`if (e is CancellationException) throw e`)."
                )
            )
        }
    }

    private fun caughtTypeName(clause: KtCatchClause): String? =
        ImportAwareTypeMatcher.outermostUserType(clause.catchParameter?.typeReference)
            ?.let { ImportAwareTypeMatcher.writtenName(it) }

    private fun isCaughtType(clause: KtCatchClause, typeNames: List<String>): Boolean =
        isOneOf(clause.catchParameter?.typeReference, typeNames)

    private fun isOneOf(typeReference: KtTypeReference?, typeNames: List<String>): Boolean {
        val matcher = typeMatcher ?: return false
        val userType = ImportAwareTypeMatcher.outermostUserType(typeReference) ?: return false
        return matcher.matchesAny(ImportAwareTypeMatcher.writtenName(userType), typeNames)
    }

    private fun handlesCancellation(clause: KtCatchClause): Boolean {
        val body = clause.catchBody ?: return false
        return rethrowsUnconditionally(clause) ||
            callsCancellationCheck(body) ||
            rethrowsWhenCancelled(clause, body)
    }

    private fun rethrowsUnconditionally(clause: KtCatchClause): Boolean {
        val parameterName = clause.catchParameter?.name ?: return false
        val body = clause.catchBody ?: return false
        val statements = (body as? KtBlockExpression)?.statements ?: listOf(body)
        return statements.any { isThrowOf(it, parameterName) }
    }

    private fun callsCancellationCheck(body: KtExpression): Boolean =
        body.collectDescendantsOfType<KtCallExpression>().any { call ->
            call.calleeExpression?.text in cancellationCheckCalls
        }

    /**
     * Accepts `if (e is CancellationException) throw e` and
     * `when (e) { is CancellationException -> throw e }` (also inside larger conditions).
     */
    private fun rethrowsWhenCancelled(clause: KtCatchClause, body: KtExpression): Boolean {
        val parameterName = clause.catchParameter?.name ?: return false
        return body.collectDescendantsOfType<KtThrowExpression>()
            .filter { isThrowOf(it, parameterName) }
            .any { throwExpression ->
                throwExpression.parents.takeWhile { it != body }.any { ancestor ->
                    isGuardedByCancellationCheck(ancestor, throwExpression, parameterName)
                }
            }
    }

    private fun isGuardedByCancellationCheck(
        ancestor: PsiElement,
        throwExpression: KtThrowExpression,
        parameterName: String,
    ): Boolean = when (ancestor) {
        is KtIfExpression ->
            PsiTreeUtil.isAncestor(ancestor.then, throwExpression, false) &&
                containsCancellationCheck(ancestor.condition, parameterName)

        is KtWhenEntry ->
            PsiTreeUtil.isAncestor(ancestor.expression, throwExpression, false) &&
                ancestor.conditions.any { condition ->
                    val isCancellationPattern = condition is KtWhenConditionIsPattern &&
                        !condition.isNegated &&
                        isOneOf(condition.typeReference, cancellationExceptionTypes)
                    isCancellationPattern || containsCancellationCheck(condition, parameterName)
                }

        else -> false
    }

    private fun containsCancellationCheck(element: PsiElement?, parameterName: String): Boolean {
        if (element == null) return false
        val isExpressions = listOfNotNull(element as? KtIsExpression) +
            element.collectDescendantsOfType<KtIsExpression>()
        return isExpressions.any { isExpression ->
            !isExpression.isNegated &&
                (isExpression.leftHandSide as? KtNameReferenceExpression)?.getReferencedName() == parameterName &&
                isOneOf(isExpression.typeReference, cancellationExceptionTypes)
        }
    }

    private fun isThrowOf(expression: KtExpression, parameterName: String): Boolean =
        expression is KtThrowExpression &&
            (expression.thrownExpression as? KtNameReferenceExpression)?.getReferencedName() == parameterName

    companion object {
        val DEFAULT_CATCH_ALL_TYPES = listOf(
            "Throwable",
            "kotlin.Throwable",
            "java.lang.Throwable",
            "Exception",
            "kotlin.Exception",
            "java.lang.Exception",
            "RuntimeException",
            "kotlin.RuntimeException",
            "java.lang.RuntimeException",
            "IllegalStateException",
            "kotlin.IllegalStateException",
            "java.lang.IllegalStateException",
        )

        val DEFAULT_CANCELLATION_EXCEPTION_TYPES = listOf(
            "CancellationException",
            "kotlin.coroutines.cancellation.CancellationException",
            "kotlinx.coroutines.CancellationException",
            "java.util.concurrent.CancellationException",
        )
    }
}
