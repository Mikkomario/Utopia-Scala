package utopia.exodus.database.access.single.auth

import utopia.exodus.database.factory.auth.TokenScopeLinkFactory
import utopia.exodus.database.model.auth.TokenScopeLinkModel
import utopia.exodus.model.stored.auth.TokenScopeLink
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual token scope links
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
object DbTokenScopeLink extends SingleRowModelAccess[TokenScopeLink] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = TokenScopeLinkModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = TokenScopeLinkFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted token scope link
	  * @return An access point to that token scope link
	  */
	def apply(id: Int) = DbSingleTokenScopeLink(id)
}

