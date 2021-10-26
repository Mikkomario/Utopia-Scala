package utopia.ambassador.database.factory.scope

import utopia.ambassador.database.factory.process.AuthPreparationScopeLinkFactory
import utopia.ambassador.model.combined.scope.AuthPreparationScope
import utopia.ambassador.model.stored.process.AuthPreparationScopeLink
import utopia.ambassador.model.stored.scope.Scope
import utopia.vault.nosql.factory.row.linked.CombiningFactory

/**
  * Used for reading AuthPreparationScopes from the database
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object AuthPreparationScopeFactory 
	extends CombiningFactory[AuthPreparationScope, Scope, AuthPreparationScopeLink]
{
	// IMPLEMENTED	--------------------
	
	override def childFactory = AuthPreparationScopeLinkFactory
	
	override def parentFactory = ScopeFactory
	
	override def apply(scope: Scope, preparationLink: AuthPreparationScopeLink) = 
		AuthPreparationScope(scope, preparationLink)
}

