package utopia.citadel.database.access.single.organization

import java.time.Instant
import utopia.citadel.database.factory.organization.InvitationResponseFactory
import utopia.citadel.database.model.organization.InvitationResponseModel
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.organization.InvitationResponse
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct InvitationResponses.
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait UniqueInvitationResponseAccess 
	extends SingleRowModelAccess[InvitationResponse] 
		with DistinctModelAccess[InvitationResponse, Option[InvitationResponse], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the invitation this response is for. None if no instance (or value) was found.
	  */
	def invitationId(implicit connection: Connection) = pullColumn(model.invitationIdColumn).int
	
	/**
	  * Attached written response. None if no instance (or value) was found.
	  */
	def message(implicit connection: Connection) = pullColumn(model.messageColumn).string
	
	/**
	  * Id of the user who responded to the invitation, 
		if still known. None if no instance (or value) was found.
	  */
	def creatorId(implicit connection: Connection) = pullColumn(model.creatorIdColumn).int
	
	/**
	  * Time when this InvitationResponse was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	/**
	  * Whether the invitation was accepted (true) or rejected (false). None if no instance (or value) was found.
	  */
	def accepted(implicit connection: Connection) = pullColumn(model.acceptedColumn).boolean
	
	/**
	  * Whether future invitations were blocked. None if no instance (or value) was found.
	  */
	def blocked(implicit connection: Connection) = pullColumn(model.blockedColumn).boolean
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = InvitationResponseModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = InvitationResponseFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the accepted of the targeted InvitationResponse instance(s)
	  * @param newAccepted A new accepted to assign
	  * @return Whether any InvitationResponse instance was affected
	  */
	def accepted_=(newAccepted: Boolean)(implicit connection: Connection) = 
		putColumn(model.acceptedColumn, newAccepted)
	
	/**
	  * Updates the blocked of the targeted InvitationResponse instance(s)
	  * @param newBlocked A new blocked to assign
	  * @return Whether any InvitationResponse instance was affected
	  */
	def blocked_=(newBlocked: Boolean)(implicit connection: Connection) = 
		putColumn(model.blockedColumn, newBlocked)
	
	/**
	  * Updates the created of the targeted InvitationResponse instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any InvitationResponse instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the creatorId of the targeted InvitationResponse instance(s)
	  * @param newCreatorId A new creatorId to assign
	  * @return Whether any InvitationResponse instance was affected
	  */
	def creatorId_=(newCreatorId: Int)(implicit connection: Connection) = 
		putColumn(model.creatorIdColumn, newCreatorId)
	
	/**
	  * Updates the invitationId of the targeted InvitationResponse instance(s)
	  * @param newInvitationId A new invitationId to assign
	  * @return Whether any InvitationResponse instance was affected
	  */
	def invitationId_=(newInvitationId: Int)(implicit connection: Connection) = 
		putColumn(model.invitationIdColumn, newInvitationId)
	
	/**
	  * Updates the message of the targeted InvitationResponse instance(s)
	  * @param newMessage A new message to assign
	  * @return Whether any InvitationResponse instance was affected
	  */
	def message_=(newMessage: String)(implicit connection: Connection) = 
		putColumn(model.messageColumn, newMessage)
}

