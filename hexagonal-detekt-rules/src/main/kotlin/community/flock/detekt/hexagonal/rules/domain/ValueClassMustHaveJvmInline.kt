package community.flock.detekt.hexagonal.rules.domain

import community.flock.detekt.hexagonal.HexagonalConfig
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

/**
 * Rule that ensures value classes have the @JvmInline annotation for proper optimization.
 *
 * <noncompliant>
 * // Missing @JvmInline annotation
 * value class UserId(val value: String)
 * </noncompliant>
 *
 * <compliant>
 * @JvmInline
 * value class UserId(val value: String)
 * </compliant>
 */
class ValueClassMustHaveJvmInline(config: Config) : Rule(
    config,
    "Value classes should have @JvmInline annotation for proper JVM optimization."
) {

    private val domainPackages: List<String> by config(HexagonalConfig.DEFAULT_DOMAIN_PACKAGES)
    private val checkOnlyInDomain: Boolean by config(false)

    private var isInDomain = false

    override fun visitKtFile(file: KtFile) {
        isInDomain = HexagonalConfig.fileIsInDomain(file, domainPackages)
        super.visitKtFile(file)
    }

    override fun visitClass(klass: KtClass) {
        // Skip if we only check domain and we're not in domain
        if (checkOnlyInDomain && !isInDomain) {
            super.visitClass(klass)
            return
        }

        if (klass.isValue()) {
            val hasJvmInline = klass.annotationEntries.any { annotation ->
                val annotationName = annotation.shortName?.asString()
                annotationName == "JvmInline"
            }

            if (!hasJvmInline) {
                report(
                    Finding(
                        Entity.from(klass),
                        "Value class '${klass.name}' is missing @JvmInline annotation. " +
                            "Add @JvmInline for proper JVM optimization."
                    )
                )
            }
        }

        super.visitClass(klass)
    }
}
