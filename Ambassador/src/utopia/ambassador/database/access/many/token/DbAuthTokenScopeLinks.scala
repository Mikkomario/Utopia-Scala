package utopia.ambassador.database.access.many.token

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple AuthTokenScopeLinks at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object DbAuthTokenScopeLinks extends ManyAuthTokenScopeLinksAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted AuthTokenScopeLinks
	  * @return An access point to AuthTokenScopeLinks with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbAuthTokenScopeLinksSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbAuthTokenScopeLinksSubset(targetIds: Set[Int]) extends ManyAuthTokenScopeLinksAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(index in targetIds)
	}
}

