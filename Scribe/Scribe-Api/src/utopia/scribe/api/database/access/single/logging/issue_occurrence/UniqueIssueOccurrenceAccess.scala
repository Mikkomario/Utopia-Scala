package utopia.scribe.api.database.access.single.logging.issue_occurrence

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.util.NotEmpty
import utopia.scribe.api.database.factory.logging.IssueOccurrenceFactory
import utopia.scribe.api.database.model.logging.IssueOccurrenceModel
import utopia.scribe.core.model.stored.logging.IssueOccurrence
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

object UniqueIssueOccurrenceAccess
{
	// OTHER	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	def apply(condition: Condition): UniqueIssueOccurrenceAccess = new _UniqueIssueOccurrenceAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _UniqueIssueOccurrenceAccess(condition: Condition) extends UniqueIssueOccurrenceAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(condition)
	}
}

/**
  * A common trait for access points that return individual and distinct issue occurrences.
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
trait UniqueIssueOccurrenceAccess 
	extends SingleRowModelAccess[IssueOccurrence] with FilterableView[UniqueIssueOccurrenceAccess]
		with DistinctModelAccess[IssueOccurrence, Option[IssueOccurrence], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the issue variant that occurred. None if no issue occurrence (or value) was found.
	  */
	def caseId(implicit connection: Connection) = pullColumn(model.caseIdColumn).int
	
	/**
	  * Error messages listed in the stack trace. 
	  * If multiple occurrences are represented, 
	  * contains data from the latest occurrence.. None if no issue occurrence (or value) was found.
	  */
	def errorMessages(implicit connection: Connection) = 
		pullColumn(model.errorMessagesColumn).notEmpty match {
			 case Some(v) => JsonBunny.sureMunch(v.getString).getVector.map { v => v.getString };
			 case None => Vector.empty }
	
	/**
	  * Number of issue occurrences represented by this entry. None if no issue occurrence (or value) was found.
	  */
	def count(implicit connection: Connection) = pullColumn(model.countColumn).int
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = IssueOccurrenceModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = IssueOccurrenceFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): UniqueIssueOccurrenceAccess = 
		new UniqueIssueOccurrenceAccess._UniqueIssueOccurrenceAccess(mergeCondition(filterCondition))
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the case ids of the targeted issue occurrences
	  * @param newCaseId A new case id to assign
	  * @return Whether any issue occurrence was affected
	  */
	def caseId_=(newCaseId: Int)(implicit connection: Connection) = putColumn(model.caseIdColumn, newCaseId)
	
	/**
	  * Updates the counts of the targeted issue occurrences
	  * @param newCount A new count to assign
	  * @return Whether any issue occurrence was affected
	  */
	def count_=(newCount: Int)(implicit connection: Connection) = putColumn(model.countColumn, newCount)
	
	/**
	  * Updates the error messages of the targeted issue occurrences
	  * @param newErrorMessages A new error messages to assign
	  * @return Whether any issue occurrence was affected
	  */
	def errorMessages_=(newErrorMessages: Vector[String])(implicit connection: Connection) = 
		putColumn(model.errorMessagesColumn, 
			NotEmpty(newErrorMessages) match { case Some(v) => (v.map { v => v }: Value).toJson: Value; case None => Value.empty })
}

