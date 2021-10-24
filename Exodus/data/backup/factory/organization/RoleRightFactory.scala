package utopia.exodus.database.factory.organization

import utopia.exodus.database.Tables
import utopia.exodus.database.model.organization.RoleRightModel
import utopia.flow.datastructure.immutable
import utopia.flow.datastructure.immutable.Constant
import utopia.flow.generic.ValueUnwraps._
import utopia.metropolis.model.stored.RoleRight
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

@deprecated("Please use the Citadel version instead", "v2.0")
object RoleRightFactory extends FromValidatedRowModelFactory[RoleRight]
{
	// COMPUTED	------------------------------
	
	private def model = RoleRightModel
	
	
	// IMPLEMENTED	--------------------------
	
	/**
	  * @return Table used by this class/object
	  */
	def table = Tables.roleRight
	
	override protected def fromValidatedModel(model: immutable.Model[Constant]) =
		RoleRight(model("id"), model(this.model.roleIdAttName), model("taskId"))
}


