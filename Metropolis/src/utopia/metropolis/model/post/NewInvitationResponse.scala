package utopia.metropolis.model.post

import utopia.flow.datastructure.immutable.{Constant, Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.{BooleanType, FromModelFactoryWithSchema, ModelConvertible}
import utopia.flow.generic.ValueConversions._

object NewInvitationResponse extends FromModelFactoryWithSchema[NewInvitationResponse]
{
	override val schema = ModelDeclaration(PropertyDeclaration("accepted", BooleanType))
	
	override protected def fromValidatedModel(model: Model[Constant]) = NewInvitationResponse(
		model("accepted").getBoolean, model("blocked").getBoolean)
}

/**
  * Used for posting invitation responses to the server
  * @author Mikko Hilpinen
  * @since 6.5.2020, v1
  * @param wasAccepted Whether the invitation was accepted
  * @param wasBlocked Whether future invitations were blocked (default = false)
  */
case class NewInvitationResponse(wasAccepted: Boolean, wasBlocked: Boolean = false) extends ModelConvertible
{
	override def toModel =
	{
		val base = Model(Vector("accepted" -> wasAccepted))
		if (!wasAccepted)
			base + Constant("blocked", wasBlocked)
		else
			base
	}
}
