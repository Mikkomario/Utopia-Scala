package utopia.exodus.database.access.many.auth

import utopia.exodus.model.combined.auth.TypedToken
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.view.{NonDeprecatedView, UnconditionalView}
import utopia.vault.sql.SqlExtensions._

/**
  * A root access point to tokens, including their type information
  * @author Mikko Hilpinen
  * @since 20.2.2022, v4.0
  */
object DbTypedTokens extends ManyTypedTokensAccess with NonDeprecatedView[TypedToken]
{
	// COMPUTED ----------------------------
	
	/**
	  * @return A copy of this access point where historical information is included
	  */
	def includingHistory = DbAllTypedTokens
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param tokenIds Ids of targeted tokens
	  * @return An access point to tokens with those ids
	  */
	def apply(tokenIds: Iterable[Int]) = new DbTypedTokensSubset(tokenIds)
	
	
	// NESTED   ----------------------------
	
	/**
	  * A root access point to tokens, whether they be active or not, where type information is included
	  */
	object DbAllTypedTokens extends ManyTypedTokensAccess with UnconditionalView
	
	class DbTypedTokensSubset(_ids: Iterable[Int]) extends ManyTypedTokensAccess
	{
		override def ids(implicit connection: Connection) = _ids.toVector
		
		override def globalCondition = Some(index in _ids)
	}
}