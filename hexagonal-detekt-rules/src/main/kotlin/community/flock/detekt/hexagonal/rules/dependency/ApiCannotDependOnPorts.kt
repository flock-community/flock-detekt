package community.flock.detekt.hexagonal.rules.dependency

import community.flock.detekt.hexagonal.HexagonalConfig
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

/**
 * Rule that prevents API layer classes from depending directly on port interfaces.
 * The API layer should depend on domain services, not on ports (Repository, Gateway, Client, etc.).
 *
 * <noncompliant>
 * // In API package
 * package com.example.api
 *
 * @RestController
 * class DeclarationsController(
 *     private val declarationRepository: DeclarationRepository  // Direct port dependency!
 * ) {
 *     // ...
 * }
 * </noncompliant>
 *
 * <compliant>
 * // In API package
 * package com.example.api
 *
 * @RestController
 * class AdviceController(
 *     private val adviceService: DeclarationAdviceService  // Uses service layer
 * ) {
 *     // ...
 * }
 * </compliant>
 */
class ApiCannotDependOnPorts(config: Config) : Rule(
    config,
    "API layer should not depend directly on port interfaces. Use domain services instead."
) {

    private val apiPackages: List<String> by config(HexagonalConfig.DEFAULT_API_PACKAGES)
    private val portSuffixes: List<String> by config(HexagonalConfig.DEFAULT_PORT_SUFFIXES)
    private val allowedPortTypes: List<String> by config(emptyList())

    private var isInApi = false

    override fun visitKtFile(file: KtFile) {
        isInApi = HexagonalConfig.fileIsInApi(file, apiPackages)
        super.visitKtFile(file)
    }

    override fun visitClass(klass: KtClass) {
        if (!isInApi) {
            super.visitClass(klass)
            return
        }

        // Only check concrete classes (not interfaces, data classes, enums, etc.)
        if (klass.isInterface() || klass.isEnum()) {
            super.visitClass(klass)
            return
        }

        checkConstructorForPortDependencies(klass)

        super.visitClass(klass)
    }

    private fun checkConstructorForPortDependencies(klass: KtClass) {
        val primaryConstructor = klass.primaryConstructor ?: return

        primaryConstructor.valueParameters.forEach { param ->
            val typeReference = param.typeReference ?: return@forEach
            val typeName = typeReference.text

            // Extract the base type (handle nullability and generics)
            val baseType = typeName
                .removeSuffix("?")
                .substringBefore("<")
                .trim()

            if (isPortType(baseType) && !isAllowed(baseType)) {
                report(
                    Finding(
                        Entity.from(param),
                        "Class '${klass.name}' in API layer has direct dependency on port type '$baseType'. " +
                            "Use a domain service instead of depending directly on ports."
                    )
                )
            }
        }
    }

    private fun isPortType(typeName: String): Boolean {
        return portSuffixes.any { suffix ->
            typeName.endsWith(suffix)
        }
    }

    private fun isAllowed(typeName: String): Boolean {
        return allowedPortTypes.any { allowed ->
            typeName == allowed || typeName.endsWith(".$allowed")
        }
    }
}
