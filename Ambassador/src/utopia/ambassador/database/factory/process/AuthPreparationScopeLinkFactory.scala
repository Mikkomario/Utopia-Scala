package utopia.ambassador.database.factory.process

import utopia.ambassador.database.AmbassadorTables
import utopia.ambassador.model.partial.process.AuthPreparationScopeLinkData
import utopia.ambassador.model.stored.process.AuthPreparationScopeLink
import utopia.flow.datastructure.immutable.Model
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading AuthPreparationScopeLink data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object AuthPreparationScopeLinkFactory extends FromValidatedRowModelFactory[AuthPreparationScopeLink]
{
	// IMPLEMENTED	--------------------
	
	override def table = AmbassadorTables.authPreparationScopeLink
	
	override def defaultOrdering = None
	
	override def fromValidatedModel(valid: Model) =
		AuthPreparationScopeLink(valid("id").getInt, 
			AuthPreparationScopeLinkData(valid("preparationId").getInt, valid("scopeId").getInt))
}

