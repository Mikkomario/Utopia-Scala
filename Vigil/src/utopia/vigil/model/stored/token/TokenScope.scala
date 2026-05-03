package utopia.vigil.model.stored.token

import utopia.vault.store.StandardStoredFactory
import utopia.vigil.database.access.token.scope.AccessTokenScope
import utopia.vigil.model.factory.token.TokenScopeFactoryWrapper
import utopia.vigil.model.partial.scope.ScopeRightData
import utopia.vigil.model.partial.token.TokenScopeData
import utopia.vigil.model.stored.scope.StoredScopeRightLike

object TokenScope extends StandardStoredFactory[TokenScopeData, TokenScope]
{
	// ATTRIBUTES	--------------------
	
	override val dataFactory = TokenScopeData
}

/**
  * Represents a token scope that has already been stored in the database. 
  * Allows a token to be used in some scope
  * @param id   ID of this token scope in the database
  * @param data Wrapped token scope data
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class TokenScope(id: Int, data: TokenScopeData) 
	extends TokenScopeFactoryWrapper[TokenScopeData, TokenScope] with ScopeRightData 
		with StoredScopeRightLike[TokenScopeData, TokenScope]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this token scope in the database
	  */
	def access = AccessTokenScope(id)
	
	
	// IMPLEMENTED	--------------------
	
	override def withId(id: Int) = copy(id = id)
	
	override protected def wrap(data: TokenScopeData) = copy(data = data)
}

