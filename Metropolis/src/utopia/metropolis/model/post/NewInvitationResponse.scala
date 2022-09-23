package utopia.metropolis.model.post

import utopia.flow.collection.value.typeless.{Model, PropertyDeclaration}
import utopia.flow.generic.{BooleanType, FromModelFactoryWithSchema, ModelConvertible}
import utopia.flow.generic.ValueConversions._

object NewInvitationResponse extends FromModelFactoryWithSchema[NewInvitationResponse]
{
	override val schema = ModelDeclaration(PropertyDeclaration("accepted", BooleanType))
	
	override protected def fromValidatedModel(model: Model) =
		NewInvitationResponse(model("message").string.filter { _.nonEmpty }, model("accepted").getBoolean,
			model("blocked").getBoolean)
}

/**
  * Used for posting invitation responses to the server
  * @author Mikko Hilpinen
  * @since 6.5.2020, v1
  * @param wasAccepted Whether the invitation was accepted
  * @param wasBlocked Whether future invitations were blocked (default = false)
  */
case class NewInvitationResponse(message: Option[String] = None, wasAccepted: Boolean = false,
                                 wasBlocked: Boolean = false)
	extends ModelConvertible
{
	override def toModel =
		Model(Vector("message" -> message, "accepted" -> wasAccepted, "blocked" -> wasBlocked))
}
