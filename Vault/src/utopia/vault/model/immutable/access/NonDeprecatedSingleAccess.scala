package utopia.vault.model.immutable.access

/**
 * Provides access to singular non-deprecated instances
 * @author Mikko Hilpinen
 * @since 11.1.2020, v1.4
 */
@deprecated("Replaced with utopia.vault.nosql.access.template.model.NonDeprecatedAccess", "v1.4")
trait NonDeprecatedSingleAccess[+A] extends ConditionalSingleAccess[A] with NonDeprecatedAccess[A]
