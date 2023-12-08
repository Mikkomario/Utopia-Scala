package utopia.scribe.api.database.access.many.logging.issue

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple issues at a time
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object DbIssues extends ManyIssuesAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted issues
	  * @return An access point to issues with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbIssuesSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbIssuesSubset(targetIds: Set[Int]) extends ManyIssuesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(index in targetIds)
	}
}

