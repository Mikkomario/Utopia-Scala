package utopia.metropolis.model.stored.organization

import utopia.flow.datastructure.immutable.Constant
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.organization.InvitationResponseData
import utopia.metropolis.model.stored.Stored

/**
  * Represents an invitation response that has been stored to DB
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  */
case class InvitationResponse(id: Int, data: InvitationResponseData) extends Stored[InvitationResponseData]
	with ModelConvertible
{
	override def toModel =
	{
		// Includes invitation id
		val base = data.toModel
		base + Constant("id", id)
	}
}
