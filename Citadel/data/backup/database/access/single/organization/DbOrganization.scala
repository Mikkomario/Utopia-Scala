package utopia.citadel.database.access.single.organization

import utopia.citadel.database.Tables
import utopia.citadel.database.access.many.description.DbOrganizationDescriptions
import utopia.citadel.database.access.many.organization.{DbUserRoles, InvitationsAccess, OrganizationDeletionsAccess}
import utopia.citadel.database.access.many.user.DbUsers
import utopia.citadel.database.access.single.description.DbOrganizationDescription
import utopia.citadel.database.factory.organization.{MembershipFactory, MembershipWithRolesFactory}
import utopia.citadel.database.model.organization.{DeletionModel, MemberRoleLinkModel, MembershipModel, OrganizationModel}
import utopia.citadel.model.enumeration.CitadelDescriptionRole.Name
import utopia.citadel.model.enumeration.StandardUserRole.Owner
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.metropolis.model.combined.organization.DetailedMembership
import utopia.metropolis.model.partial.organization.{DeletionData, InvitationData, MembershipData}
import utopia.metropolis.model.stored.organization.Membership
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.sql.{Delete, Select, Where}

import scala.concurrent.duration.FiniteDuration

/**
  * Used for accessing individual organizations
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1.0
  */
object DbOrganization
{
	// OTHER	----------------------------
	
	private def table = Tables.organization
	
	private def factory = OrganizationModel
	
	private def model = OrganizationModel
	
	/**
	  * @param id An organization id
	  * @return An access point to that organization's data
	  */
	def apply(id: Int) = new DbSingleOrganization(id)
	
	/**
	 * Inserts a new organization to the database
	 * @param founderId  Id of the user who created the organization
	 * @param connection DB Connection (implicit)
	 * @return Id of the newly inserted organization
	 */
	def insert(organizationName: String, languageId: Int, founderId: Int)(implicit connection: Connection) =
	{
		// Inserts a new organization
		val organizationId = factory.insert(founderId)
		// Adds the user to the organization (as owner)
		val membership = MembershipModel.insert(MembershipData(organizationId, founderId, Some(founderId)))
		MemberRoleLinkModel.insert(membership.id, Owner.id, founderId)
		// Inserts a name for that organization
		DbOrganizationDescriptions(organizationId).update(Name.id, languageId, founderId, organizationName)
		// Returns organization id
		organizationId
	}
	
	
	// NESTED	----------------------------
	
	class DbSingleOrganization(val organizationId: Int)
	{
		// COMPUTED	------------------------------
		
		private def condition = model.withId(organizationId).toCondition
		
		/**
		  * @return An access point to this organization's memberships (organization-user-links)
		  */
		def memberships = DbOrganizationMemberships
		
		/**
		  * @return An access point to invitations to join this organization
		  */
		def invitations = DbOrganizationInvitations
		
		/**
		  * @return An access point to deletions targeting this organization
		  */
		def deletions = DbOrganizationDeletions
		
		/**
		  * @return An access point to individual descriptions of this organization
		  */
		def description = DbOrganizationDescription(organizationId)
		/**
		  * @return An access point to descriptions of this organization
		  */
		def descriptions = DbOrganizationDescriptions(organizationId)
		
		
		// OTHER    -------------------------------
		
		/**
		  * Deletes this organization <b>permanently</b>
		  * @param connection Implicit DB Connection
		  * @return Whether an organization was deleted
		  */
		def delete()(implicit connection: Connection) =
			connection(Delete(table) + Where(condition)).updatedRows
		
		
		// NESTED	-------------------------------
		
		object DbOrganizationMemberships extends ManyRowModelAccess[Membership]
		{
			// COMPUTED	---------------------------
			
			private def memberRolesFactory = MemberRoleLinkModel
			
			private def withRolesFactory = MembershipWithRolesFactory
			
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
						DetailedMembership(membership, roles, settings)
					}
				}
			}
			
			
			// IMPLEMENTED	-----------------------
			
			override def factory = MembershipFactory
			
			override def globalCondition = Some(condition)
			
			override protected def defaultOrdering = None
			
			
			// OTHER	---------------------------
			
			/**
			  * @param roleId     Searched user role's id
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
			  * @param userId         Id of the new organization member (user)
			  * @param startingRoleId Id of the role given to the user in this organization
			  * @param creatorId      Id of the user who authorized / added this membership
			  * @param connection     DB Connection (implicit)
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
		
		object DbOrganizationInvitations extends InvitationsAccess
		{
			// IMPLEMENTED	------------------------
			
			override def globalCondition = Some(model.withOrganizationId(organizationId).toCondition)
			
			override protected def defaultOrdering = None
			
			
			// OTHER	---------------------------
			
			/**
			  * Sends a new invitation. Please make sure the user is allowed to send this invitation before calling this
			  * method.
			  * @param recipient     Either recipient user id (right) or recipient user email (left)
			  * @param roleId        Id of the role the user will receive upon accepting this invitation
			  * @param senderId      Id of the user sending this invitation
			  * @param validDuration Duration how long the invitation can still be answered (default = 1 week)
			  * @param connection    DB Connection (implicit)
			  * @return Newly saved invitation
			  */
			def send(recipient: Either[String, Int], roleId: Int, senderId: Int,
			         validDuration: FiniteDuration = 7.days)(implicit connection: Connection) =
				model.insert(InvitationData(organizationId, recipient, roleId, Now + validDuration, Some(senderId)))
		}
		
		object DbOrganizationDeletions extends OrganizationDeletionsAccess
		{
			// IMPLEMENTED	----------------------
			
			override def globalCondition = Some(DeletionModel.withOrganizationId(organizationId).toCondition)
			
			override protected def defaultOrdering = None
			
			
			// OTHER	--------------------------
			
			/**
			  * Inserts a new deletion for this organization
			  * @param creatorId          Id of the user starting the deletion process
			  * @param actualizationDelay Delay before actualizing this deletion
			  * @param connection         DB Connection (implicit)
			  * @return Newly inserted deletion
			  */
			def insert(creatorId: Int, actualizationDelay: FiniteDuration)(implicit connection: Connection) =
				DeletionModel.insert(DeletionData(organizationId, creatorId, Now + actualizationDelay))
		}
	}
}
