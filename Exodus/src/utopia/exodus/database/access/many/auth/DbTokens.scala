package utopia.exodus.database.access.many.auth

import utopia.exodus.model.stored.auth.Token
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.{NonDeprecatedView, UnconditionalView}
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple tokens at a time
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
object DbTokens extends ManyTokensAccess with NonDeprecatedView[Token]
{
	// COMPUTED --------------------
	
	/**
	  * @return A copy of this access point which also includes historical (revoked & expired) tokens
	  */
	def includingHistory = DbAllTokens
	
	
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted tokens
	  * @return An access point to tokens with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbTokensSubset(ids)
	
	
	// NESTED	--------------------
	
	/**
	  * An access point to tokens, including those that have been revoked and those that have expired
	  */
	object DbAllTokens extends ManyTokensAccess with UnconditionalView
	
	class DbTokensSubset(targetIds: Set[Int]) extends ManyTokensAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

