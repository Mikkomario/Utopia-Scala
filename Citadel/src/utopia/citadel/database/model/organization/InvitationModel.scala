package utopia.citadel.database.model.organization

import java.time.Instant
import utopia.citadel.database.factory.organization.InvitationFactory
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.metropolis.model.partial.organization.InvitationData
import utopia.metropolis.model.stored.organization.Invitation
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter
import utopia.vault.nosql.storable.deprecation.Expiring

/**
  * Used for constructing InvitationModel instances and for inserting Invitations to the database
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object InvitationModel extends DataInserter[InvitationModel, Invitation, InvitationData] with Expiring
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains Invitation organizationId
	  */
	val organizationIdAttName = "organizationId"
	/**
	  * Name of the property that contains Invitation startingRoleId
	  */
	val startingRoleIdAttName = "startingRoleId"
	/**
	  * Name of the property that contains Invitation expires
	  */
	val expiresAttName = "expires"
	/**
	  * Name of the property that contains Invitation recipientId
	  */
	val recipientIdAttName = "recipientId"
	/**
	  * Name of the property that contains Invitation recipientEmail
	  */
	val recipientEmailAttName = "recipientEmail"
	/**
	  * Name of the property that contains Invitation message
	  */
	val messageAttName = "message"
	/**
	  * Name of the property that contains Invitation senderId
	  */
	val senderIdAttName = "senderId"
	/**
	  * Name of the property that contains Invitation created
	  */
	val createdAttName = "created"
	
	override val deprecationAttName = "expires"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains Invitation organizationId
	  */
	def organizationIdColumn = table(organizationIdAttName)
	/**
	  * Column that contains Invitation startingRoleId
	  */
	def startingRoleIdColumn = table(startingRoleIdAttName)
	/**
	  * Column that contains Invitation expires
	  */
	def expiresColumn = table(expiresAttName)
	/**
	  * Column that contains Invitation recipientId
	  */
	def recipientIdColumn = table(recipientIdAttName)
	/**
	  * Column that contains Invitation recipientEmail
	  */
	def recipientEmailColumn = table(recipientEmailAttName)
	/**
	  * Column that contains Invitation message
	  */
	def messageColumn = table(messageAttName)
	/**
	  * Column that contains Invitation senderId
	  */
	def senderIdColumn = table(senderIdAttName)
	/**
	  * Column that contains Invitation created
	  */
	def createdColumn = table(createdAttName)
	/**
	  * The factory object used by this model type
	  */
	def factory = InvitationFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: InvitationData) = 
		apply(None, Some(data.organizationId), Some(data.startingRoleId), Some(data.expires), 
			data.recipientId, data.recipientEmail, data.message, data.senderId, Some(data.created))
	
	override def complete(id: Value, data: InvitationData) = Invitation(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this invitation was created / sent
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	/**
	  * @param expires Time when this Invitation expires / becomes invalid
	  * @return A model containing only the specified expires
	  */
	def withExpires(expires: Instant) = apply(expires = Some(expires))
	/**
	  * @param id A Invitation id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	/**
	  * @param message Message written by the sender to accompany this invitation
	  * @return A model containing only the specified message
	  */
	def withMessage(message: String) = apply(message = Some(message))
	/**
	  * @param organizationId Id of the organization which the recipient is invited to join
	  * @return A model containing only the specified organizationId
	  */
	def withOrganizationId(organizationId: Int) = apply(organizationId = Some(organizationId))
	/**
	  * @param recipientEmail Email address of the invited user / the email address where this invitation is sent to
	  * @return A model containing only the specified recipientEmail
	  */
	def withRecipientEmail(recipientEmail: String) = apply(recipientEmail = Some(recipientEmail))
	/**
	  * @param recipientId Id of the invited user, if known
	  * @return A model containing only the specified recipientId
	  */
	def withRecipientId(recipientId: Int) = apply(recipientId = Some(recipientId))
	/**
	  * @param senderId Id of the user who sent this invitation, if still known
	  * @return A model containing only the specified senderId
	  */
	def withSenderId(senderId: Int) = apply(senderId = Some(senderId))
	/**
	  * @param startingRoleId The role the recipient will have in the organization initially if they join
	  * @return A model containing only the specified startingRoleId
	  */
	def withStartingRoleId(startingRoleId: Int) = apply(startingRoleId = Some(startingRoleId))
}

/**
  * Used for interacting with Invitations in the database
  * @param id Invitation database id
  * @param organizationId Id of the organization which the recipient is invited to join
  * @param startingRoleId The role the recipient will have in the organization initially if they join
  * @param expires Time when this Invitation expires / becomes invalid
  * @param recipientId Id of the invited user, if known
  * @param recipientEmail Email address of the invited user / the email address where this invitation is sent to
  * @param message Message written by the sender to accompany this invitation
  * @param senderId Id of the user who sent this invitation, if still known
  * @param created Time when this invitation was created / sent
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class InvitationModel(id: Option[Int] = None, organizationId: Option[Int] = None, 
	startingRoleId: Option[Int] = None, expires: Option[Instant] = None, recipientId: Option[Int] = None, 
	recipientEmail: Option[String] = None, message: Option[String] = None, senderId: Option[Int] = None, 
	created: Option[Instant] = None) 
	extends StorableWithFactory[Invitation]
{
	// IMPLEMENTED	--------------------
	
	override def factory = InvitationModel.factory
	
	override def valueProperties = 
	{
		import InvitationModel._
		Vector("id" -> id, organizationIdAttName -> organizationId, startingRoleIdAttName -> startingRoleId, 
			expiresAttName -> expires, recipientIdAttName -> recipientId, 
			recipientEmailAttName -> recipientEmail, messageAttName -> message, senderIdAttName -> senderId, 
			createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param expires A new expires
	  * @return A new copy of this model with the specified expires
	  */
	def withExpires(expires: Instant) = copy(expires = Some(expires))
	
	/**
	  * @param message A new message
	  * @return A new copy of this model with the specified message
	  */
	def withMessage(message: String) = copy(message = Some(message))
	
	/**
	  * @param organizationId A new organizationId
	  * @return A new copy of this model with the specified organizationId
	  */
	def withOrganizationId(organizationId: Int) = copy(organizationId = Some(organizationId))
	
	/**
	  * @param recipientEmail A new recipientEmail
	  * @return A new copy of this model with the specified recipientEmail
	  */
	def withRecipientEmail(recipientEmail: String) = copy(recipientEmail = Some(recipientEmail))
	
	/**
	  * @param recipientId A new recipientId
	  * @return A new copy of this model with the specified recipientId
	  */
	def withRecipientId(recipientId: Int) = copy(recipientId = Some(recipientId))
	
	/**
	  * @param senderId A new senderId
	  * @return A new copy of this model with the specified senderId
	  */
	def withSenderId(senderId: Int) = copy(senderId = Some(senderId))
	
	/**
	  * @param startingRoleId A new startingRoleId
	  * @return A new copy of this model with the specified startingRoleId
	  */
	def withStartingRoleId(startingRoleId: Int) = copy(startingRoleId = Some(startingRoleId))
}

