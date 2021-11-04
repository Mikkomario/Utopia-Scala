package utopia.citadel.database.access.many.organization

import utopia.citadel.database.factory.organization.MemberRoleWithRightsFactory
import utopia.citadel.database.model.organization.UserRoleRightModel
import utopia.metropolis.model.combined.organization.MemberRoleWithRights
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyMemberRolesWithRightsAccess
{
	private class RoleLinksSubView(override val parent: ManyModelAccess[MemberRoleWithRights],
	                               override val filterCondition: Condition)
		extends ManyMemberRolesWithRightsAccess with SubView
}

/**
  * A common trait for access points which target multiple member role links at a time and include allowed task ids
  * @author Mikko Hilpinen
  * @since 24.10.2021, v2.0
  */
trait ManyMemberRolesWithRightsAccess
	extends ManyMemberRoleLinksAccessLike[MemberRoleWithRights, ManyMemberRolesWithRightsAccess]
{
	// COMPUTED ----------------------------------
	
	/**
	  * @return Model used for interacting with user role rights
	  */
	protected def rightModel = UserRoleRightModel
	
	/**
	  * @param connection Implicit DB Connection
	  * @return Task ids allowed by this member role
	  */
	def taskIds(implicit connection: Connection) =
		pullColumn(rightModel.taskIdColumn).flatMap { _.int }.toSet
	
	
	// IMPLEMENTED  ------------------------------
	
	override def factory = MemberRoleWithRightsFactory
	override protected def defaultOrdering = None
	
	override def _filter(additionalCondition: Condition): ManyMemberRolesWithRightsAccess =
		new ManyMemberRolesWithRightsAccess.RoleLinksSubView(this, additionalCondition)
		
	
	// OTHER    ---------------------------------
	
	/**
	  * @param taskId Id of the searched task
	  * @param connection Implicit DB Connection
	  * @return Whether this access point contains a link to that task
	  */
	def allowsTaskWithId(taskId: Int)(implicit connection: Connection) =
		exists(rightModel.withTaskId(taskId).toCondition)
}
