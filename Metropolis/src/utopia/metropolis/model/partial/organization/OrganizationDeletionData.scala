package utopia.metropolis.model.partial.organization

import utopia.flow.collection.value.typeless.Model

import java.time.Instant
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now

/**
  * Represents a request to delete an organization. There exists a time period between the request and its completion, 
	during which other users may cancel the deletion.
  * @param organizationId Id of the organization whose deletion was requested
  * @param actualization Time when this deletion is/was scheduled to actualize
  * @param creatorId Id of the user who requested organization deletion
  * @param created Time when this deletion was requested
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class OrganizationDeletionData(organizationId: Int, actualization: Instant = Now, creatorId: Int, 
	created: Instant = Now) 
	extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("organization_id" -> organizationId, "actualization" -> actualization, 
			"creator_id" -> creatorId, "created" -> created))
}

