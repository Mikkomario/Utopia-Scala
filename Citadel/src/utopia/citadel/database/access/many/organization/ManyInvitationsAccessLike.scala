package utopia.citadel.database.access.many.organization

import utopia.citadel.database.model.organization.InvitationModel
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.enumeration.BasicCombineOperator.Or
import utopia.vault.nosql.access.many.model.{ManyModelAccess, ManyRowModelAccess}
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

import java.time.Instant

/**
  * A common trait for access points which target multiple Invitations or invitation-like instances at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait ManyInvitationsAccessLike[+A, +Repr <: ManyModelAccess[A]] extends ManyRowModelAccess[A] with Indexed
	with FilterableView[Repr]
{
	// ABSTRACT --------------------
	
	protected def _filter(condition: Condition): Repr
	
	
	// COMPUTED	--------------------
	
	/**
	  * organizationIds of the accessible Invitations
	  */
	def organizationIds(implicit connection: Connection) = 
		pullColumn(model.organizationIdColumn).flatMap { value => value.int }
	/**
	  * startingRoleIds of the accessible Invitations
	  */
	def startingRoleIds(implicit connection: Connection) = 
		pullColumn(model.startingRoleIdColumn).flatMap { value => value.int }
	/**
	  * expirationTimes of the accessible Invitations
	  */
	def expirationTimes(implicit connection: Connection) = 
		pullColumn(model.expiresColumn).flatMap { value => value.instant }
	/**
	  * recipientIds of the accessible Invitations
	  */
	def recipientIds(implicit connection: Connection) = 
		pullColumn(model.recipientIdColumn).flatMap { value => value.int }
	/**
	  * recipientEmailAddresses of the accessible Invitations
	  */
	def recipientEmailAddresses(implicit connection: Connection) = 
		pullColumn(model.recipientEmailColumn).flatMap { value => value.string }
	/**
	  * messages of the accessible Invitations
	  */
	def messages(implicit connection: Connection) = 
		pullColumn(model.messageColumn).flatMap { value => value.string }
	/**
	  * senderIds of the accessible Invitations
	  */
	def senderIds(implicit connection: Connection) = 
		pullColumn(model.senderIdColumn).flatMap { value => value.int }
	/**
	  * creationTimes of the accessible Invitations
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = InvitationModel
	
	
	// IMPLEMENTED	--------------------
	
	override def filter(additionalCondition: Condition): Repr = _filter(additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * @param organizationId Id of the targeted organization
	  * @return An access point to invitations into that organization
	  */
	def toOrganizationWithId(organizationId: Int) = filter(model.withOrganizationId(organizationId).toCondition)
	/**
	  * @param recipientId Id of the targeted user
	  * @return An access point to invitations targeting that user specifically (NB: Email links are not included)
	  */
	def forRecipientWithId(recipientId: Int) = filter(model.withRecipientId(recipientId).toCondition)
	/**
	  * @param email Targeted email address
	  * @return An access point to invitations targeting that email address (NB: direct user links are not included)
	  */
	def forEmailAddress(email: String) = filter(model.withRecipientEmail(email).toCondition)
	/**
	  * @param recipientId Id of the targeted user
	  * @param email Email address of the targeted user
	  * @return An access point to invitations targeting that user
	  */
	def forRecipient(recipientId: Int, email: String) =
		filter(model.withRecipientId(recipientId).withRecipientEmail(email)
			.toConditionWithOperator(combineOperator = Or))
	
	/**
	  * Updates the created of the targeted Invitation instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any Invitation instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	/**
	  * Updates the expires of the targeted Invitation instance(s)
	  * @param newExpires A new expires to assign
	  * @return Whether any Invitation instance was affected
	  */
	def expirationTimes_=(newExpires: Instant)(implicit connection: Connection) = 
		putColumn(model.expiresColumn, newExpires)
	/**
	  * Updates the message of the targeted Invitation instance(s)
	  * @param newMessage A new message to assign
	  * @return Whether any Invitation instance was affected
	  */
	def messages_=(newMessage: String)(implicit connection: Connection) = 
		putColumn(model.messageColumn, newMessage)
	/**
	  * Updates the organizationId of the targeted Invitation instance(s)
	  * @param newOrganizationId A new organizationId to assign
	  * @return Whether any Invitation instance was affected
	  */
	def organizationIds_=(newOrganizationId: Int)(implicit connection: Connection) = 
		putColumn(model.organizationIdColumn, newOrganizationId)
	/**
	  * Updates the recipientEmail of the targeted Invitation instance(s)
	  * @param newRecipientEmail A new recipientEmail to assign
	  * @return Whether any Invitation instance was affected
	  */
	def recipientEmailAddresses_=(newRecipientEmail: String)(implicit connection: Connection) = 
		putColumn(model.recipientEmailColumn, newRecipientEmail)
	/**
	  * Updates the recipientId of the targeted Invitation instance(s)
	  * @param newRecipientId A new recipientId to assign
	  * @return Whether any Invitation instance was affected
	  */
	def recipientIds_=(newRecipientId: Int)(implicit connection: Connection) = 
		putColumn(model.recipientIdColumn, newRecipientId)
	/**
	  * Updates the senderId of the targeted Invitation instance(s)
	  * @param newSenderId A new senderId to assign
	  * @return Whether any Invitation instance was affected
	  */
	def senderIds_=(newSenderId: Int)(implicit connection: Connection) = 
		putColumn(model.senderIdColumn, newSenderId)
	/**
	  * Updates the startingRoleId of the targeted Invitation instance(s)
	  * @param newStartingRoleId A new startingRoleId to assign
	  * @return Whether any Invitation instance was affected
	  */
	def startingRoleIds_=(newStartingRoleId: Int)(implicit connection: Connection) = 
		putColumn(model.startingRoleIdColumn, newStartingRoleId)
}

