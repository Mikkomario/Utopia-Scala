package utopia.scribe.api.database.access.many.logging.issue_variant

import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.access.many.logging.issue.SeverityBasedAccess
import utopia.scribe.api.database.factory.logging.ContextualIssueVariantFactory
import utopia.scribe.api.database.model.logging.IssueModel
import utopia.scribe.core.model.combined.logging.ContextualIssueVariant
import utopia.scribe.core.model.enumeration.Severity
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.sql.Condition

import java.time.Instant

object ManyContextualIssueVariantsAccess
{
	// NESTED	--------------------
	
	private class SubAccess(condition: Condition) extends ManyContextualIssueVariantsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(condition)
	}
}

/**
  * A common trait for access points that return multiple contextual issue variants at a time
  * @author Mikko Hilpinen
  * @since 23.05.2023
  */
trait ManyContextualIssueVariantsAccess 
	extends ManyIssueVariantsAccessLike[ContextualIssueVariant, ManyContextualIssueVariantsAccess] 
		with ManyRowModelAccess[ContextualIssueVariant] with SeverityBasedAccess[ManyContextualIssueVariantsAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * contexts of the accessible issues
	  */
	def issueContexts(implicit connection: Connection) = pullColumn(issueModel.contextColumn)
		.flatMap { _.string }
	
	/**
	  * severities of the accessible issues
	  */
	def issueSeverities(implicit connection: Connection) = 
		pullColumn(issueModel.severityColumn).map { v => v.getInt }.flatMap(Severity.findForLevel)
	
	/**
	  * creation times of the accessible issues
	  */
	def issueCreationTimes(implicit connection: Connection) = 
		pullColumn(issueModel.createdColumn).map { v => v.getInstant }
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ContextualIssueVariantFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): ManyContextualIssueVariantsAccess = 
		new ManyContextualIssueVariantsAccess.SubAccess(mergeCondition(filterCondition))
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the contexts of the targeted issues
	  * @param newContext A new context to assign
	  * @return Whether any issue was affected
	  */
	def issueContexts_=(newContext: String)(implicit connection: Connection) = 
		putColumn(issueModel.contextColumn, newContext)
	
	/**
	  * Updates the creation times of the targeted issues
	  * @param newCreated A new created to assign
	  * @return Whether any issue was affected
	  */
	def issueCreationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(issueModel.createdColumn, newCreated)
	
	/**
	  * Updates the severities of the targeted issues
	  * @param newSeverity A new severity to assign
	  * @return Whether any issue was affected
	  */
	def issueSeverities_=(newSeverity: Severity)(implicit connection: Connection) = 
		putColumn(issueModel.severityColumn, newSeverity.level)
}

