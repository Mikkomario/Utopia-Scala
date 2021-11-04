package utopia.citadel.database.access.single.organization

import utopia.citadel.database.factory.organization.InvitationResponseFactory
import utopia.citadel.database.model.organization.InvitationResponseModel
import utopia.metropolis.model.partial.organization.InvitationResponseData
import utopia.metropolis.model.post.NewInvitationResponse
import utopia.metropolis.model.stored.organization.InvitationResponse
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual InvitationResponses
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbInvitationResponse 
	extends SingleRowModelAccess[InvitationResponse] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = InvitationResponseModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = InvitationResponseFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted InvitationResponse instance
	  * @return An access point to that InvitationResponse
	  */
	def apply(id: Int) = DbSingleInvitationResponse(id)
	
	/**
	  * @param invitationId An invitation id
	  * @return An access point to that invitation's response
	  */
	def forInvitationWithId(invitationId: Int) = new DbResponseForInvitation(invitationId)
	
	
	// NESTED   --------------------
	
	class DbResponseForInvitation(invitationId: Int) extends UniqueInvitationResponseAccess
	{
		// IMPLEMENTED  ------------
		
		override protected def defaultOrdering = None
		
		override def globalCondition = Some(model.withInvitationId(invitationId).toCondition)
		
		
		// OTHER    ---------------
		
		/**
		  * Inserts a new response for this invitation
		  * @param creatorId   Id of the user who added this data
		  * @param message Message given with the response (optional)
		  * @param wasAccepted Whether the invitation was accepted (default = false)
		  * @param wasBlocked  Whether future invitations were blocked (default = false)
		  * @param connection  DB Connection (implicit)
		  * @return Newly inserted response
		  */
		def insert(creatorId: Int, message: Option[String] = None, wasAccepted: Boolean = false,
		           wasBlocked: Boolean = false)
		          (implicit connection: Connection) =
			model.insert(InvitationResponseData(invitationId, message, Some(creatorId),
				accepted = wasAccepted, blocked = wasBlocked))
		
		/**
		  * Inserts a new response for this invitation
		  * @param creatorId   Response creator's id
		  * @param newResponse New response
		  * @param connection  DB Connection (implicit)
		  * @return Newly inserted response
		  */
		def insert(creatorId: Int, newResponse: NewInvitationResponse)
		          (implicit connection: Connection): InvitationResponse =
			insert(creatorId, newResponse.message, newResponse.wasAccepted, newResponse.wasBlocked)
	}
}

