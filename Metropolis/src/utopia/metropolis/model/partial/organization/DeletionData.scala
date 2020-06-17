package utopia.metropolis.model.partial.organization

import java.time.Instant

import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._

/**
  * Contains basic information about an organization deletion attempt
  * @author Mikko Hilpinen
  * @since 16.5.2020, v2
  * @param organizationId Id of the targeted organization
  * @param creatorId Id of the user who attempted deletion
  * @param actualizationTime Time when this deletion actualizes if not cancelled
  */
case class DeletionData(organizationId: Int, creatorId: Int, actualizationTime: Instant) extends ModelConvertible
{
	override def toModel = Model(Vector("organization_id" -> organizationId, "creator_id" -> creatorId,
		"actualization" -> actualizationTime))
}
