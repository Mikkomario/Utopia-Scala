package utopia.citadel.database.access.single.organization

import utopia.citadel.database.access.many.description.DbOrganizationDescriptions
import utopia.citadel.database.access.many.organization.{DbInvitations, DbMemberships, DbOrganizationDeletions}
import utopia.citadel.database.access.single.description.{DbOrganizationDescription, SingleIdDescribedAccess}
import utopia.metropolis.model.combined.organization.DescribedOrganization
import utopia.metropolis.model.stored.organization.Organization

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
	  * @return An access point to invitations into this organization
	  */
	def invitations = DbInvitations.toOrganizationWithId(id)
	/**
	  * @return An access point to deletions targeting this organization
	  */
	def deletions = DbOrganizationDeletions.withOrganizationId(id)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def describedFactory = DescribedOrganization
	
	override protected def manyDescriptionsAccess = DbOrganizationDescriptions
	
	override protected def singleDescriptionAccess = DbOrganizationDescription
}

