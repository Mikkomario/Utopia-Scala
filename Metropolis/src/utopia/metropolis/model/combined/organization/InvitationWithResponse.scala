package utopia.metropolis.model.combined.organization

import utopia.flow.datastructure.immutable.Constant
import utopia.flow.datastructure.template.{Model, Property}
import utopia.flow.generic.FromModelFactory
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.Extender
import utopia.metropolis.model.StyledModelConvertible
import utopia.metropolis.model.partial.organization.InvitationData
import utopia.metropolis.model.stored.organization.{Invitation, InvitationResponse}

import scala.util.Success

object InvitationWithResponse extends FromModelFactory[InvitationWithResponse]
{
	override def apply(model: Model[Property]) =
		Invitation(model).flatMap { invitation =>
			model("response").model match
			{
				case Some(responseModel) => InvitationResponse(responseModel).map { invitation + _ }
				case None => Success(invitation + None)
			}
		}
}

/**
  * Combines Invitation with response data
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class InvitationWithResponse(invitation: Invitation, response: Option[InvitationResponse]) 
	extends Extender[InvitationData] with StyledModelConvertible
{
	// COMPUTED	--------------------
	
	/**
	  * Id of this Invitation in the database
	  */
	def id = invitation.id
	
	
	// IMPLEMENTED	--------------------
	
	override def wrapped = invitation.data
	
	override def toModel = invitation.toModel + Constant("response", response.map { _.toModel })
	
	override def toSimpleModel =
	{
		val base = invitation.toModel
		response match {
			case Some(response) => base + Constant("response", response.toSimpleModel)
			case None => base
		}
	}
}

