package utopia.scribe.api.database.access.many.logging.issue

import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.model.logging.IssueModel
import utopia.scribe.core.model.enumeration.Severity
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView

import java.time.Instant

/**
  * A common trait for access points which target multiple issues or similar instances at a time
  * @author Mikko Hilpinen
  * @since 25.05.2023, v0.1
  */
trait ManyIssuesAccessLike[+A, +Repr]
	extends ManyModelAccess[A] with Indexed with FilterableView[Repr] with SeverityBasedAccess[Repr]
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
	
	
	// OTHER	--------------------
	
	/**
	  * @param threshold A time threshold
	  * @return Access to issues that appeared since the specified time threshold
	  */
	def appearedSince(threshold: Instant) = filter(model.createdColumn > threshold)
	
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

