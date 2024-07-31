package utopia.citadel.database.access.many.organization

import utopia.citadel.database.access.many.user.DbUsers
import utopia.citadel.database.factory.organization.MembershipWithRolesFactory
import utopia.citadel.database.model.organization.MemberRoleLinkModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.combined.organization.{DetailedMembership, MembershipWithRoles}
import utopia.vault.database.Connection
import utopia.vault.sql.Condition

import java.time.Instant

object ManyMembershipsWithRolesAccess
{
	// OTHER    --------------------
	
	/**
	  * @param condition Search condition to apply
	  * @return Access to memberships that fulfill the specified search condition
	  */
	def apply(condition: Condition): ManyMembershipsWithRolesAccess = _Access(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _Access(accessCondition: Option[Condition]) extends ManyMembershipsWithRolesAccess
}

/**
  * A common trait for access points which return multiple memberships, including linked user role connections
  * @author Mikko Hilpinen
  * @since 24.10.2021, v2.0
  */
trait ManyMembershipsWithRolesAccess 
	extends ManyMembershipsAccessLike[MembershipWithRoles, ManyMembershipsWithRolesAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * Detailed copies of these memberships, including the roles, allowed tasks and user settings
	  * @param connection Implicit DB connection
	  */
	def detailed(implicit connection: Connection) = {
		// Reads membership data
		val membershipData = pull
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
					roleLink.withAccessToTasksWithIds(taskIdsForRoleId.getOrElse(roleLink.roleId, 
						Vector()).toSet) }
				DetailedMembership(membership.membership, rolesWithTasks, settings)
			}
		}
	}
	
	/**
	  * Model used for interacting with member role links
	  */
	protected def roleLinkModel = MemberRoleLinkModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = MembershipWithRolesFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyMembershipsWithRolesAccess = 
		ManyMembershipsWithRolesAccess(condition)
	
	override def isModifiedSince(threshold: Instant)(implicit connection: Connection) = 
		exists(model.startedColumn > threshold || model.deprecationColumn >
			 threshold ||roleLinkModel.createdColumn >
			 threshold || roleLinkModel.deprecatedAfterColumn > threshold)
	
	
	// OTHER	--------------------
	
	/**
	  * @param userRoleId A user role id
	  * @return An access point to memberships + member roles that refer to the specified user role
	  */
	def limitedToRoleWithId(userRoleId: Int) = filter(roleLinkModel.withRoleId(userRoleId).toCondition)
}

