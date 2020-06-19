package utopia.metropolis.model.combined.organization

import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.Extender
import utopia.metropolis.model.enumeration.{TaskType, UserRole}

/**
  * Contains a role + all tasks that are linked to that role
  * @author Mikko Hilpinen
  * @since 10.5.2020, v1
  * @constructor Links rights with a role
  * @param role Described role
  * @param tasks Tasks available to that role
  */
case class RoleWithRights(role: UserRole, tasks: Set[TaskType]) extends Extender[UserRole] with ModelConvertible
{
	override def wrapped = role
	
	override def toModel = Model(Vector("id" -> role.id,
		"task_ids" -> tasks.map { _.id }.toVector.sorted))
}
