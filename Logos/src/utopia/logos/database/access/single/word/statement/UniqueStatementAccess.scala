package utopia.logos.database.access.single.word.statement

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.factory.word.StatementDbFactory
import utopia.logos.model.stored.word.Statement
import utopia.vault.nosql.access.single.model.SingleChronoRowModelAccess
import utopia.vault.sql.Condition

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
  * @since 20.03.2024, v0.2
  */
trait UniqueStatementAccess 
	extends UniqueStatementAccessLike[Statement] 
		with SingleChronoRowModelAccess[Statement, UniqueStatementAccess]
{
	// IMPLEMENTED	--------------------
	
	override def factory = StatementDbFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): UniqueStatementAccess = UniqueStatementAccess(condition)
}

