package utopia.citadel.database.access.many.organization

import utopia.citadel.database.factory.organization.MemberRoleWithRightsFactory
import utopia.citadel.database.model.organization.UserRoleRightModel
import utopia.metropolis.model.combined.organization.MemberRoleWithRights
import utopia.vault.database.Connection
import utopia.vault.sql.Condition

object ManyMemberRolesWithRightsAccess
{
	// OTHER	--------------------
	
	def apply(condition: Condition): ManyMemberRolesWithRightsAccess = SubAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class SubAccess(accessCondition: Option[Condition]) extends ManyMemberRolesWithRightsAccess
}

/**
  * A common trait for access points which target multiple member role links at a time and include
  *  allowed task ids
  * @author Mikko Hilpinen
  * @since 24.10.2021, v2.0
  */
trait ManyMemberRolesWithRightsAccess 
	extends ManyMemberRoleLinksAccessLike[MemberRoleWithRights, ManyMemberRolesWithRightsAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * Task ids allowed by this member role
	  * @param connection Implicit DB Connection
	  */
	def taskIds(implicit connection: Connection) = pullColumn(rightModel.taskIdColumn).flatMap { _.int }.toSet
	
	/**
	  * Model used for interacting with user role rights
	  */
	protected def rightModel = UserRoleRightModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = MemberRoleWithRightsFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyMemberRolesWithRightsAccess = 
		ManyMemberRolesWithRightsAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * @param taskId Id of the searched task
	  * @param connection Implicit DB Connection
	  * @return Whether this access point contains a link to that task
	  */
	def allowsTaskWithId(taskId: Int)(implicit connection: Connection) = 
		exists(rightModel.withTaskId(taskId).toCondition)
}

