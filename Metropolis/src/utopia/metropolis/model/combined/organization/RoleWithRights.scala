package utopia.metropolis.model.combined.organization

import utopia.flow.datastructure.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, IntType, ModelConvertible}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._
import utopia.metropolis.model.Extender
import utopia.metropolis.model.enumeration.{TaskType, UserRole}

object RoleWithRights extends FromModelFactory[RoleWithRights]
{
	// ATTRIBUTES	------------------------------
	
	private val schema = ModelDeclaration(PropertyDeclaration("id", IntType))
	
	
	// IMPLEMENTED	------------------------------
	
	override def apply(model: template.Model[Property]) = schema.validate(model).toTry.flatMap { valid =>
		UserRole.forId(valid("id")).map { role =>
			// Ignores invalid task ids
			RoleWithRights(role,
				valid("task_ids").getVector.flatMap { _.int }.flatMap { TaskType.forId(_).toOption }.toSet)
		}
	}
}

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
