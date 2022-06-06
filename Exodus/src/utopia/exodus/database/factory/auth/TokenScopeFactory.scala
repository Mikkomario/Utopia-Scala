package utopia.exodus.database.factory.auth

import utopia.exodus.model.combined.auth.TokenScope
import utopia.exodus.model.stored.auth.{Scope, TokenScopeLink}
import utopia.vault.nosql.factory.row.linked.CombiningFactory

/**
  * Used for reading token scopes from the database
  * @author Mikko Hilpinen
  * @since 19.02.2022, v4.0
  */
object TokenScopeFactory extends CombiningFactory[TokenScope, Scope, TokenScopeLink]
{
	// IMPLEMENTED	--------------------
	
	override def childFactory = TokenScopeLinkFactory
	
	override def parentFactory = ScopeFactory
	
	override def apply(scope: Scope, tokenLink: TokenScopeLink) = TokenScope(scope, tokenLink)
}

