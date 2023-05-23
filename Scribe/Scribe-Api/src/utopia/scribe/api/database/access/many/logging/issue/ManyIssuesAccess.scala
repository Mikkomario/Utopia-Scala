package utopia.scribe.api.database.access.many.logging.issue

import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.factory.logging.IssueFactory
import utopia.scribe.api.database.model.logging.IssueModel
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.stored.logging.Issue
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.ChronoRowFactoryView
import utopia.vault.sql.Condition

import java.time.Instant

object ManyIssuesAccess
{
	// NESTED	--------------------
	
	private class ManyIssuesSubView(condition: Condition) extends ManyIssuesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple issues at a time
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
trait ManyIssuesAccess 
	extends ManyRowModelAccess[Issue] with ChronoRowFactoryView[Issue, ManyIssuesAccess] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * contexts of the accessible issues
	  */
	def contexts(implicit connection: Connection) = pullColumn(model.contextColumn).flatMap { _.string }
	
	/**
	  * severities of the accessible issues
	  */
	def severities(implicit connection: Connection) = 
		pullColumn(model.severityColumn).map { v => v.getInt }.flatMap(Severity.findForLevel)
	
	/**
	  * creation times of the accessible issues
	  */
	def creationTimes(implicit connection: Connection) = pullColumn(model.createdColumn)
		.map { v => v.getInstant }
	
	def ids(implicit connection: Connection) = pullColumn(index).map { v => v.getInt }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = IssueModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = IssueFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): ManyIssuesAccess = 
		new ManyIssuesAccess.ManyIssuesSubView(mergeCondition(filterCondition))
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the contexts of the targeted issues
	  * @param newContext A new context to assign
	  * @return Whether any issue was affected
	  */
	def contexts_=(newContext: String)(implicit connection: Connection) = 
		putColumn(model.contextColumn, newContext)
	
	/**
	  * Updates the creation times of the targeted issues
	  * @param newCreated A new created to assign
	  * @return Whether any issue was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the severities of the targeted issues
	  * @param newSeverity A new severity to assign
	  * @return Whether any issue was affected
	  */
	def severities_=(newSeverity: Severity)(implicit connection: Connection) = 
		putColumn(model.severityColumn, newSeverity.level)
}

