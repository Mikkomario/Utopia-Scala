package utopia.exodus.database.access.single

import java.time.{Instant, Period}

import utopia.exodus.database.access.many.{DbDescriptions, DbUserRoles, DbUsers, InvitationsAccess, OrganizationDeletionsAccess}
import utopia.exodus.database.factory.organization.{MembershipFactory, MembershipWithRolesFactory, RoleRightFactory}
import utopia.exodus.database.model.organization.{DeletionModel, MemberRoleModel, MembershipModel}
import utopia.flow.util.TimeExtensions._
import utopia.metropolis.model.combined.organization.DescribedMembership
import utopia.metropolis.model.partial.organization.{DeletionData, InvitationData, MembershipData}
import utopia.metropolis.model.stored.organization.Membership
import utopia.vault.database.Connection
import utopia.vault.nosql.access.ManyRowModelAccess
import utopia.vault.sql.{Select, Where}

/**
  * Used for accessing individual organizations
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  */
object DbOrganization
{
	// OTHER	----------------------------
	
	/**
	  * @param id An organization id
	  * @return An access point to that organization's data
	  */
	def apply(id: Int) = new SingleOrganization(id)
	
	
	// NESTED	----------------------------
	
	class SingleOrganization(organizationId: Int)
	{
		// COMPUTED	------------------------------
		
		/**
		  * @return An access point to this organization's memberships (organization-user-links)
		  */
		def memberships = Memberships
		
		/**
		  * @return An access point to invitations to join this organization
		  */
		def invitations = Invitations
		
		/**
		  * @return An access point to deletions targeting this organization
		  */
		def deletions = Deletions
		
		/**
		  * @return An access point to descriptions of this organization
		  */
		def descriptions = DbDescriptions.ofOrganizationWithId(organizationId)
		
		
		// NESTED	-------------------------------
		
		object Memberships extends ManyRowModelAccess[Membership]
		{
			// COMPUTED	---------------------------
			
			private def memberRolesFactory = MemberRoleModel
			
			private def withRolesFactory = MembershipWithRolesFactory
			
			private def roleRightsFactory = RoleRightFactory
			
			private def model = MembershipModel
			
			private def condition = model.withOrganizationId(organizationId).toCondition &&
				factory.nonDeprecatedCondition
			
			/**
			  * @param connection Implicit DB connection
			  * @return All memberships in this organization, including the roles, allowed tasks and user settings
			  */
			def described(implicit connection: Connection) =
			{
				// Reads membership data
				val membershipData = withRolesFactory.getMany(condition)
				// Reads role and task data
				val roleIds = membershipData.flatMap { _.roleIds }.toSet
				val rolesWithRights = DbUserRoles(roleIds).withRights
				// Reads user settings
				val settings = DbUsers(membershipData.map { _.wrapped.userId }.toSet).settings
				// Combines the data
				membershipData.flatMap { membership =>
					settings.find { _.userId == membership.wrapped.userId }.map { settings =>
						val roles = membership.roleIds.flatMap { roleId => rolesWithRights.find { _.roleId == roleId } }
						DescribedMembership(membership, roles, settings)
					}
				}
			}
			
			
			// IMPLEMENTED	-----------------------
			
			override def factory = MembershipFactory
			
			override def globalCondition = Some(condition)
			
			
			// OTHER	---------------------------
			
			/**
			  * @param roleId Searched user role's id
			  * @param connection DB Connection (implicit)
			  * @return Members within this organization that have the specified role
			  */
			def withRole(roleId: Int)(implicit connection: Connection) =
			{
				// Needs to join into role rights
				connection(Select(factory.target join memberRolesFactory.table, factory.table) +
					Where(mergeCondition(memberRolesFactory.withRoleId(roleId).toCondition))).parse(factory)
			}
			
			/**
			  * Inserts a new membership, along with a single role
			  * @param userId Id of the new organization member (user)
			  * @param startingRoleId Id of the role given to the user in this organization
			  * @param creatorId Id of the user who authorized / added this membership
			  * @param connection DB Connection (implicit)
			  * @return Inserted membership
			  */
			def insert(userId: Int, startingRoleId: Int, creatorId: Int)(implicit connection: Connection) =
			{
				// Adds membership
				val newMembership = model.insert(MembershipData(organizationId, userId, Some(creatorId)))
				// Adds user role
				memberRolesFactory.insert(newMembership.id, startingRoleId, creatorId)
				newMembership
			}
		}
		
		object Invitations extends InvitationsAccess
		{
			// IMPLEMENTED	------------------------
			
			override def globalCondition = Some(model.withOrganizationId(organizationId).toCondition)
			
			
			// OTHER	---------------------------
			
			/**
			  * Sends a new invitation. Please make sure the user is allowed to send this invitation before calling this
			  * method.
			  * @param recipient Either recipient user id (right) or recipient user email (left)
			  * @param roleId Id of the role the user will receive upon accepting this invitation
			  * @param senderId Id of the user sending this invitation
			  * @param validDuration Duration how long the invitation can still be answered (default = 1 week)
			  * @param connection DB Connection (implicit)
			  * @return Newly saved invitation
			  */
			def send(recipient: Either[String, Int], roleId: Int, senderId: Int, validDuration: Period = 7.days)
					(implicit connection: Connection) =
			{
				model.insert(InvitationData(organizationId, recipient, roleId, Instant.now() + validDuration, Some(senderId)))
			}
		}
		
		object Deletions extends OrganizationDeletionsAccess
		{
			// IMPLEMENTED	----------------------
			
			override def globalCondition = Some(DeletionModel.withOrganizationId(organizationId).toCondition)
			
			
			// OTHER	--------------------------
			
			/**
			  * Inserts a new deletion for this organization
			  * @param creatorId Id of the user starting the deletion process
			  * @param actualizationDelay Delay before actualizing this deletion
			  * @param connection DB Connection (implicit)
			  * @return Newly inserted deletion
			  */
			def insert(creatorId: Int, actualizationDelay: Period)(implicit connection: Connection) =
				DeletionModel.insert(DeletionData(organizationId, creatorId, Instant.now() + actualizationDelay))
		}
	}
}
