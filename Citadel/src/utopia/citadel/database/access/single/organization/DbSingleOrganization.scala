package utopia.citadel.database.access.single.organization

import utopia.citadel.database.access.many.description.DbOrganizationDescriptions
import utopia.citadel.database.access.many.organization.DbInvitations.DbCurrentAndPastInvitations
import utopia.citadel.database.access.many.organization.{DbInvitations, DbMemberships, DbMembershipsWithRoles, DbOrganizationDeletions}
import utopia.citadel.database.access.single.description.{DbOrganizationDescription, SingleIdDescribedAccess}
import utopia.metropolis.model.combined.organization.DescribedOrganization
import utopia.metropolis.model.stored.organization.Organization
import utopia.vault.database.Connection

/**
  * An access point to individual Organizations, based on their id
  * @since 2021-10-23
  */
case class DbSingleOrganization(id: Int) 
	extends UniqueOrganizationAccess with SingleIdDescribedAccess[Organization, DescribedOrganization]
{
	// COMPUTED ------------------------
	
	/**
	  * @return An access point to this organization's memberships
	  */
	def memberships = DbMemberships.inOrganizationWithId(id)
	/**
	  * @return An access point to this organizations memberships, including their role links
	  */
	def membershipsWithRoles = DbMembershipsWithRoles.inOrganizationWithId(id)
	/**
	  * @return An access point to active (non-expired) invitations into this organization
	  */
	def currentInvitations = DbInvitations.toOrganizationWithId(id)
	/**
	  * @return An access point to both current and past invitations to this organization
	  */
	def currentAndPastInvitations = DbCurrentAndPastInvitations.toOrganizationWithId(id)
	/**
	  * @return An access point to deletions targeting this organization
	  */
	def deletions = DbOrganizationDeletions.withOrganizationId(id)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def describedFactory = DescribedOrganization
	
	override protected def manyDescriptionsAccess = DbOrganizationDescriptions
	
	override protected def singleDescriptionAccess = DbOrganizationDescription
	
	
	// OTHER    ------------------------
	
	/**
	  * Adds a new member to this organization
	  * @param userId Id of the user to add
	  * @param startingRoleId Id of the role to assign to the user
	  * @param creatorId Id of the user who added this user to this organization
	  * @param connection Implicit DB Connection
	  * @return Newly started membership
	  */
	def addMember(userId: Int, startingRoleId: Int, creatorId: Int)(implicit connection: Connection) =
		DbMembership.start(id, userId, startingRoleId, creatorId)
}

