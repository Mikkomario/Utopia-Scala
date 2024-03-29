package utopia.ambassador.database.access.many.scope

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple TaskScopeLinks at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object DbTaskScopeLinks extends ManyTaskScopeLinksAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted TaskScopeLinks
	  * @return An access point to TaskScopeLinks with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbTaskScopeLinksSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbTaskScopeLinksSubset(targetIds: Set[Int]) extends ManyTaskScopeLinksAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(index in targetIds)
	}
}

