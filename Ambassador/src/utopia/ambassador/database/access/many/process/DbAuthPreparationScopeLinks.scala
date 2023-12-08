package utopia.ambassador.database.access.many.process

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple AuthPreparationScopeLinks at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object DbAuthPreparationScopeLinks extends ManyAuthPreparationScopeLinksAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted AuthPreparationScopeLinks
	  * @return An access point to AuthPreparationScopeLinks with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbAuthPreparationScopeLinksSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbAuthPreparationScopeLinksSubset(targetIds: Set[Int]) extends ManyAuthPreparationScopeLinksAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(index in targetIds)
	}
}

