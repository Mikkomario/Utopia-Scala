package utopia.metropolis.model.stored.organization

import utopia.metropolis.model.partial.organization.OrganizationDeletionCancellationData
import utopia.metropolis.model.stored.StoredModelConvertible

/**
  * Represents a OrganizationDeletionCancellation that has already been stored in the database
  * @param id id of this OrganizationDeletionCancellation in the database
  * @param data Wrapped OrganizationDeletionCancellation data
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class OrganizationDeletionCancellation(id: Int, data: OrganizationDeletionCancellationData) 
	extends StoredModelConvertible[OrganizationDeletionCancellationData]

