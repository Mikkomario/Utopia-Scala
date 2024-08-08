package utopia.vault.sql

import utopia.vault.model.immutable.{Column, Table}

/**
  * Used for creating select distinct -sql statements which are used for reading distinct values from a table / result
  * @author Mikko Hilpinen
  * @since 14.7.2019, v0.1+
  */
@deprecated("Please use Select.distinct(...) instead", "v1.20")
object SelectDistinct
{
	/**
	  * @param target Selection target
	  * @param column Selected column
	  * @return A distinct select statement for column in target
	  */
	def apply(target: SqlTarget, column: Column) = SqlSegment(s"SELECT DISTINCT ${
		column.columnNameWithTable} FROM", isSelect = true) + target.toSqlSegment
	
	/**
	  * @param table Target table
	  * @param selectedPropertyName Name of selected property
	  * @return A distinct select statement for column in table
	  */
	def apply(table: Table, selectedPropertyName: String): SqlSegment = apply(table, table(selectedPropertyName))
}
