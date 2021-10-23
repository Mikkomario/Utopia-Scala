package utopia.metropolis.model.stored.organization

import utopia.metropolis.model.partial.organization.OrganizationData
import utopia.metropolis.model.stored.{StoredFromModelFactory, StoredModelConvertible}

object Organization extends StoredFromModelFactory[Organization, OrganizationData]
{
	// IMPLEMENTED  -------------------------------
	
	override def dataFactory = OrganizationData
}

/**
  * Represents a Organization that has already been stored in the database
  * @param id id of this Organization in the database
  * @param data Wrapped Organization data
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class Organization(id: Int, data: OrganizationData) extends StoredModelConvertible[OrganizationData]

