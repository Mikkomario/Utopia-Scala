package utopia.scribe.api.database.access.many.logging.issue

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple varying issues at a time
  * @author Mikko Hilpinen
  * @since 26.05.2023, v0.1
  */
object DbVaryingIssues extends ManyVaryingIssuesAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted varying issues
	  * @return An access point to varying issues with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbVaryingIssuesSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbVaryingIssuesSubset(targetIds: Set[Int]) extends ManyVaryingIssuesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(index in targetIds)
	}
}

