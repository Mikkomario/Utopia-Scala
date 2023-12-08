package utopia.scribe.api.database.access.many.logging.issue_occurrence

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple issue occurrences at a time
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object DbIssueOccurrences extends ManyIssueOccurrencesAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted issue occurrences
	  * @return An access point to issue occurrences with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbIssueOccurrencesSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbIssueOccurrencesSubset(targetIds: Set[Int]) extends ManyIssueOccurrencesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(index in targetIds)
	}
}

