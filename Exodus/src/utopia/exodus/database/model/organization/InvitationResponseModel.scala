package utopia.exodus.database.model.organization

import utopia.exodus.database.factory.organization.InvitationResponseFactory
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.organization.InvitationResponseData
import utopia.metropolis.model.stored.organization.InvitationResponse
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory

@deprecated("Please use the Citadel version instead", "v2.0")
object InvitationResponseModel
{
	// ATTRIBUTES	--------------------------
	
	/**
	  * A model that has been marked as blocked
	  */
	val blocked = apply(wasBlocked = Some(true))
	
	
	// OTHER	------------------------------
	
	/**
	  * @param invitationId Id of the invitation this response is for
	  * @return A model with only invitation id set
	  */
	def withInvitationId(invitationId: Int) = apply(invitationId = Some(invitationId))
	
	/**
	  * Inserts a new invitation response to the DB
	  * @param data Data to insert
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted response
	  */
	def insert(data: InvitationResponseData)(implicit connection: Connection) =
	{
		val newId = apply(None, Some(data.invitationId), Some(data.wasAccepted), Some(data.wasBlocked),
			Some(data.creatorId)).insert().getInt
		InvitationResponse(newId, data)
	}
}

/**
  * Used for interacting with invitation responses in DB
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  */
@deprecated("Please use the Citadel version instead", "v2.0")
case class InvitationResponseModel(id: Option[Int] = None, invitationId: Option[Int] = None,
								   wasAccepted: Option[Boolean] = None, wasBlocked: Option[Boolean] = None,
								   creatorId: Option[Int] = None) extends StorableWithFactory[InvitationResponse]
{
	override def factory = InvitationResponseFactory
	
	override def valueProperties = Vector("id" -> id, "invitationId" -> invitationId, "wasAccepted" -> wasAccepted,
		"wasBlocked" -> wasBlocked, "creatorId" -> creatorId)
}
