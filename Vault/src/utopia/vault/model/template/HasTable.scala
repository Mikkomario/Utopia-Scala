package utopia.vault.model.template

import utopia.vault.model.immutable.Table

/**
 * Common trait for interfaces which specify a (primary) table
 *
 * @author Mikko Hilpinen
 * @since 16.06.2024, v1.19
 */
trait HasTable
{
	/**
	 * @return The table associated with this instance
	 */
	def table: Table
}
