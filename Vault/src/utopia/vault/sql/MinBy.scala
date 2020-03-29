package utopia.vault.sql

import utopia.vault.model.immutable.Column

/**
  * Used for searching for single smallest row based on a condition
  * @author Mikko Hilpinen
  * @since 20.7.2019, v1.3+
  */
object MinBy
{
	/**
	  * @param column Column that determines the sort order
	  * @return A segment that only includes 'minimum' row based on the sort order
	  */
	def apply(column: Column) = OrderBy.ascending(column) + Limit(1)
}
