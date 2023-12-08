package utopia.exodus.database.access.many.auth

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple token scope links at a time
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
object DbTokenScopeLinks extends ManyTokenScopeLinksAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted token scope links
	  * @return An access point to token scope links with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbTokenScopeLinksSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbTokenScopeLinksSubset(targetIds: Set[Int]) extends ManyTokenScopeLinksAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(index in targetIds)
	}
}

