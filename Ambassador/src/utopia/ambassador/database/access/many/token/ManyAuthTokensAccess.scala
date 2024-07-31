package utopia.ambassador.database.access.many.token

import utopia.ambassador.database.factory.token.AuthTokenFactory
import utopia.ambassador.model.stored.token.AuthToken
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.sql.Condition

object ManyAuthTokensAccess
{
	// OTHER	--------------------
	
	def apply(condition: Condition): ManyAuthTokensAccess = _Access(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _Access(accessCondition: Option[Condition]) extends ManyAuthTokensAccess
}

/**
  * A common trait for access points which target multiple AuthTokens at a time
  * @author Mikko Hilpinen
  * @since 26.10.2021
  */
trait ManyAuthTokensAccess 
	extends ManyAuthTokensAccessLike[AuthToken, ManyAuthTokensAccess] with ManyRowModelAccess[AuthToken]
{
	// COMPUTED	--------------------
	
	/**
	  * A copy of this access point with scope data included
	  */
	def withScopes = {
		accessCondition match 
		{
			case Some(c) => DbAuthTokensWithScopes.filter(c)
			case None => DbAuthTokensWithScopes
		}
	}
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthTokenFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyAuthTokensAccess = ManyAuthTokensAccess(condition)
}

