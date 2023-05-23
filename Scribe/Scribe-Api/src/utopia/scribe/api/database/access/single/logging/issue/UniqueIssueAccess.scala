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
trait UniqueIssueAccess 
	extends SingleChronoRowModelAccess[Issue, UniqueIssueAccess] 
		with DistinctModelAccess[Issue, Option[Issue], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * 
		Program context where this issue occurred or was logged. Should be unique.. None if no issue (or value)
	  *  was found.
	  */
	def context(implicit connection: Connection) = pullColumn(model.contextColumn).getString
	
	/**
	  * The estimated severity of this issue. None if no issue (or value) was found.
	  */
	def severity(implicit connection: Connection) = 
		pullColumn(model.severityColumn).int.flatMap(Severity.findForLevel)
	
	/**
	  * Time when this issue first occurred or was first recorded. None if no issue (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = IssueModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = IssueFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): UniqueIssueAccess = 
		new UniqueIssueAccess._UniqueIssueAccess(mergeCondition(filterCondition))
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the contexts of the targeted issues
	  * @param newContext A new context to assign
	  * @return Whether any issue was affected
	  */
	def context_=(newContext: String)(implicit connection: Connection) = 
		putColumn(model.contextColumn, newContext)
	
	/**
	  * Updates the creation times of the targeted issues
	  * @param newCreated A new created to assign
	  * @return Whether any issue was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the severities of the targeted issues
	  * @param newSeverity A new severity to assign
	  * @return Whether any issue was affected
	  */
	def severity_=(newSeverity: Severity)(implicit connection: Connection) = 
		putColumn(model.severityColumn, newSeverity.level)
}

