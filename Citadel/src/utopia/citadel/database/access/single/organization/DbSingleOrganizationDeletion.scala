package utopia.citadel.database.access.single.organization

import utopia.metropolis.model.stored.organization.OrganizationDeletion
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual OrganizationDeletions, based on their id
  * @since 2021-10-23
  */
case class DbSingleOrganizationDeletion(id: Int) 
	extends UniqueOrganizationDeletionAccess with SingleIntIdModelAccess[OrganizationDeletion]

