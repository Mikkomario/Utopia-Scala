package utopia.exodus.database.model.organization

import java.time.Instant

import utopia.exodus.database.Tables
import utopia.exodus.database.factory.organization.InvitationFactory
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.CollectionExtensions._
import utopia.metropolis.model.enumeration.UserRole
import utopia.metropolis.model.partial.organization.InvitationData
import utopia.metropolis.model.stored.organization.Invitation
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory

object InvitationModel
{
	// COMPUTED	-------------------------------
	
	/**
	  * @return Table this model uses
	  */
	def table = Tables.organizationInvitation
	
	
	// OTHER	--------------------------------
	
	/**
	  * @param organizationId Id of targeted organization
	  * @return A model with only organization id set
	  */
	def withOrganizationId(organizationId: Int) = apply(organizationId = Some(organizationId))
	
	/**
	  * @param expireTime Time when invitation expires
	  * @return A model with only expire time set
	  */
	def withExpireTime(expireTime: Instant) = apply(expireTime = Some(expireTime))
	
	/**
	  * @param recipientId Id of recipient user
	  * @return A model with only recipient id set
	  */
	def withRecipientId(recipientId: Int) = apply(recipientId = Some(recipientId))
	
	/**
	  * Inserts a new invitation to the database
	  * @param data Invitation data to insert
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted invitation
	  */
	def insert(data: InvitationData)(implicit connection: Connection) =
	{
		val newId = apply(None, Some(data.organizationId), data.recipient.rightOption, data.recipient.leftOption,
			Some(data.startingRole), Some(data.expireTime), data.creatorId).insert().getInt
		Invitation(newId, data)
	}
}

/**
  * Used for interacting with organization invitations in DB
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  */
case class InvitationModel(id: Option[Int] = None, organizationId: Option[Int] = None, recipientId: Option[Int] = None,
						   recipientEmail: Option[String] = None, startingRole: Option[UserRole] = None,
						   expireTime: Option[Instant] = None, creatorId: Option[Int] = None)
	extends StorableWithFactory[Invitation]
{
	// IMPLEMENTED	------------------------------
	
	override def factory = InvitationFactory
	
	override def valueProperties = Vector("id" -> id, "organizationId" -> organizationId, "recipientId" -> recipientId,
		"recipientEmail" -> recipientEmail, "startingRoleId" -> startingRole.map { _.id }, "expiresIn" -> expireTime,
		"creatorId" -> creatorId)
	
	
	// OTHER	----------------------------------
	
	/**
	  * @param email Recipient user's email address
	  * @return A copy of this model with specified email address
	  */
	def withRecipientEmail(email: String) = copy(recipientEmail = Some(email))
}
