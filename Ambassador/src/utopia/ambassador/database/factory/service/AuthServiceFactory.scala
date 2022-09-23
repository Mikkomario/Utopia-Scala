package utopia.ambassador.database.factory.service

import utopia.ambassador.database.AmbassadorTables
import utopia.ambassador.model.partial.service.AuthServiceData
import utopia.ambassador.model.stored.service.AuthService
import utopia.flow.collection.value.typeless.Model
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading AuthService data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object AuthServiceFactory extends FromValidatedRowModelFactory[AuthService]
{
	// IMPLEMENTED	--------------------
	
	override def table = AmbassadorTables.authService
	
	override def defaultOrdering = None
	
	override def fromValidatedModel(valid: Model) =
		AuthService(valid("id").getInt, AuthServiceData(valid("name").getString, valid("created").getInstant))
}

