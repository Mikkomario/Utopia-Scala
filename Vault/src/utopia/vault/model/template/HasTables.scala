package utopia.vault.model.template

import utopia.vault.model.immutable.Table

/**
 * Common trait for classes which wrap / associate with one or more database tables
 *
 * @author Mikko Hilpinen
 * @since 11.07.2025, v1.22
 */
trait HasTables
{
	/**
	 * @return The tables wrapped / used by this item
	 */
	def tables: Seq[Table]
}
