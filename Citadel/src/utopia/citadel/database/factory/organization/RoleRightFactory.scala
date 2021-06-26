package utopia.citadel.database.factory.organization

import utopia.citadel.database.Tables
import utopia.citadel.database.model.organization.RoleRightModel
import utopia.flow.datastructure.immutable
import utopia.flow.datastructure.immutable.Constant
import utopia.flow.generic.ValueUnwraps._
import utopia.metropolis.model.stored.RoleRight
import utopia.vault.nosql.factory.FromValidatedRowModelFactory

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


