package utopia.exodus.database.model.organization

import utopia.exodus.database.Tables
import utopia.exodus.database.factory.organization.RoleRightFactory
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.enumeration.{TaskType, UserRole}
import utopia.metropolis.model.stored.RoleRight
import utopia.vault.model.immutable.StorableWithFactory

@deprecated("Please use the Citadel version instead", "v2.0")
object RoleRightModel
{
	// ATTRIBUTES	--------------------------
	
	/**
	  * Name of the attribute that holds the role id
	  */
	val roleIdAttName = "roleId"
	
	/**
	  * Name of the attribute that holds the task id
	  */
	val taskIdAttName = "taskId"
	
	
	// COMPUTED	------------------------------
	
	/**
	  * @return Table used by this class/object
	  */
	def table = Tables.roleRight
	
	/**
	  * @return Column that holds the role id
	  */
	def roleIdColumn = table(roleIdAttName)
	
	/**
	  * @return Column that holds the task id
	  */
	def taskIdColumn = table(taskIdAttName)
	
	
	// OTHER	------------------------------
	
	/**
	  * @param role A user role
	  * @return A model with only that role set
	  */
	@deprecated("Please use .withRoleId(Int) instead", "v1")
	def withRole(role: UserRole) = withRoleId(role.id)
	
	/**
	  * @param roleId Id of a user role
	  * @return A model with only role id set
	  */
	def withRoleId(roleId: Int) = apply(roleId = Some(roleId))
	
	/**
	  * @param task A task type
	  * @return A model with only task type set
	  */
	@deprecated("Please use .withTaskId(Int) instead", "v1")
	def withTask(task: TaskType) = withTaskId(task.id)
	
	/**
	  * @param taskId Id of a task
	  * @return A model with only task id set
	  */
	def withTaskId(taskId: Int) = apply(taskId = Some(taskId))
}

/**
  * Used for interacting with role-task -links in DB
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  */
@deprecated("Please use the Citadel version instead", "v2.0")
case class RoleRightModel(id: Option[Int] = None, roleId: Option[Int] = None, taskId: Option[Int] = None)
	extends StorableWithFactory[RoleRight]
{
	import RoleRightModel._
	
	// IMPLEMENTED	----------------------
	
	override def factory = RoleRightFactory
	
	override def valueProperties = Vector("id" -> id, roleIdAttName -> roleId, taskIdAttName -> taskId)
}
