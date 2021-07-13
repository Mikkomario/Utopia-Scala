package utopia.vault.model.immutable.access

/**
 * Used for accessing both model and index data for a table
 * @author Mikko Hilpinen
 * @since 30.7.2019, v1.3+
 */
@deprecated("Replaced with utopia.vault.nosql.access.single.model.SingleModelAccess", "v1.4")
trait SingleAccessWithIds[I, +A, Id <: SingleIdAccess[I]] extends SingleAccess[I, A]
{
	/**
	 * @return An id access node for this access
	 */
	def id: Id
}
