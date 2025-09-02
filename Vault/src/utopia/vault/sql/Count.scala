package utopia.vault.sql

import utopia.vault.model.immutable.Column

/**
  * A SQL segment used for counting rows in a table
  * @author Mikko Hilpinen
  * @since 26.8.2020, v1.2
  */
object Count
{
	/**
	  * Creates a new count segment
	  * @param target Targeted table or tables
	  * @return A new SQL segment
	  */
	def apply(target: SqlTarget) = SqlSegment("SELECT COUNT(*) FROM", isSelect = true) + target.toSqlSegment
	/**
	 * Creates a count statement for a specific column
	 * @param target Targeted table or tables
	 * @param column Counted column
	 * @param distinct Whether only distinct values should be counted
	 * @return A new SQL segment
	 */
	def apply(target: SqlTarget, column: Column, distinct: Boolean) =
		SqlSegment(s"SELECT COUNT(${ if (distinct) "DISTINCT " else "" }${ column.sqlName }) FROM", isSelect = true) +
			target.toSqlSegment
}
