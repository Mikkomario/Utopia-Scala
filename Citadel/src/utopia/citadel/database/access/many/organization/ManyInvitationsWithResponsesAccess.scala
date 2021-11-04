package utopia.citadel.database.access.many.organization

import utopia.citadel.database.access.many.organization.ManyInvitationsWithResponsesAccess.SubAccess
import utopia.citadel.database.access.many.user.DbManyUserSettings
import utopia.citadel.database.factory.organization.InvitationWithResponseFactory
import utopia.citadel.database.model.organization.InvitationResponseModel
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.combined.organization.{DetailedInvitation, InvitationWithResponse}
import utopia.metropolis.model.stored.user.UserSettings
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyInvitationsWithResponsesAccess
{
	private class SubAccess(override val parent: ManyModelAccess[InvitationWithResponse],
	                        override val filterCondition: Condition)
		extends ManyInvitationsWithResponsesAccess with SubView
}

/**
  * A common trait for access points that return invitations with responses included
  * @author Mikko Hilpinen
  * @since 24.10.2021, v2.0
  */
trait ManyInvitationsWithResponsesAccess
	extends ManyInvitationsAccessLike[InvitationWithResponse, ManyInvitationsWithResponsesAccess]
{
	// COMPUTED ---------------------------------------
	
	/**
	  * @return Model used for interacting with invitation responses
	  */
	protected def responseModel = InvitationResponseModel
	
	/**
	  * @return An access point to blocked invitations
	  */
	def blocked = filter(responseModel.blocked.toCondition)
	/**
	  * @return An access point to invitations which are open but without response
	  */
	def notAnswered = filter(factory.notLinkedCondition)
	
	/**
	  * @param connection Implicit DB Connection
	  * @return Whether there exists an accessible response that blocks future invitations
	  */
	def containsBlocked(implicit connection: Connection) = exists(responseModel.blocked.toCondition)
	
	/**
	  * @param connection Implicit DB connection
	  * @param languageIds Language ids to use when reading organization descriptions
	  * @return Detailed copies of accessible invitations
	  */
	def detailed(implicit connection: Connection, languageIds: LanguageIds) =
	{
		// Reads invitation data, then attaches organization and user data
		val invitations = pull
		if (invitations.isEmpty)
			Vector()
		else
		{
			val organizationIds = invitations.map { _.organizationId }.toSet
			val organizationsById = DbOrganizations(organizationIds).described.map { o => o.id -> o }.toMap
			val senderIds = invitations.flatMap { _.senderId }.toSet
			val senderDataByUserId: Map[Int, UserSettings] = if (senderIds.isEmpty) Map() else
				DbManyUserSettings.forAnyOfUsers(senderIds).pull.map { s => s.userId -> s }.toMap
			invitations.map { invitation =>
				DetailedInvitation(invitation, organizationsById(invitation.organizationId),
					invitation.senderId.flatMap(senderDataByUserId.get))
			}
		}
	}
	
	
	// IMPLEMENTED  -----------------------------------
	
	override def factory = InvitationWithResponseFactory
	override protected def defaultOrdering = None
	
	override protected def _filter(condition: Condition): ManyInvitationsWithResponsesAccess =
		new SubAccess(this, condition)
}
