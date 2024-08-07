package utopia.journey.model

import utopia.annex.model.Spirit
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Constant, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.template.{ModelConvertible, ModelLike, Property}
import utopia.metropolis.model.post.NewInvitationResponse

object InvitationResponseSpirit extends FromModelFactory[InvitationResponseSpirit]
{
	// ATTRIBUTES	-------------------------
	
	private val schema = ModelDeclaration(PropertyDeclaration("invitation_id", IntType))
	
	
	// IMPLEMENTED	-------------------------
	
	override def apply(model: ModelLike[Property]) = NewInvitationResponse(model).flatMap { response =>
		schema.validate(model).map { valid => InvitationResponseSpirit(valid("invitation_id").getInt, response) }
	}
	
	
	// OTHER	-----------------------------
	
	/**
	  * @param invitationId Id of the targeted invitation
	  * @param accept Whether the invitation should be accepted
	  * @param block Whether future invitations should be blocked (default = false)
	  * @return A new response spirit
	  */
	// TODO: Add a message to the invitation response
	def apply(invitationId: Int, accept: Boolean, block: Boolean = false): InvitationResponseSpirit =
		InvitationResponseSpirit(invitationId, NewInvitationResponse(None, accept, block))
	
	/**
	  * Creates a new positive response
	  * @param invitationId Id of the accepted invitation
	  * @return A new response spirit
	  */
	def accept(invitationId: Int) = InvitationResponseSpirit(invitationId, NewInvitationResponse(wasAccepted = true))
	
	/**
	  * Creates a new negative response
	  * @param invitationId Id of the rejected invitation
	  * @param shouldBlockFutureInvitations Whether future invitations from the inviting organization should be blocked
	  *                                     automatically (default = false)
	  * @return A new response spirit
	  */
	def reject(invitationId: Int, shouldBlockFutureInvitations: Boolean = false) =
		InvitationResponseSpirit(invitationId, NewInvitationResponse(wasBlocked = shouldBlockFutureInvitations))
	
	/**
	  * Checks whether the specified model is likely to represent a valid response spirit
	  * @param model a model being tested
	  * @return Whether the specified model is likely to be a valid response spirit model
	  */
	def isProbablyValidModel(model: ModelLike[Property]) = NewInvitationResponse.schema.isProbablyValid(model) &&
		schema.isProbablyValid(model)
}

/**
  * Represents an invitation response before it has been posted to server
  * @author Mikko Hilpinen
  * @since 12.7.2020, v0.1
  */
case class InvitationResponseSpirit private(invitationId: Int, private val response: NewInvitationResponse)
	extends Spirit with ModelConvertible
{
	// COMPUTED	----------------------------
	
	/**
	  * @return Whether the invitation was accepted
	  */
	def isAccepted = response.wasAccepted
	
	/**
	  * @return Whether future invitations were blocked
	  */
	def isBlocked = response.wasBlocked
	
	
	// IMPLEMENTED	------------------------
	
	override def identifier = invitationId * (if (isAccepted) 1 else -1)
	
	override def postPath = s"users/me/invitations/$invitationId/response"
	
	override def postBody = response.toModel
	
	override def toModel = response.toModel + Constant("invitation_id", invitationId)
}
