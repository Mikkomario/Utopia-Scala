package utopia.ambassador.database.access.single.token

import utopia.ambassador.database.factory.token.AuthTokenScopeLinkFactory
import utopia.ambassador.database.model.token.AuthTokenScopeLinkModel
import utopia.ambassador.model.stored.token.AuthTokenScopeLink
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual AuthTokenScopeLinks
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object DbAuthTokenScopeLink 
	extends SingleRowModelAccess[AuthTokenScopeLink] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthTokenScopeLinkModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthTokenScopeLinkFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted AuthTokenScopeLink instance
	  * @return An access point to that AuthTokenScopeLink
	  */
	def apply(id: Int) = DbSingleAuthTokenScopeLink(id)
}

