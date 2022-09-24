package utopia.citadel.database.access.single.organization

import java.time.Instant
import utopia.citadel.database.factory.organization.UserRoleRightFactory
import utopia.citadel.database.model.organization.UserRoleRightModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.metropolis.model.stored.organization.UserRoleRight
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct UserRoleRights.
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait UniqueUserRoleRightAccess 
	extends SingleRowModelAccess[UserRoleRight] 
		with DistinctModelAccess[UserRoleRight, Option[UserRoleRight], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the organization member role that has authorization to perform the referenced task. None if no instance (or value) was found.
	  */
	def roleId(implicit connection: Connection) = pullColumn(model.roleIdColumn).int
	
	/**
	  * Id of the task the user's 
		with referenced membership role are allowed to perform. None if no instance (or value) was found.
	  */
	def taskId(implicit connection: Connection) = pullColumn(model.taskIdColumn).int
	
	/**
	  * Time when this UserRoleRight was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = UserRoleRightModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = UserRoleRightFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted UserRoleRight instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any UserRoleRight instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the roleId of the targeted UserRoleRight instance(s)
	  * @param newRoleId A new roleId to assign
	  * @return Whether any UserRoleRight instance was affected
	  */
	def roleId_=(newRoleId: Int)(implicit connection: Connection) = putColumn(model.roleIdColumn, newRoleId)
	
	/**
	  * Updates the taskId of the targeted UserRoleRight instance(s)
	  * @param newTaskId A new taskId to assign
	  * @return Whether any UserRoleRight instance was affected
	  */
	def taskId_=(newTaskId: Int)(implicit connection: Connection) = putColumn(model.taskIdColumn, newTaskId)
}

