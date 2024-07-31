package utopia.scribe.api.database.access.single.logging.issue

import utopia.scribe.api.database.factory.logging.IssueFactory
import utopia.scribe.core.model.stored.logging.Issue
import utopia.vault.nosql.access.single.model.SingleChronoRowModelAccess
import utopia.vault.sql.Condition

object UniqueIssueAccess
{
	// OTHER	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	def apply(condition: Condition): UniqueIssueAccess = new _UniqueIssueAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _UniqueIssueAccess(condition: Condition) extends UniqueIssueAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points that return individual and distinct issues.
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
trait UniqueIssueAccess 
	extends UniqueIssueAccessLike[Issue] with SingleChronoRowModelAccess[Issue, UniqueIssueAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * Copy of this access point where issue variants and occurrences are also included
	  */
	def withInstances = DbIssueInstances.filterDistinct(accessCondition.getOrElse(Condition.alwaysTrue))
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = IssueFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): UniqueIssueAccess = UniqueIssueAccess(condition)
}

