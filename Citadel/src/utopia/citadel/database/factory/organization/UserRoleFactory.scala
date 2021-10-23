package utopia.citadel.database.factory.organization

import utopia.citadel.database.CitadelTables
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.metropolis.model.partial.organization.UserRoleData
import utopia.metropolis.model.stored.organization.UserRole
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading UserRole data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object UserRoleFactory extends FromValidatedRowModelFactory[UserRole]
{
	// IMPLEMENTED	--------------------
	
	override def table = CitadelTables.userRole
	
	override def fromValidatedModel(valid: Model[Constant]) = 
		UserRole(valid("id").getInt, UserRoleData(valid("created").getInstant))
}

