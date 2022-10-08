package utopia.metropolis.model.partial.organization

import java.time.Instant
import utopia.flow.generic.model.immutable.ModelDeclaration
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Constant, Model}
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.time.Now
import utopia.metropolis.model.StyledModelConvertible

object InvitationResponseData extends FromModelFactoryWithSchema[InvitationResponseData]
{
	override val schema = ModelDeclaration("invitation_id" -> IntType)
	
	override protected def fromValidatedModel(model: Model) =
		InvitationResponseData(model("invitation_id"), model("message"), model("creator_id"), model("created"),
			model("accepted"), model("blocked"))
}

/**
  * Represents a response (yes|no) to an invitation to join an organization
  * @param invitationId Id of the invitation this response is for
  * @param message Attached written response
  * @param creatorId Id of the user who responded to the invitation, if still known
  * @param created Time when this InvitationResponse was first created
  * @param accepted Whether the invitation was accepted (true) or rejected (false)
  * @param blocked Whether future invitations were blocked
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class InvitationResponseData(invitationId: Int, message: Option[String] = None, 
	creatorId: Option[Int] = None, created: Instant = Now, accepted: Boolean = false, 
	blocked: Boolean = false) 
	extends StyledModelConvertible
{
	// COMPUTED ------------------------
	
	/**
	  * @return Whether the invitation was rejected
	  */
	def rejected = !accepted
	
	
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("invitation_id" -> invitationId, "message" -> message, "creator_id" -> creatorId, 
			"created" -> created, "accepted" -> accepted, "blocked" -> blocked))
	
	override def toSimpleModel =
	{
		val base = Model(Vector("accepted" -> accepted, "message" -> message))
		if (accepted) base else base + Constant("blocked", blocked)
	}
}

