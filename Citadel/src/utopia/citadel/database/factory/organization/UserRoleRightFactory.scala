package utopia.citadel.database.factory.organization

import utopia.citadel.database.CitadelTables
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.metropolis.model.partial.organization.UserRoleRightData
import utopia.metropolis.model.stored.organization.UserRoleRight
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading UserRoleRight data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object UserRoleRightFactory extends FromValidatedRowModelFactory[UserRoleRight]
{
	// IMPLEMENTED	--------------------
	
	override def table = CitadelTables.userRoleRight
	
	override def fromValidatedModel(valid: Model) =
		UserRoleRight(valid("id").getInt, UserRoleRightData(valid("roleId").getInt, valid("taskId").getInt, 
			valid("created").getInstant))
}

