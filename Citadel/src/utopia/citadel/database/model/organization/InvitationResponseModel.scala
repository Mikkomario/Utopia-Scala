package utopia.citadel.database.model.organization

import java.time.Instant
import utopia.citadel.database.factory.organization.InvitationResponseFactory
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.organization.InvitationResponseData
import utopia.metropolis.model.stored.organization.InvitationResponse
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing InvitationResponseModel instances and for inserting InvitationResponses to the database
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object InvitationResponseModel 
	extends DataInserter[InvitationResponseModel, InvitationResponse, InvitationResponseData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains InvitationResponse invitationId
	  */
	val invitationIdAttName = "invitationId"
	/**
	  * Name of the property that contains InvitationResponse message
	  */
	val messageAttName = "message"
	/**
	  * Name of the property that contains InvitationResponse creatorId
	  */
	val creatorIdAttName = "creatorId"
	/**
	  * Name of the property that contains InvitationResponse created
	  */
	val createdAttName = "created"
	/**
	  * Name of the property that contains InvitationResponse accepted
	  */
	val acceptedAttName = "accepted"
	/**
	  * Name of the property that contains InvitationResponse blocked
	  */
	val blockedAttName = "blocked"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains InvitationResponse invitationId
	  */
	def invitationIdColumn = table(invitationIdAttName)
	/**
	  * Column that contains InvitationResponse message
	  */
	def messageColumn = table(messageAttName)
	/**
	  * Column that contains InvitationResponse creatorId
	  */
	def creatorIdColumn = table(creatorIdAttName)
	/**
	  * Column that contains InvitationResponse created
	  */
	def createdColumn = table(createdAttName)
	/**
	  * Column that contains InvitationResponse accepted
	  */
	def acceptedColumn = table(acceptedAttName)
	/**
	  * Column that contains InvitationResponse blocked
	  */
	def blockedColumn = table(blockedAttName)
	/**
	  * The factory object used by this model type
	  */
	def factory = InvitationResponseFactory
	
	def accepted = withAccepted(accepted = true)
	def rejected = withAccepted(accepted = false)
	def blocked = withBlocked(blocked = true)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: InvitationResponseData) = 
		apply(None, Some(data.invitationId), data.message, data.creatorId, Some(data.created), 
			Some(data.accepted), Some(data.blocked))
	
	override def complete(id: Value, data: InvitationResponseData) = InvitationResponse(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param accepted Whether the invitation was accepted (true) or rejected (false)
	  * @return A model containing only the specified accepted
	  */
	def withAccepted(accepted: Boolean) = apply(accepted = Some(accepted))
	/**
	  * @param blocked Whether future invitations were blocked
	  * @return A model containing only the specified blocked
	  */
	def withBlocked(blocked: Boolean) = apply(blocked = Some(blocked))
	/**
	  * @param created Time when this InvitationResponse was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	/**
	  * @param creatorId Id of the user who responded to the invitation, if still known
	  * @return A model containing only the specified creatorId
	  */
	def withCreatorId(creatorId: Int) = apply(creatorId = Some(creatorId))
	/**
	  * @param id A InvitationResponse id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	/**
	  * @param invitationId Id of the invitation this response is for
	  * @return A model containing only the specified invitationId
	  */
	def withInvitationId(invitationId: Int) = apply(invitationId = Some(invitationId))
	/**
	  * @param message Attached written response
	  * @return A model containing only the specified message
	  */
	def withMessage(message: String) = apply(message = Some(message))
}

/**
  * Used for interacting with InvitationResponses in the database
  * @param id InvitationResponse database id
  * @param invitationId Id of the invitation this response is for
  * @param message Attached written response
  * @param creatorId Id of the user who responded to the invitation, if still known
  * @param created Time when this InvitationResponse was first created
  * @param accepted Whether the invitation was accepted (true) or rejected (false)
  * @param blocked Whether future invitations were blocked
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class InvitationResponseModel(id: Option[Int] = None, invitationId: Option[Int] = None, 
	message: Option[String] = None, creatorId: Option[Int] = None, created: Option[Instant] = None, 
	accepted: Option[Boolean] = None, blocked: Option[Boolean] = None) 
	extends StorableWithFactory[InvitationResponse]
{
	// IMPLEMENTED	--------------------
	
	override def factory = InvitationResponseModel.factory
	
	override def valueProperties = 
	{
		import InvitationResponseModel._
		Vector("id" -> id, invitationIdAttName -> invitationId, messageAttName -> message, 
			creatorIdAttName -> creatorId, createdAttName -> created, acceptedAttName -> this.accepted,
			blockedAttName -> this.blocked)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param accepted A new accepted
	  * @return A new copy of this model with the specified accepted
	  */
	def withAccepted(accepted: Boolean) = copy(accepted = Some(accepted))
	
	/**
	  * @param blocked A new blocked
	  * @return A new copy of this model with the specified blocked
	  */
	def withBlocked(blocked: Boolean) = copy(blocked = Some(blocked))
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param creatorId A new creatorId
	  * @return A new copy of this model with the specified creatorId
	  */
	def withCreatorId(creatorId: Int) = copy(creatorId = Some(creatorId))
	
	/**
	  * @param invitationId A new invitationId
	  * @return A new copy of this model with the specified invitationId
	  */
	def withInvitationId(invitationId: Int) = copy(invitationId = Some(invitationId))
	
	/**
	  * @param message A new message
	  * @return A new copy of this model with the specified message
	  */
	def withMessage(message: String) = copy(message = Some(message))
}

