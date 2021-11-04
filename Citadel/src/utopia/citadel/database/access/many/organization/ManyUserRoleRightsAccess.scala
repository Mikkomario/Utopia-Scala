package utopia.citadel.database.access.many.organization

import java.time.Instant
import utopia.citadel.database.factory.organization.UserRoleRightFactory
import utopia.citadel.database.model.organization.UserRoleRightModel
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.organization.UserRoleRight
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition
import utopia.vault.sql.SqlExtensions._

object ManyUserRoleRightsAccess
{
	// NESTED	--------------------
	
	private class ManyUserRoleRightsSubView(override val parent: ManyRowModelAccess[UserRoleRight], 
		override val filterCondition: Condition) 
		extends ManyUserRoleRightsAccess with SubView
}

/**
  * A common trait for access points which target multiple UserRoleRights at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait ManyUserRoleRightsAccess extends ManyRowModelAccess[UserRoleRight] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * roleIds of the accessible UserRoleRights
	  */
	def roleIds(implicit connection: Connection) = pullColumn(model.roleIdColumn)
		.flatMap { value => value.int }
	/**
	  * taskIds of the accessible UserRoleRights
	  */
	def taskIds(implicit connection: Connection) = pullColumn(model.taskIdColumn)
		.flatMap { value => value.int }
	/**
	  * creationTimes of the accessible UserRoleRights
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = UserRoleRightModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = UserRoleRightFactory
	
	override protected def defaultOrdering = None
	
	override def filter(additionalCondition: Condition): ManyUserRoleRightsAccess = 
		new ManyUserRoleRightsAccess.ManyUserRoleRightsSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * @param userRoleId Id of the targeted user role
	  * @return An access point to links for that role
	  */
	def withRoleId(userRoleId: Int) = filter(model.withRoleId(userRoleId).toCondition)
	/**
	  * @param taskId Id of the targeted task id
	  * @return An access point to links to that task
	  */
	def withTaskId(taskId: Int) = filter(model.withTaskId(taskId).toCondition)
	
	/**
	  * @param roleIds Ids of the targeted user roles
	  * @return An access point to role right links concerning those user roles
	  */
	def withAnyOfRoles(roleIds: Iterable[Int]) = filter(model.roleIdColumn in roleIds)
	
	/**
	  * Updates the created of the targeted UserRoleRight instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any UserRoleRight instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	/**
	  * Updates the roleId of the targeted UserRoleRight instance(s)
	  * @param newRoleId A new roleId to assign
	  * @return Whether any UserRoleRight instance was affected
	  */
	def roleIds_=(newRoleId: Int)(implicit connection: Connection) = putColumn(model.roleIdColumn, newRoleId)
	/**
	  * Updates the taskId of the targeted UserRoleRight instance(s)
	  * @param newTaskId A new taskId to assign
	  * @return Whether any UserRoleRight instance was affected
	  */
	def taskIds_=(newTaskId: Int)(implicit connection: Connection) = putColumn(model.taskIdColumn, newTaskId)
}

