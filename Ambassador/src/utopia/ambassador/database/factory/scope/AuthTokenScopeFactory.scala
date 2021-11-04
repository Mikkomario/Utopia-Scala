package utopia.ambassador.database.factory.scope

import utopia.ambassador.database.factory.token.AuthTokenScopeLinkFactory
import utopia.ambassador.model.combined.scope.AuthTokenScope
import utopia.ambassador.model.stored.scope.Scope
import utopia.ambassador.model.stored.token.AuthTokenScopeLink
import utopia.vault.nosql.factory.row.linked.CombiningFactory

/**
  * Used for reading AuthTokenScopes from the database
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object AuthTokenScopeFactory extends CombiningFactory[AuthTokenScope, Scope, AuthTokenScopeLink]
{
	// IMPLEMENTED	--------------------
	
	override def childFactory = AuthTokenScopeLinkFactory
	
	override def parentFactory = ScopeFactory
	
	override def apply(scope: Scope, tokenLink: AuthTokenScopeLink) = AuthTokenScope(scope, tokenLink)
}

