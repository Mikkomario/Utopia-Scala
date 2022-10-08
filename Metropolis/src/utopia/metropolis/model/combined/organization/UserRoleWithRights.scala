package utopia.metropolis.model.combined.organization

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.template.ModelConvertible

object UserRoleWithRights extends FromModelFactoryWithSchema[UserRoleWithRights]
{
	// ATTRIBUTES	------------------------------
	
	val schema = ModelDeclaration(PropertyDeclaration("id", IntType))
	
	
	// IMPLEMENTED	------------------------------
	
	override protected def fromValidatedModel(model: Model) =
		UserRoleWithRights(model("id"), model("task_ids").getVector.flatMap { _.int }.toSet)
}

/**
  * Contains a role + all tasks that are linked to that role
  * @author Mikko Hilpinen
  * @since 10.5.2020, v1
  * @constructor Links rights with a role
  * @param roleId Described role id
  * @param taskIds Ids of the tasks available to that role
  */
case class UserRoleWithRights(roleId: Int, taskIds: Set[Int]) extends ModelConvertible
{
	/**
	  * @return Id of this user role
	  */
	def id = roleId
	
	override def toModel = Model(Vector("id" -> roleId, "task_ids" -> taskIds.toVector.sorted))
}
