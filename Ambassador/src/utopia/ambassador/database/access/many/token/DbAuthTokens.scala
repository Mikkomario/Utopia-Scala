package utopia.ambassador.database.access.many.token

import utopia.ambassador.model.stored.token.AuthToken
import utopia.flow.generic.ValueConversions._
import utopia.vault.nosql.view.NonDeprecatedView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple AuthTokens at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object DbAuthTokens extends ManyAuthTokensAccess with NonDeprecatedView[AuthToken]
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted AuthTokens
	  * @return An access point to AuthTokens with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbAuthTokensSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbAuthTokensSubset(targetIds: Set[Int]) extends ManyAuthTokensAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

