package utopia.citadel.database.access.many.organization

import utopia.citadel.database.access.many.user.DbUsers

import utopia.citadel.database.factory.organization.{MembershipFactory, MembershipWithRolesFactory}
import utopia.metropolis.model.combined.organization.DetailedMembership
import utopia.metropolis.model.stored.organization.Membership
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyMembershipsAccess
{
	// NESTED	--------------------
	
	private class ManyMembershipsSubView(override val parent: ManyRowModelAccess[Membership], 
		override val filterCondition: Condition) 
		extends ManyMembershipsAccess with SubView
}

/**
  * A common trait for access points which target multiple Memberships at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait ManyMembershipsAccess
	extends ManyMembershipsAccessLike[Membership, ManyMembershipsAccess] with ManyRowModelAccess[Membership]
{
	// COMPUTED	--------------------
	
	/**
	  * @return Factory used for reading membership data, including role links
	  */
	protected def withRolesFactory = MembershipWithRolesFactory
	
	/**
	  * @param connection Implicit DB Connection
	  * @return Accessible memberships, with their role links included
	  */
	// TODO: This should refer to a separate access point
	def withRoles(implicit connection: Connection) = globalCondition match
	{
		case Some(condition) => withRolesFactory.getMany(condition)
		case None => withRolesFactory.getAll()
	}
	/**
	  * @param connection Implicit DB connection
	  * @return Detailed copies of these memberships, including the roles, allowed tasks and user settings
	  */
	def detailed(implicit connection: Connection) =
	{
		// Reads membership data
		val membershipData = withRoles
		// Reads rights data
		val roleIds = membershipData.flatMap { _.roleIds }.toSet
		val roleRights = if (roleIds.isEmpty) Vector() else DbUserRoleRights.withAnyOfRoles(roleIds).pull
		val taskIdsForRoleId = roleRights.groupMap { _.roleId } { _.taskId }
		// Reads user settings
		val settings = DbUsers(membershipData.map { _.wrapped.userId }.toSet).settings.pull
		// Combines the data
		membershipData.flatMap { membership =>
			settings.find { _.userId == membership.userId }.map { settings =>
				val rolesWithTasks = membership.roleLinks.map { roleLink =>
					roleLink.withAccessToTasksWithIds(taskIdsForRoleId.getOrElse(roleLink.roleId, Vector()).toSet) }
				DetailedMembership(membership.membership, rolesWithTasks, settings)
			}
		}
	}
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = MembershipFactory
	
	override protected def defaultOrdering = Some(factory.defaultOrdering)
	
	override def _filter(additionalCondition: Condition): ManyMembershipsAccess =
		new ManyMembershipsAccess.ManyMembershipsSubView(this, additionalCondition)
}

