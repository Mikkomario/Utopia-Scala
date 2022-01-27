package utopia.citadel.database.factory.user

import utopia.citadel.database.CitadelTables
import utopia.flow.datastructure.immutable.Model
import utopia.metropolis.model.partial.user.UserData
import utopia.metropolis.model.stored.user.User
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading User data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object UserFactory extends FromValidatedRowModelFactory[User]
{
	// IMPLEMENTED	--------------------
	
	override def table = CitadelTables.user
	
	override def defaultOrdering = None
	
	override def fromValidatedModel(valid: Model) =
		User(valid("id").getInt, UserData(valid("created").getInstant))
}

