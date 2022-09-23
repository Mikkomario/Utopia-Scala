package utopia.citadel.database.access.single.organization

import java.time.Instant
import utopia.citadel.database.factory.organization.InvitationFactory
import utopia.citadel.database.model.organization.InvitationModel
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.organization.Invitation
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct Invitations.
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait UniqueInvitationAccess 
	extends SingleRowModelAccess[Invitation] with DistinctModelAccess[Invitation, Option[Invitation], Value] 
		with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the organization which the recipient is invited to join. None if no instance (or value) was found.
	  */
	def organizationId(implicit connection: Connection) = pullColumn(model.organizationIdColumn).int
	
	/**
	  * The role the recipient will have in the organization initially if they join. None if no instance (or value) was found.
	  */
	def startingRoleId(implicit connection: Connection) = pullColumn(model.startingRoleIdColumn).int
	
	/**
	  * Time when this Invitation expires / becomes invalid. None if no instance (or value) was found.
	  */
	def expires(implicit connection: Connection) = pullColumn(model.expiresColumn).instant
	
	/**
	  * Id of the invited user, if known. None if no instance (or value) was found.
	  */
	def recipientId(implicit connection: Connection) = pullColumn(model.recipientIdColumn).int
	
	/**
	  * Email address of the invited user / the email address where this invitation is sent to. None if no instance (or value) was found.
	  */
	def recipientEmail(implicit connection: Connection) = pullColumn(model.recipientEmailColumn).string
	
	/**
	  * Message written by the sender to accompany this invitation. None if no instance (or value) was found.
	  */
	def message(implicit connection: Connection) = pullColumn(model.messageColumn).string
	
	/**
	  * Id of the user who sent this invitation, if still known. None if no instance (or value) was found.
	  */
	def senderId(implicit connection: Connection) = pullColumn(model.senderIdColumn).int
	
	/**
	  * Time when this invitation was created / sent. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = InvitationModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = InvitationFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted Invitation instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any Invitation instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the expires of the targeted Invitation instance(s)
	  * @param newExpires A new expires to assign
	  * @return Whether any Invitation instance was affected
	  */
	def expires_=(newExpires: Instant)(implicit connection: Connection) = 
		putColumn(model.expiresColumn, newExpires)
	
	/**
	  * Updates the message of the targeted Invitation instance(s)
	  * @param newMessage A new message to assign
	  * @return Whether any Invitation instance was affected
	  */
	def message_=(newMessage: String)(implicit connection: Connection) = 
		putColumn(model.messageColumn, newMessage)
	
	/**
	  * Updates the organizationId of the targeted Invitation instance(s)
	  * @param newOrganizationId A new organizationId to assign
	  * @return Whether any Invitation instance was affected
	  */
	def organizationId_=(newOrganizationId: Int)(implicit connection: Connection) = 
		putColumn(model.organizationIdColumn, newOrganizationId)
	
	/**
	  * Updates the recipientEmail of the targeted Invitation instance(s)
	  * @param newRecipientEmail A new recipientEmail to assign
	  * @return Whether any Invitation instance was affected
	  */
	def recipientEmail_=(newRecipientEmail: String)(implicit connection: Connection) = 
		putColumn(model.recipientEmailColumn, newRecipientEmail)
	
	/**
	  * Updates the recipientId of the targeted Invitation instance(s)
	  * @param newRecipientId A new recipientId to assign
	  * @return Whether any Invitation instance was affected
	  */
	def recipientId_=(newRecipientId: Int)(implicit connection: Connection) = 
		putColumn(model.recipientIdColumn, newRecipientId)
	
	/**
	  * Updates the senderId of the targeted Invitation instance(s)
	  * @param newSenderId A new senderId to assign
	  * @return Whether any Invitation instance was affected
	  */
	def senderId_=(newSenderId: Int)(implicit connection: Connection) = 
		putColumn(model.senderIdColumn, newSenderId)
	
	/**
	  * Updates the startingRoleId of the targeted Invitation instance(s)
	  * @param newStartingRoleId A new startingRoleId to assign
	  * @return Whether any Invitation instance was affected
	  */
	def startingRoleId_=(newStartingRoleId: Int)(implicit connection: Connection) = 
		putColumn(model.startingRoleIdColumn, newStartingRoleId)
}

