package utopia.vault.sql

/**
  * An sql segment used for counting rows in a table
  * @author Mikko Hilpinen
  * @since 26.8.2020, v1.2
  */
object Count
{
	/**
	  * Creates a new count segment
	  * @param target Targeted table or tables
	  * @return A new sql segment
	  */
	def apply(target: SqlTarget) = SqlSegment("SELECT COUNT(*) FROM", isSelect = true) + target.toSqlSegment
}
