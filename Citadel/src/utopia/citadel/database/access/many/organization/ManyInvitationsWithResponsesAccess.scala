package utopia.citadel.database.access.many.organization

import utopia.citadel.database.access.many.user.DbManyUserSettings
import utopia.citadel.database.factory.organization.InvitationWithResponseFactory
import utopia.citadel.database.model.organization.InvitationResponseModel
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.combined.organization.{DetailedInvitation, InvitationWithResponse}
import utopia.metropolis.model.stored.user.UserSettings
import utopia.vault.database.Connection
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

object ManyInvitationsWithResponsesAccess extends ViewFactory[ManyInvitationsWithResponsesAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyInvitationsWithResponsesAccess = 
		new _ManyInvitationsWithResponsesAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyInvitationsWithResponsesAccess(condition: Condition) 
		extends ManyInvitationsWithResponsesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points that return invitations with responses included
  * @author Mikko Hilpinen
  * @since 24.10.2021, v2.0
  */
trait ManyInvitationsWithResponsesAccess 
	extends ManyInvitationsAccessLike[InvitationWithResponse, ManyInvitationsWithResponsesAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to blocked invitations
	  */
	def blocked = filter(responseModel.blocked.toCondition)
	
	/**
	  * An access point to invitations which are open but without response
	  */
	def notAnswered = filter(factory.notLinkedCondition)
	
	/**
	  * Whether there exists an accessible response that blocks future invitations
	  * @param connection Implicit DB Connection
	  */
	def containsBlocked(implicit connection: Connection) = exists(responseModel.blocked.toCondition)
	
	/**
	  * Detailed copies of accessible invitations
	  * @param connection Implicit DB connection
	  * @param languageIds Language ids to use when reading organization descriptions
	  */
	def detailed(implicit connection: Connection, languageIds: LanguageIds) = 
		detailedInLanguages(Some(languageIds))
	
	/**
	  * Model used for interacting with invitation responses
	  */
	protected def responseModel = InvitationResponseModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = InvitationWithResponseFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyInvitationsWithResponsesAccess = 
		ManyInvitationsWithResponsesAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * @param languageIds Language ids to use when reading organization descriptions
	  * @param connection Implicit DB connection
	  * @return Detailed copies of accessible invitations
	  */
	def detailedInLanguages(languageIds: Option[LanguageIds])(implicit connection: Connection) = {
		// Reads invitation data, then attaches organization and user data
		val invitations = pull
		if (invitations.isEmpty)
			Vector()
		else
		{
			val organizationIds = invitations.map { _.organizationId }.toSet
			val organizationsAccess = DbOrganizations(organizationIds)
			// Organization descriptions may be read in requested languages, or in all languages
			val organizations = languageIds match {
				case Some(languageIds) =>
					implicit val l: LanguageIds = languageIds
					organizationsAccess.described
				case None => organizationsAccess.fullyDescribed
			}
			val organizationsById = organizations.map { o => o.id -> o }.toMap
			val senderIds = invitations.flatMap { _.senderId }.toSet
			val senderDataByUserId: Map[Int, UserSettings] = if (senderIds.isEmpty) Map() else
				DbManyUserSettings.forAnyOfUsers(senderIds).pull.map { s => s.userId -> s }.toMap
			invitations.map { invitation =>
				DetailedInvitation(invitation, organizationsById(invitation.organizationId),
					invitation.senderId.flatMap(senderDataByUserId.get))
			}
		}
	}
}

