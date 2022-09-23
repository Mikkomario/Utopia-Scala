package utopia.exodus.database.factory.auth

import utopia.exodus.database.ExodusTables
import utopia.exodus.model.partial.auth.TokenScopeLinkData
import utopia.exodus.model.stored.auth.TokenScopeLink
import utopia.flow.collection.value.typeless.Model
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading token scope link data from the DB
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
object TokenScopeLinkFactory extends FromValidatedRowModelFactory[TokenScopeLink]
{
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering = None
	
	override def table = ExodusTables.tokenScopeLink
	
	override def fromValidatedModel(valid: Model) = 
		TokenScopeLink(valid("id").getInt, TokenScopeLinkData(valid("tokenId").getInt, 
			valid("scopeId").getInt, valid("created").getInstant, valid("isDirectlyAccessible").getBoolean, 
			valid("grantsForward").getBoolean))
}

