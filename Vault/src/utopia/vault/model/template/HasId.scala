package utopia.vault.model.template

/**
 * Common trait for classes which specify a (database row) id
 * @tparam Id Type of id of this instance
 * @author Mikko Hilpinen
 * @since 15/03/2024, v1.19
 */
trait HasId[+Id]
{
	/**
	 * @return Unique id of this item
	 */
	def id: Id
}
