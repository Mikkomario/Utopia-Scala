package utopia.scribe.api.database.access.many.logging.issue_variant

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.util.NotEmpty
import utopia.scribe.api.database.factory.logging.IssueVariantInstancesFactory
import utopia.scribe.core.model.combined.logging.IssueVariantInstances
import utopia.vault.database.Connection
import utopia.vault.sql.Condition

object ManyIssueVariantInstancesAccess
{
	// OTHER	--------------------
	
	def apply(condition: Condition): ManyIssueVariantInstancesAccess = new SubAccess(condition)
	
	
	// NESTED	--------------------
	
	private class SubAccess(condition: Condition) extends ManyIssueVariantInstancesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points that return multiple issue variant instances at a time
  * @author Mikko Hilpinen
  * @since 25.05.2023
  */
trait ManyIssueVariantInstancesAccess 
	extends ManyIssueVariantsAccessLike[IssueVariantInstances, ManyIssueVariantInstancesAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * case ids of the accessible issue occurrences
	  */
	def occurrenceCaseIds(implicit connection: Connection) = 
		pullColumn(occurrenceModel.caseIdColumn).map { v => v.getInt }
	
	/**
	  * error messages of the accessible issue occurrences
	  */
	def occurrencesErrorMessages(implicit connection: Connection) = 
		pullColumn(occurrenceModel.errorMessagesColumn).map { v => v.getString }
	
	/**
	  * counts of the accessible issue occurrences
	  */
	def occurrenceCounts(implicit connection: Connection) = 
		pullColumn(occurrenceModel.countColumn).map { v => v.getInt }
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = IssueVariantInstancesFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyIssueVariantInstancesAccess = 
		ManyIssueVariantInstancesAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the case ids of the targeted issue occurrences
	  * @param newCaseId A new case id to assign
	  * @return Whether any issue occurrence was affected
	  */
	def occurrenceCaseIds_=(newCaseId: Int)(implicit connection: Connection) = 
		putColumn(occurrenceModel.caseIdColumn, newCaseId)
	
	/**
	  * Updates the counts of the targeted issue occurrences
	  * @param newCount A new count to assign
	  * @return Whether any issue occurrence was affected
	  */
	def occurrenceCounts_=(newCount: Int)(implicit connection: Connection) = 
		putColumn(occurrenceModel.countColumn, newCount)
	
	/**
	  * Updates the error messages of the targeted issue occurrences
	  * @param newErrorMessages A new error messages to assign
	  * @return Whether any issue occurrence was affected
	  */
	def occurrencesErrorMessages_=(newErrorMessages: Vector[String])(implicit connection: Connection) = 
		putColumn(occurrenceModel.errorMessagesColumn, 
			NotEmpty(newErrorMessages) match { case Some(v) => ((v.map { v => v }: Value).toJson): Value; case None => Value.empty })
}

