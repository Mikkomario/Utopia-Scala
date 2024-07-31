package utopia.scribe.api.database.access.many.logging.issue

import utopia.scribe.api.database.access.many.logging.issue_occurrence.OccurrenceTimeBasedAccess
import utopia.scribe.api.database.factory.logging.IssueInstancesFactory
import utopia.scribe.api.database.model.logging.IssueOccurrenceModel
import utopia.scribe.core.model.combined.logging.IssueInstances
import utopia.vault.database.Connection
import utopia.vault.nosql.factory.FromResultFactory
import utopia.vault.sql.Condition

object ManyIssueInstancesAccess
{
	// OTHER	--------------------
	
	def apply(condition: Condition): ManyIssueInstancesAccess = new ManyIssueInstancesSubView(condition)
	
	
	// NESTED	--------------------
	
	private class ManyIssueInstancesSubView(condition: Condition) extends ManyIssueInstancesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * Common trait for access point that retrieve issues, as well as their variant and occurrence data
  * @author Mikko Hilpinen
  * @since 25.05.2023, v0.1
  */
trait ManyIssueInstancesAccess 
	extends ManyIssuesAccessLike[IssueInstances, ManyIssueInstancesAccess] 
		with OccurrenceTimeBasedAccess[ManyIssueInstancesAccess]
{
	// COMPUTED	--------------------
	
	protected def occurrenceModel = IssueOccurrenceModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory: FromResultFactory[IssueInstances] = IssueInstancesFactory
	
	override protected def self: ManyIssueInstancesAccess = this
	
	override def apply(condition: Condition): ManyIssueInstancesAccess = ManyIssueInstancesAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * Deletes all accessible instance occurrences from the database
	  * @param connection Implicit DB connection
	  */
	def deleteOccurrences()(implicit connection: Connection) = delete(occurrenceModel.table)
}

