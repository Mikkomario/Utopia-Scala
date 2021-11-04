package utopia.citadel.database.model.organization

import java.time.Instant
import utopia.citadel.database.factory.organization.UserRoleRightFactory
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.organization.UserRoleRightData
import utopia.metropolis.model.stored.organization.UserRoleRight
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing UserRoleRightModel instances and for inserting UserRoleRights to the database
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object UserRoleRightModel extends DataInserter[UserRoleRightModel, UserRoleRight, UserRoleRightData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains UserRoleRight roleId
	  */
	val roleIdAttName = "roleId"
	
	/**
	  * Name of the property that contains UserRoleRight taskId
	  */
	val taskIdAttName = "taskId"
	
	/**
	  * Name of the property that contains UserRoleRight created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains UserRoleRight roleId
	  */
	def roleIdColumn = table(roleIdAttName)
	
	/**
	  * Column that contains UserRoleRight taskId
	  */
	def taskIdColumn = table(taskIdAttName)
	
	/**
	  * Column that contains UserRoleRight created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = UserRoleRightFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: UserRoleRightData) = 
		apply(None, Some(data.roleId), Some(data.taskId), Some(data.created))
	
	override def complete(id: Value, data: UserRoleRightData) = UserRoleRight(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this UserRoleRight was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param id A UserRoleRight id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param roleId Id of the organization member role that has authorization to perform the referenced task
	  * @return A model containing only the specified roleId
	  */
	def withRoleId(roleId: Int) = apply(roleId = Some(roleId))
	
	/**
	  * @param taskId Id of the task the user's with referenced membership role are allowed to perform
	  * @return A model containing only the specified taskId
	  */
	def withTaskId(taskId: Int) = apply(taskId = Some(taskId))
}

/**
  * Used for interacting with UserRoleRights in the database
  * @param id UserRoleRight database id
  * @param roleId Id of the organization member role that has authorization to perform the referenced task
  * @param taskId Id of the task the user's with referenced membership role are allowed to perform
  * @param created Time when this UserRoleRight was first created
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class UserRoleRightModel(id: Option[Int] = None, roleId: Option[Int] = None, taskId: Option[Int] = None, 
	created: Option[Instant] = None) 
	extends StorableWithFactory[UserRoleRight]
{
	// IMPLEMENTED	--------------------
	
	override def factory = UserRoleRightModel.factory
	
	override def valueProperties = 
	{
		import UserRoleRightModel._
		Vector("id" -> id, roleIdAttName -> roleId, taskIdAttName -> taskId, createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param roleId A new roleId
	  * @return A new copy of this model with the specified roleId
	  */
	def withRoleId(roleId: Int) = copy(roleId = Some(roleId))
	
	/**
	  * @param taskId A new taskId
	  * @return A new copy of this model with the specified taskId
	  */
	def withTaskId(taskId: Int) = copy(taskId = Some(taskId))
}

