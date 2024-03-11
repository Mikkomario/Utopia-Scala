package utopia.logos.database.access.single.text.statement

import utopia.vault.nosql.access.single.model.SingleChronoRowModelAccess
import utopia.vault.sql.Condition
import utopia.logos.database.factory.text.StatementFactory
import utopia.logos.model.stored.text.Statement

object UniqueStatementAccess
{
	// OTHER	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	def apply(condition: Condition): UniqueStatementAccess = new _UniqueStatementAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _UniqueStatementAccess(condition: Condition) extends UniqueStatementAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points that return individual and distinct statements.
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
trait UniqueStatementAccess 
	extends UniqueStatementAccessLike[Statement] 
		with SingleChronoRowModelAccess[Statement, UniqueStatementAccess]
{
	// IMPLEMENTED	--------------------
	
	override def factory = StatementFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): UniqueStatementAccess = 
		new UniqueStatementAccess._UniqueStatementAccess(mergeCondition(filterCondition))
}

