package utopia.vault.sql

import utopia.flow.generic.casting.ValueConversions._

/**
 * Offset determines how many rows to skip from the beginning. Offset is always used in combination with Limit.
 * @author Mikko Hilpinen
 * @since 15.10.2019, v1.4+
 */
object Offset
{
	/**
	 * Creates a new offset Sql segment
	 * @param numberOfRowsToSkip Number of rows to skip in result
	 * @return offset sql segment
	 */
	def apply(numberOfRowsToSkip: Int) = SqlSegment("OFFSET ?", Vector(numberOfRowsToSkip))
}
