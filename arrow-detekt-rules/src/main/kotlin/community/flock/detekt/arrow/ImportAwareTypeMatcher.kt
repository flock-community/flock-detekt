package community.flock.detekt.arrow

import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * Matches type names as written in a file against configured type names, without type resolution.
 *
 * Configured names without a package (`Either`) match any type with that simple name. Configured
 * fully qualified names (`kotlin.Result`) match a type written fully qualified, or a simple name
 * that the file's imports resolve to it: an explicit or star import of it, or Kotlin's default
 * imports (`kotlin.*`, `java.lang.*`, ...) when the file neither imports nor declares another type
 * with that simple name. This keeps a project's own `Result` type apart from `kotlin.Result`.
 */
class ImportAwareTypeMatcher(file: KtFile) {

    private val explicitImports = mutableMapOf<String, String>()
    private val starImportPackages = mutableSetOf<String>()
    private val declaredNames: Set<String> =
        (file.collectDescendantsOfType<KtClassOrObject>().mapNotNull { it.name } +
            file.collectDescendantsOfType<KtTypeAlias>().mapNotNull { it.name }).toSet()

    init {
        file.importDirectives.forEach { directive ->
            val fqName = directive.importedFqName ?: return@forEach
            if (directive.isAllUnder) {
                starImportPackages += fqName.asString()
            } else {
                explicitImports[directive.aliasName ?: fqName.shortName().asString()] = fqName.asString()
            }
        }
    }

    /**
     * Checks whether [writtenName] matches one of [typeNames] (see the class documentation).
     */
    fun matchesAny(writtenName: String, typeNames: List<String>): Boolean =
        typeNames.any { typeName ->
            if ('.' in typeName) {
                refersTo(writtenName, typeName)
            } else {
                writtenName.substringAfterLast('.') == typeName
            }
        }

    /**
     * Checks whether the type name [writtenName], as written in this file, refers to [fqName].
     */
    fun refersTo(writtenName: String, fqName: String): Boolean {
        if ('.' in writtenName) return writtenName == fqName

        val simpleName = fqName.substringAfterLast('.')
        if (writtenName != simpleName) return false

        explicitImports[writtenName]?.let { importedFqName -> return importedFqName == fqName }
        if (writtenName in declaredNames) return false

        val packageName = fqName.substringBeforeLast('.', missingDelimiterValue = "")
        return packageName in starImportPackages || packageName in DEFAULT_IMPORTED_PACKAGES
    }

    companion object {
        /**
         * Packages Kotlin/JVM files can reference without an import.
         */
        val DEFAULT_IMPORTED_PACKAGES = setOf(
            "kotlin",
            "kotlin.annotation",
            "kotlin.collections",
            "kotlin.comparisons",
            "kotlin.io",
            "kotlin.ranges",
            "kotlin.sequences",
            "kotlin.text",
            "kotlin.jvm",
            "java.lang",
        )

        /**
         * Returns the outermost user type of [typeReference] with nullability unwrapped
         * (`Either<E, A>?` gives `Either<E, A>`), or `null` for function and other non-user types.
         */
        fun outermostUserType(typeReference: KtTypeReference?): KtUserType? {
            var element: KtTypeElement? = typeReference?.typeElement
            while (element is KtNullableType) {
                element = element.innerType
            }
            return element as? KtUserType
        }

        /**
         * Returns the type name as written, with qualifiers but without type arguments
         * (`arrow.core.Either<E, A>` gives `arrow.core.Either`).
         */
        fun writtenName(userType: KtUserType): String {
            val name = userType.referencedName.orEmpty()
            return userType.qualifier?.let { "${writtenName(it)}.$name" } ?: name
        }
    }
}
