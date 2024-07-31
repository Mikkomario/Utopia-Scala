package utopia.citadel.database.access.many.organization

import utopia.citadel.database.factory.organization.UserRoleRightFactory
import utopia.citadel.database.model.organization.UserRoleRightModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.stored.organization.UserRoleRight
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

import java.time.Instant

object ManyUserRoleRightsAccess
{
	// OTHER	--------------------
	
	def apply(condition: Condition): ManyUserRoleRightsAccess = _Access(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _Access(accessCondition: Option[Condition]) extends ManyUserRoleRightsAccess
}

/**
  * A common trait for access points which target multiple UserRoleRights at a time
  * @author Mikko Hilpinen
  * @since 23.10.2021
  */
trait ManyUserRoleRightsAccess 
	extends ManyRowModelAccess[UserRoleRight] with Indexed with FilterableView[ManyUserRoleRightsAccess]
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
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyUserRoleRightsAccess = ManyUserRoleRightsAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted UserRoleRight instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any UserRoleRight instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * @param taskIds Ids of excluded tasks
	  * @return An access point to rights excluding those tasks
	  */
	def outsideTasks(taskIds: Iterable[Int]) = filter(!model.taskIdColumn.in(taskIds))
	
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
	
	/**
	  * @param roleIds Ids of the targeted user roles
	  * @return An access point to role right links concerning those user roles
	  */
	def withAnyOfRoles(roleIds: Iterable[Int]) = filter(model.roleIdColumn in roleIds)
	
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
}

