package utopia.vault.model.template

/**
 * Common trait for classes which specify a (database row) id
 * @author Mikko Hilpinen
 * @since 15/03/2024, v1.18.1
 */
trait HasId[+Id]
{
	/**
	 * @return Unique id of this item
	 */
	def id: Id
}
