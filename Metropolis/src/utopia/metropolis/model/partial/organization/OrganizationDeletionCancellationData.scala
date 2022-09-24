package utopia.metropolis.model.partial.organization

import java.time.Instant
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now

/**
  * Records a cancellation for a pending organization deletion request
  * @param deletionId Id of the cancelled deletion
  * @param creatorId Id of the user who cancelled the referenced organization deletion, if still known
  * @param created Time when this OrganizationDeletionCancellation was first created
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class OrganizationDeletionCancellationData(deletionId: Int, creatorId: Option[Int] = None, 
	created: Instant = Now) 
	extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("deletion_id" -> deletionId, "creator_id" -> creatorId, "created" -> created))
}

