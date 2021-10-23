package utopia.citadel.database.access.single.organization

import utopia.metropolis.model.stored.organization.OrganizationDeletionCancellation
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual OrganizationDeletionCancellations, based on their id
  * @since 2021-10-23
  */
case class DbSingleOrganizationDeletionCancellation(id: Int) 
	extends UniqueOrganizationDeletionCancellationAccess 
		with SingleIntIdModelAccess[OrganizationDeletionCancellation]

