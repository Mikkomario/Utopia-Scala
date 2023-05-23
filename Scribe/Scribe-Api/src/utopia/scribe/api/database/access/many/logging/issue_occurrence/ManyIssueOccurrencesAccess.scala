package utopia.scribe.api.database.access.many.logging.issue_occurrence

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.scribe.api.database.factory.logging.IssueOccurrenceFactory
import utopia.scribe.api.database.model.logging.IssueOccurrenceModel
import utopia.scribe.core.model.stored.logging.IssueOccurrence
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.ChronoRowFactoryView
import utopia.vault.sql.Condition

import java.time.Instant

object ManyIssueOccurrencesAccess
{
	// NESTED	--------------------
	
	private class ManyIssueOccurrencesSubView(condition: Condition) extends ManyIssueOccurrencesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple issue occurrences at a time
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
trait ManyIssueOccurrencesAccess 
	extends ManyRowModelAccess[IssueOccurrence] 
		with ChronoRowFactoryView[IssueOccurrence, ManyIssueOccurrencesAccess] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * case ids of the accessible issue occurrences
	  */
	def caseIds(implicit connection: Connection) = pullColumn(model.caseIdColumn).map { v => v.getInt }
	
	/**
	  * error messages of the accessible issue occurrences
	  */
	def errorMessages(implicit connection: Connection) = 
		pullColumn(model.errorMessagesColumn).map { v => v.getString }
	
	/**
	  * creation times of the accessible issue occurrences
	  */
	def creationTimes(implicit connection: Connection) = pullColumn(model.createdColumn)
		.map { v => v.getInstant }
	
	def ids(implicit connection: Connection) = pullColumn(index).map { v => v.getInt }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = IssueOccurrenceModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = IssueOccurrenceFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): ManyIssueOccurrencesAccess = 
		new ManyIssueOccurrencesAccess.ManyIssueOccurrencesSubView(mergeCondition(filterCondition))
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the case ids of the targeted issue occurrences
	  * @param newCaseId A new case id to assign
	  * @return Whether any issue occurrence was affected
	  */
	def caseIds_=(newCaseId: Int)(implicit connection: Connection) = putColumn(model.caseIdColumn, newCaseId)
	
	/**
	  * Updates the creation times of the targeted issue occurrences
	  * @param newCreated A new created to assign
	  * @return Whether any issue occurrence was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the error messages of the targeted issue occurrences
	  * @param newErrorMessages A new error messages to assign
	  * @return Whether any issue occurrence was affected
	  */
	def errorMessages_=(newErrorMessages: Vector[String])(implicit connection: Connection) = 
		putColumn(model.errorMessagesColumn, (newErrorMessages.map { v => v }: Value).toJson)
}

