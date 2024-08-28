package utopia.logos.database.access.single.text.statement

import utopia.logos.database.factory.text.StatementDbFactory
import utopia.logos.model.stored.text.Statement
import utopia.vault.nosql.access.single.model.SingleChronoRowModelAccess
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

object UniqueStatementAccess extends ViewFactory[UniqueStatementAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): UniqueStatementAccess = _UniqueStatementAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _UniqueStatementAccess(override val accessCondition: Option[Condition]) 
		extends UniqueStatementAccess
}

/**
  * A common trait for access points that return individual and distinct statements.
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait UniqueStatementAccess 
	extends UniqueStatementAccessLike[Statement, UniqueStatementAccess] 
		with SingleChronoRowModelAccess[Statement, UniqueStatementAccess]
{
	// IMPLEMENTED	--------------------
	
	override def factory = StatementDbFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): UniqueStatementAccess = UniqueStatementAccess(condition)
}

