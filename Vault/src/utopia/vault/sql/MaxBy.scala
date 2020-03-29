package utopia.vault.sql

import utopia.vault.model.immutable.Column

/**
  * Used for statements that find a single largest row based on some measurement
  * @author Mikko Hilpinen
  * @since 20.7.2019, v1.3+
  */
object MaxBy
{
	/**
	  * @param column Column that determines sort order
	  * @return A statement that only includes the 'maximum' row based on column
	  */
	def apply(column: Column) = OrderBy.descending(column) + Limit(1)
}
