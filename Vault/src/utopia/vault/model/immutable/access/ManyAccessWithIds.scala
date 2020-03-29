package utopia.vault.model.immutable.access

/**
 * Used for accessing both model and index data in a table
 * @author Mikko Hilpinen
 * @since 30.7.2019, v1.3+
 */
@deprecated("Replaced with utopia.vault.nosql.access.ManyModelAccess", "v1.4")
trait ManyAccessWithIds[I, +A, Id <: ManyIdAccess[I]] extends ManyAccess[I, A]
{
	/**
	 * @return An access point to ids in the related table
	 */
	def ids: Id
}
