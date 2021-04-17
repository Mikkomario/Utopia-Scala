package utopia.exodus.database.access.single

import utopia.exodus.database.factory.organization.{InvitationFactory, InvitationResponseFactory, InvitationWithResponseFactory}
import utopia.exodus.database.model.organization.InvitationResponseModel
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.combined.organization.InvitationWithResponse
import utopia.metropolis.model.partial.organization.InvitationResponseData
import utopia.metropolis.model.post.NewInvitationResponse
import utopia.metropolis.model.stored.organization.{Invitation, InvitationResponse}
import utopia.vault.database.Connection
import utopia.vault.nosql.access.{SingleIdModelAccess, SingleModelAccess, UniqueModelAccess}

/**
  * Used for accessing individual invitations
  * @author Mikko Hilpinen
  * @since 6.5.2020, v1
  */
object DbInvitation extends SingleModelAccess[Invitation]
{
	// IMPLEMENTED	---------------------------
	
	override def factory = InvitationFactory
	
	override def globalCondition = None
	
	
	// OTHER	-------------------------------
	
	/**
	  * @param id Invitation id
	  * @return An access point to that invitation's data
	  */
	def apply(id: Int) = new SingleInvitation(id)
	
	
	// NESTED	-------------------------------
	
	class SingleInvitation(invitationId: Int)
		extends SingleIdModelAccess[Invitation](invitationId, DbInvitation.factory)
	{
		// COMPUTED	---------------------------
		
		/**
		  * @return An access point to this invitation's response
		  */
		def response = ResponseAccess
		
		/**
		  * @return An access point to this invitation's data where response is also included. Please note that this
		  *         access point will only find invitations that have responses.
		  */
		def withResponse = InvitationWithResponseAccess
		
		
		// NESTED	---------------------------
		
		object ResponseAccess extends UniqueModelAccess[InvitationResponse]
		{
			// IMPLEMENTED	-------------------
			
			override val condition = model.withInvitationId(invitationId).toCondition
			
			override def factory = InvitationResponseFactory
			
			
			// COMPUTED	------------------------
			
			private def model = InvitationResponseModel
			
			
			// OTHER	------------------------
			
			/**
			  * Inserts a new response for this invitation
			  * @param wasAccepted Whether the invitation was accepted
			  * @param wasBlocked Whether future invitations were blocked
			  * @param creatorId Invitation creator id
			  * @param connection DB Connection (implicit)
			  * @return Newly inserted response
			  */
			def insert(wasAccepted: Boolean, wasBlocked: Boolean, creatorId: Int)(implicit connection: Connection) =
			{
				model.insert(InvitationResponseData(invitationId, wasAccepted, wasBlocked, creatorId))
			}
			
			/**
			  * Inserts a new response for this invitation
			  * @param newResponse New response
			  * @param creatorId Response creator's id
			  * @param connection DB Connection (implicit)
			  * @return Newly inserted response
			  */
			def insert(newResponse: NewInvitationResponse, creatorId: Int)
					  (implicit connection: Connection): InvitationResponse =
				insert(newResponse.wasAccepted, newResponse.wasBlocked, creatorId)
		}
		
		object InvitationWithResponseAccess extends SingleIdModelAccess[InvitationWithResponse](invitationId,
			InvitationWithResponseFactory)
	}
}
