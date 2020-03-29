package utopia.vault.model.immutable.access

/**
 * Provides access to multiple non-deprecated instances
 * @author Mikko Hilpinen
 * @since 11.1.2020, v1.4
 */
@deprecated("Replaced with utopia.vault.nosql.access.NonDeprecatedAccess", "v1.4")
trait NonDeprecatedManyAccess[+A] extends ConditionalManyAccess[A] with NonDeprecatedAccess[A]
