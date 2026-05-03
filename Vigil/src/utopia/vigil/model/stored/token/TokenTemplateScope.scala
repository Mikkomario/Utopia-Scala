package utopia.vigil.model.stored.token

import utopia.vault.store.StandardStoredFactory
import utopia.vigil.database.access.token.template.right.AccessTokenTemplateScope
import utopia.vigil.model.factory.token.TokenTemplateScopeFactoryWrapper
import utopia.vigil.model.partial.scope.ScopeRightData
import utopia.vigil.model.partial.token.TokenTemplateScopeData
import utopia.vigil.model.stored.scope.StoredScopeRightLike

object TokenTemplateScope extends StandardStoredFactory[TokenTemplateScopeData, TokenTemplateScope]
{
	// ATTRIBUTES	--------------------
	
	override val dataFactory = TokenTemplateScopeData
}

/**
  * Represents a token template scope that has already been stored in the database. 
  * Links a (granted) scope to a token template
  * @param id   ID of this token template scope in the database
  * @param data Wrapped token template scope data
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class TokenTemplateScope(id: Int, data: TokenTemplateScopeData) 
	extends TokenTemplateScopeFactoryWrapper[TokenTemplateScopeData, TokenTemplateScope] with ScopeRightData 
		with StoredScopeRightLike[TokenTemplateScopeData, TokenTemplateScope]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this token template scope in the database
	  */
	def access = AccessTokenTemplateScope(id)
	
	
	// IMPLEMENTED	--------------------
	
	override def withId(id: Int) = copy(id = id)
	
	override protected def wrap(data: TokenTemplateScopeData) = copy(data = data)
}

