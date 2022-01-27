package utopia.ambassador.database.factory.token

import utopia.ambassador.database.AmbassadorTables
import utopia.ambassador.model.partial.token.AuthTokenScopeLinkData
import utopia.ambassador.model.stored.token.AuthTokenScopeLink
import utopia.flow.datastructure.immutable.Model
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading AuthTokenScopeLink data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object AuthTokenScopeLinkFactory extends FromValidatedRowModelFactory[AuthTokenScopeLink]
{
	// IMPLEMENTED	--------------------
	
	override def table = AmbassadorTables.authTokenScopeLink
	
	override def defaultOrdering = None
	
	override def fromValidatedModel(valid: Model) =
		AuthTokenScopeLink(valid("id").getInt, AuthTokenScopeLinkData(valid("tokenId").getInt, 
			valid("scopeId").getInt))
}

