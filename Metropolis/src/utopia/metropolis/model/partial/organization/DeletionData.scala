package utopia.metropolis.model.partial.organization

import java.time.Instant
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.ModelConvertible

/**
  * Contains basic information about an organization deletion attempt
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1
  * @param organizationId Id of the targeted organization
  * @param creatorId Id of the user who attempted deletion
  * @param actualizationTime Time when this deletion actualizes if not cancelled
  */
@deprecated("Replaced with OrganizationDeletionData", "v2.0")
case class DeletionData(organizationId: Int, creatorId: Int, actualizationTime: Instant) extends ModelConvertible
{
	// IMPLEMENTED  ----------------------------
	
	override def toModel = Model(Vector("organization_id" -> organizationId, "creator_id" -> creatorId,
		"actualization" -> actualizationTime))
}
