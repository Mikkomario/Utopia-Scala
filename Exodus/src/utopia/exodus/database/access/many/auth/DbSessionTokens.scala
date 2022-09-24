package utopia.exodus.database.access.many.auth

import utopia.exodus.model.stored.auth.SessionToken
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.NonDeprecatedView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple SessionTokens at a time
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Will be removed in a future release", "v4.0")
object DbSessionTokens extends ManySessionTokensAccess with NonDeprecatedView[SessionToken]
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted SessionTokens
	  * @return An access point to SessionTokens with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbSessionTokensSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbSessionTokensSubset(targetIds: Set[Int]) extends ManySessionTokensAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

