package utopia.metropolis.model.combined.organization

import utopia.flow.datastructure.immutable.{Constant, ModelDeclaration, PropertyDeclaration}
import utopia.flow.datastructure.template.{Model, Property}
import utopia.flow.generic.{FromModelFactory, ModelConvertible, ModelType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.Extender
import utopia.metropolis.model.stored.organization.{Invitation, InvitationResponse}

object InvitationWithResponse extends FromModelFactory[InvitationWithResponse]
{
	// ATTRIBUTES	------------------------------
	
	private val schema = ModelDeclaration(PropertyDeclaration("response", ModelType))
	
	
	// IMPLEMENTED	------------------------------
	
	override def apply(model: Model[Property]) = Invitation(model).flatMap { invitation =>
		schema.validate(model).toTry.flatMap { valid =>
			InvitationResponse(valid("response").getModel).map { response =>
				InvitationWithResponse(invitation, response)
			}
		}
	}
}

/**
  * An extender to standard invitation model that also contains the response to that invitation (if present)
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  */
case class InvitationWithResponse(invitation: Invitation, response: InvitationResponse) extends Extender[Invitation]
	with ModelConvertible
{
	// COMPUTED	----------------------------------
	
	/**
	  * @return Id of the user who received this invitation
	  */
	def recipientId = response.creatorId
	
	
	// IMPLEMENTED	------------------------------
	
	override def wrapped = invitation
	
	override def toModel =
	{
		// Includes the response model
		val base = wrapped.toModel
		val responseModel = response.toModel
		base + Constant("response", responseModel)
	}
}
