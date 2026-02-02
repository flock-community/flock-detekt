package community.flock.detekt.hexagonal.rules.layering

import community.flock.detekt.hexagonal.HexagonalConfig
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

/**
 * Rule that enforces DTO classes to be defined only in adapter or API layers.
 * DTOs are boundary artifacts for data transfer and should not exist in the domain.
 *
 * <noncompliant>
 * // DTO class defined in domain package - wrong!
 * package com.example.domain.user
 *
 * data class UserDto(
 *     val id: String,
 *     val name: String
 * )
 * </noncompliant>
 *
 * <compliant>
 * // DTO class defined in API package - correct!
 * package com.example.api.user
 *
 * data class UserDto(
 *     val id: String,
 *     val name: String
 * )
 * </compliant>
 */
class DtoOnlyInAdaptersOrApi(config: Config) : Rule(
    config,
    "DTO classes should only be defined in adapter or API layers, not in domain."
) {

    private val domainPackages: List<String> by config(HexagonalConfig.DEFAULT_DOMAIN_PACKAGES)
    private val adapterPackages: List<String> by config(HexagonalConfig.DEFAULT_ADAPTER_PACKAGES)
    private val apiPackages: List<String> by config(HexagonalConfig.DEFAULT_API_PACKAGES)
    private val dtoSuffixes: List<String> by config(listOf("Dto", "Request", "Response"))

    private var isInDomain = false
    private var packageName = ""

    override fun visitKtFile(file: KtFile) {
        packageName = file.packageFqName.asString()
        isInDomain = HexagonalConfig.fileIsInDomain(file, domainPackages)
        super.visitKtFile(file)
    }

    override fun visitClass(klass: KtClass) {
        if (!isInDomain) {
            super.visitClass(klass)
            return
        }

        val className = klass.name ?: return

        val isDto = dtoSuffixes.any { suffix -> className.endsWith(suffix) }

        if (isDto) {
            report(
                Finding(
                    Entity.from(klass),
                    "DTO class '$className' is defined in domain package '$packageName'. " +
                        "DTOs should only be in adapter or API layers."
                )
            )
        }

        super.visitClass(klass)
    }
}
