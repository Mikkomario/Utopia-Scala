package utopia.scribe.api.database.access.single.logging.issue_variant

import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.factory.logging.ContextualIssueVariantFactory
import utopia.scribe.api.database.model.logging.IssueModel
import utopia.scribe.core.model.combined.logging.ContextualIssueVariant
import utopia.scribe.core.model.enumeration.Severity
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleChronoRowModelAccess
import utopia.vault.sql.Condition

import java.time.Instant

object UniqueContextualIssueVariantAccess
{
	// OTHER	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	def apply(condition: Condition): UniqueContextualIssueVariantAccess = 
		new _UniqueContextualIssueVariantAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _UniqueContextualIssueVariantAccess(condition: Condition) 
		extends UniqueContextualIssueVariantAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points that return distinct contextual issue variants
  * @author Mikko Hilpinen
  * @since 23.05.2023, v0.1
  */
trait UniqueContextualIssueVariantAccess 
	extends UniqueIssueVariantAccessLike[ContextualIssueVariant] 
		with SingleChronoRowModelAccess[ContextualIssueVariant, UniqueContextualIssueVariantAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * 
		Program context where this issue occurred or was logged. Should be unique.. None if no issue (or value)
	  * was found.
	  */
	def issueContext(implicit connection: Connection) = pullColumn(issueModel.contextColumn).getString
	
	/**
	  * The estimated severity of this issue. None if no issue (or value) was found.
	  */
	def issueSeverity(implicit connection: Connection) = 
		pullColumn(issueModel.severityColumn).int.flatMap(Severity.findForLevel)
	
	/**
	  * Time when this issue first occurred or was first recorded. None if no issue (or value) was found.
	  */
	def issueCreated(implicit connection: Connection) = pullColumn(issueModel.createdColumn).instant
	
	/**
	  * A database model (factory) used for interacting with the linked issue
	  */
	protected def issueModel = IssueModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ContextualIssueVariantFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): UniqueContextualIssueVariantAccess = 
		UniqueContextualIssueVariantAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the contexts of the targeted issues
	  * @param newContext A new context to assign
	  * @return Whether any issue was affected
	  */
	def issueContext_=(newContext: String)(implicit connection: Connection) = 
		putColumn(issueModel.contextColumn, newContext)
	
	/**
	  * Updates the creation times of the targeted issues
	  * @param newCreated A new created to assign
	  * @return Whether any issue was affected
	  */
	def issueCreated_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(issueModel.createdColumn, newCreated)
	
	/**
	  * Updates the severities of the targeted issues
	  * @param newSeverity A new severity to assign
	  * @return Whether any issue was affected
	  */
	def issueSeverity_=(newSeverity: Severity)(implicit connection: Connection) = 
		putColumn(issueModel.severityColumn, newSeverity.level)
}

