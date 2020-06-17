package utopia.exodus.database.factory.organization

import utopia.exodus.database.Tables
import utopia.exodus.database.model.organization.RoleRightModel
import utopia.exodus.model.stored.RoleRight
import utopia.flow.datastructure.template.{Model, Property}
import utopia.metropolis.model.enumeration.{TaskType, UserRole}
import utopia.vault.nosql.factory.FromRowModelFactory

object RoleRightFactory extends FromRowModelFactory[RoleRight]
{
	// COMPUTED	------------------------------
	
	private def model = RoleRightModel
	
	
	// IMPLEMENTED	--------------------------
	
	/**
	  * @return Table used by this class/object
	  */
	def table = Tables.roleRight
	
	override def apply(model: Model[Property]) = table.requirementDeclaration.validate(model).toTry.flatMap { valid =>
		// Both enumeration values must be parseable
		UserRole.forId(valid(this.model.roleIdAttName).getInt).flatMap { role =>
			TaskType.forId(valid("taskId").getInt).map { task => RoleRight(valid("id").getInt, role, task) }
		}
	}
}


