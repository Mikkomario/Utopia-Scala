package utopia.ambassador.database.access.many.token

import utopia.ambassador.database.factory.token.AuthTokenFactory
import utopia.ambassador.model.stored.token.AuthToken
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyAuthTokensAccess
{
	// NESTED	--------------------
	
	private class ManyAuthTokensSubView(override val parent: ManyRowModelAccess[AuthToken], 
		override val filterCondition: Condition) 
		extends ManyAuthTokensAccess with SubView
}

/**
  * A common trait for access points which target multiple AuthTokens at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
trait ManyAuthTokensAccess
	extends ManyAuthTokensAccessLike[AuthToken, ManyAuthTokensAccess] with ManyRowModelAccess[AuthToken]
{
	// COMPUTED ------------------------
	
	/**
	  * @return A copy of this access point with scope data included
	  */
	def withScopes = globalCondition match
	{
		case Some(c) => DbAuthTokensWithScopes.filter(c)
		case None => DbAuthTokensWithScopes
	}
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override def factory = AuthTokenFactory
	
	override def _filter(additionalCondition: Condition): ManyAuthTokensAccess =
		new ManyAuthTokensAccess.ManyAuthTokensSubView(this, additionalCondition)
}

