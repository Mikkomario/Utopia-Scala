package utopia.scribe.api.database.access.single.logging.issue

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.scribe.api.database.factory.logging.IssueFactory
import utopia.scribe.api.database.model.logging.IssueModel
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.stored.logging.Issue
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleChronoRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.sql.Condition

import java.time.Instant

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
		
		override def globalCondition = Some(condition)
	}
}

/**
  * A common trait for access points that return individual and distinct issues.
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
trait UniqueIssueAccess extends UniqueIssueAccessLike[Issue] with SingleChronoRowModelAccess[Issue, UniqueIssueAccess]
{
	// IMPLEMENTED	--------------------
	
	override def factory = IssueFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): UniqueIssueAccess = 
		new UniqueIssueAccess._UniqueIssueAccess(mergeCondition(filterCondition))
}

