package utopia.exodus.database.model.organization

import utopia.exodus.database.Tables
import utopia.exodus.database.factory.organization.RoleRightFactory
import utopia.exodus.model.stored.RoleRight
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.enumeration.{TaskType, UserRole}
import utopia.vault.model.immutable.StorableWithFactory

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
	def withRole(role: UserRole) = apply(role = Some(role))
	
	/**
	  * @param task A task type
	  * @return A model with only task type set
	  */
	def withTask(task: TaskType) = apply(task = Some(task))
}

/**
  * Used for interacting with role-task -links in DB
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  */
case class RoleRightModel(id: Option[Int] = None, role: Option[UserRole] = None, task: Option[TaskType] = None)
	extends StorableWithFactory[RoleRight]
{
	import RoleRightModel._
	
	// IMPLEMENTED	----------------------
	
	override def factory = RoleRightFactory
	
	override def valueProperties = Vector("id" -> id, roleIdAttName -> role.map { _.id }, taskIdAttName -> task.map { _.id })
}
