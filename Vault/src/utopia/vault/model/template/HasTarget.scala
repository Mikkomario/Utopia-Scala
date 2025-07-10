package utopia.vault.model.template

import utopia.vault.sql.SqlTarget

/**
 * Common trait for interfaces which can specify an SQL target
 *
 * @author Mikko Hilpinen
 * @since 09.07.2025, v1.22
 */
trait HasTarget
{
	/**
	 * @return Targeted SQL target
	 */
	def target: SqlTarget
}
