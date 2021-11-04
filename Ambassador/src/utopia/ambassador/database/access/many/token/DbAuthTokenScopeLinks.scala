package utopia.ambassador.database.access.many.token

import utopia.flow.generic.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.SqlExtensions._

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
		
		override def globalCondition = Some(index in targetIds)
	}
}

