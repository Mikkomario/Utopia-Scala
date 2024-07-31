package utopia.citadel.database.access.many.organization

import utopia.citadel.database.factory.organization.InvitationResponseFactory
import utopia.citadel.database.model.organization.InvitationResponseModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.stored.organization.InvitationResponse
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, ViewFactory}
import utopia.vault.sql.Condition

import java.time.Instant

object ManyInvitationResponsesAccess extends ViewFactory[ManyInvitationResponsesAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyInvitationResponsesAccess = 
		new _ManyInvitationResponsesAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyInvitationResponsesAccess(condition: Condition) extends ManyInvitationResponsesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple InvitationResponses at a time
  * @author Mikko Hilpinen
  * @since 23.10.2021
  */
trait ManyInvitationResponsesAccess 
	extends ManyRowModelAccess[InvitationResponse] with Indexed 
		with FilterableView[ManyInvitationResponsesAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * invitationIds of the accessible InvitationResponses
	  */
	def invitationIds(implicit connection: Connection) = 
		pullColumn(model.invitationIdColumn).flatMap { value => value.int }
	
	/**
	  * messages of the accessible InvitationResponses
	  */
	def messages(implicit connection: Connection) = 
		pullColumn(model.messageColumn).flatMap { value => value.string }
	
	/**
	  * creatorIds of the accessible InvitationResponses
	  */
	def creatorIds(implicit connection: Connection) = 
		pullColumn(model.creatorIdColumn).flatMap { value => value.int }
	
	/**
	  * creationTimes of the accessible InvitationResponses
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	/**
	  * wereAccepted of the accessible InvitationResponses
	  */
	def wereAccepted(implicit connection: Connection) = 
		pullColumn(model.acceptedColumn).flatMap { value => value.boolean }
	
	/**
	  * wereBlocked of the accessible InvitationResponses
	  */
	def wereBlocked(implicit connection: Connection) = 
		pullColumn(model.blockedColumn).flatMap { value => value.boolean }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = InvitationResponseModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = InvitationResponseFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyInvitationResponsesAccess = 
		ManyInvitationResponsesAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted InvitationResponse instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any InvitationResponse instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the creatorId of the targeted InvitationResponse instance(s)
	  * @param newCreatorId A new creatorId to assign
	  * @return Whether any InvitationResponse instance was affected
	  */
	def creatorIds_=(newCreatorId: Int)(implicit connection: Connection) = 
		putColumn(model.creatorIdColumn, newCreatorId)
	
	/**
	  * Updates the invitationId of the targeted InvitationResponse instance(s)
	  * @param newInvitationId A new invitationId to assign
	  * @return Whether any InvitationResponse instance was affected
	  */
	def invitationIds_=(newInvitationId: Int)(implicit connection: Connection) = 
		putColumn(model.invitationIdColumn, newInvitationId)
	
	/**
	  * Updates the message of the targeted InvitationResponse instance(s)
	  * @param newMessage A new message to assign
	  * @return Whether any InvitationResponse instance was affected
	  */
	def messages_=(newMessage: String)(implicit connection: Connection) = 
		putColumn(model.messageColumn, newMessage)
	
	/**
	  * Updates the accepted of the targeted InvitationResponse instance(s)
	  * @param newAccepted A new accepted to assign
	  * @return Whether any InvitationResponse instance was affected
	  */
	def wereAccepted_=(newAccepted: Boolean)(implicit connection: Connection) = 
		putColumn(model.acceptedColumn, newAccepted)
	
	/**
	  * Updates the blocked of the targeted InvitationResponse instance(s)
	  * @param newBlocked A new blocked to assign
	  * @return Whether any InvitationResponse instance was affected
	  */
	def wereBlocked_=(newBlocked: Boolean)(implicit connection: Connection) = 
		putColumn(model.blockedColumn, newBlocked)
}

