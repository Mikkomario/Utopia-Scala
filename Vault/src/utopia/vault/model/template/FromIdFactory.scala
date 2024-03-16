package utopia.vault.model.template

/**
 * Common trait for factory classes, which attach an id to an item, forming a new item
 * @tparam Id Type of accepted ids
 * @tparam A Type of constructed items
 * @author Mikko Hilpinen
 * @since 15/03/2024, v1.18.1
 */
trait FromIdFactory[-Id, +A]
{
	/**
	 * @param id New id to assign
	 * @return An item with that id
	 */
	def withId(id: Id): A
}
