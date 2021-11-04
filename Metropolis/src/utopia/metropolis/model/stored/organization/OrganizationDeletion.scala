package utopia.metropolis.model.stored.organization

import utopia.metropolis.model.partial.organization.OrganizationDeletionData
import utopia.metropolis.model.stored.StoredModelConvertible

/**
  * Represents a OrganizationDeletion that has already been stored in the database
  * @param id id of this OrganizationDeletion in the database
  * @param data Wrapped OrganizationDeletion data
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class OrganizationDeletion(id: Int, data: OrganizationDeletionData) 
	extends StoredModelConvertible[OrganizationDeletionData]

