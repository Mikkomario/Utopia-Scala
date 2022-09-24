package utopia.exodus.rest.resource.organization

import utopia.access.http.Method.Post
import utopia.access.http.Status.{Forbidden, Unauthorized}
import utopia.citadel.database.access.id.single.DbUserId
import utopia.citadel.database.access.single.organization.{DbMembership, DbOrganization}
import utopia.citadel.database.access.single.user.DbUser
import utopia.citadel.database.model.organization.InvitationModel
import utopia.exodus.model.enumeration.ExodusEmailValidationPurpose.OrganizationInvitation
import utopia.exodus.model.enumeration.ExodusScope.{CreateUser, JoinOrganization, OrganizationActions, ReadGeneralData, ReadOrganizationData, ReadPersonalData}
import utopia.exodus.model.enumeration.ExodusTask.InviteMembers
import utopia.exodus.rest.resource.scalable.{ExtendableOrganizationResource, ExtendableOrganizationResourceFactory, OrganizationUseCaseImplementation}
import utopia.exodus.util.ExodusContext
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.time.Now
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.util.StringExtensions._
import utopia.metropolis.model.partial.organization.InvitationData
import utopia.metropolis.model.post.NewInvitation
import utopia.metropolis.model.stored.organization.Invitation
import utopia.nexus.result.Result
import utopia.vault.database.Connection
import ExodusContext.uuidGenerator
import utopia.flow.generic.model.immutable.Model

object OrganizationInvitationsNode extends ExtendableOrganizationResourceFactory[OrganizationInvitationsNode]
{
	override protected def buildBase(param: Int) = new OrganizationInvitationsNode(param)
}

/**
  * An access point to an organization's invitations
  * @author Mikko Hilpinen
  * @since 5.5.2020, v1
  */
class OrganizationInvitationsNode(organizationId: Int) extends ExtendableOrganizationResource(organizationId)
{
	override val name = "invitations"
	
	private val defaultPost = OrganizationUseCaseImplementation
		.default { (session, membershipId, connection, context, _) =>
			implicit val c: Connection = connection
			val membershipAccess = DbMembership(membershipId)
			// Makes sure the request is authorized
			if (session.access.hasScope(OrganizationActions) && membershipAccess.allowsTaskWithId(InviteMembers.id)) {
				// Parses the posted invitation
				context.handlePost(NewInvitation) { newInvitation =>
					implicit val c: Connection = connection
					// Makes sure the user has a right to give the specified role to another user
					if (DbMembership(membershipId).canPromoteToRoleWithId(newInvitation.startingRoleId))
					{
						// Finds the user that is being invited (if registered)
						val recipientEmail = newInvitation.recipientEmail
						val recipientUserId = DbUserId.forEmail(recipientEmail)
						
						// Checks whether the user already is a member of this organization
						if (recipientUserId.exists { userId =>
							DbUser(userId).isMemberOfOrganizationWithId(organizationId) })
							Result.Success(invitationSendResultModel(
								description = "The user was already a member of this organization"))
						else
						{
							// Makes sure the user hasn't blocked this organization from sending invites
							val historicalInvitationsAccess = {
								val base = DbOrganization(organizationId).currentAndPastInvitations.withResponses
								recipientUserId match
								{
									case Some(recipientId) => base.forRecipient(recipientId, recipientEmail)
									case None => base.forEmailAddress(recipientEmail)
								}
							}
							if (historicalInvitationsAccess.containsBlocked)
								Result.Failure(Forbidden,
									"The recipient has blocked you from sending further invitations")
							else
							{
								// Checks whether there is already a pending non-deprecated invitation without
								// an answer
								val activeInvitationsAccess =
								{
									val base = DbOrganization(organizationId).currentInvitations.withResponses
									recipientUserId match
									{
										case Some(recipientId) => base.forRecipient(recipientId, recipientEmail)
										case None => base.forEmailAddress(recipientEmail)
									}
								}
								activeInvitationsAccess.notAnswered.pull.headOption match
								{
									case Some(pending) =>
										// If there was a pending invitation, won't send another but
										// registers this as a success
										Result.Success(invitationSendResultModel(Some(pending.invitation),
											"There already existed a pending invitation for that user"))
									case None =>
										// Creates a new invitation and saves it
										val invitation = InvitationModel.insert(InvitationData(organizationId,
											newInvitation.startingRoleId, Now + newInvitation.duration,
											recipientUserId, Some(recipientEmail), newInvitation.message.notEmpty,
											session.ownerId))
										// Records a new email validation attempt based on the invitation,
										// if possible
										val sendSucceeded = ExodusContext.emailValidator.exists { validator =>
											val result = validator(newInvitation.recipientEmail,
												OrganizationInvitation,
												Set(JoinOrganization.id, ReadOrganizationData.id, ReadPersonalData.id),
												Set(JoinOrganization.id, ReadGeneralData.id, ReadPersonalData.id,
													ReadOrganizationData.id, CreateUser.id),
												Some(session.id),
												session.modelStylePreference.orElse(context.modelStyle),
												Some(invitation))
											result.failure.foreach { error =>
												ExodusContext.logger(error, "Failed to send an organization invitation")
											}
											result.isSuccess
										}
										// Returns the new invitation
										Result.Success(invitationSendResultModel(
											Some(invitation), invitationWasCreated = true,
											invitationWasSent = sendSucceeded))
								}
							}
						}
					}
					else
						Result.Failure(Forbidden, s"You're not allowed to promote a user to role ${
							newInvitation.startingRoleId}")
				}
			}
			else
				Result.Failure(Unauthorized, "You're not allowed to invite users into this organization")
		}
	
	override protected val defaultUseCaseImplementations = Map(Post -> defaultPost)
	
	
	// IMPLEMENTED  -----------------------
	
	override protected def defaultFollowImplementations = Vector()
	
	
	// OTHER	---------------------------
	
	private def invitationSendResultModel(invitation: Option[Invitation] = None,
	                                      description: String = "", invitationWasCreated: Boolean = false,
	                                      invitationWasSent: Boolean = false) =
		Model(Vector("was_created" -> invitationWasCreated, "was_sent" -> invitationWasSent,
			"invitation" -> invitation.map { _.toModel }, "description" -> description.notEmpty))
}
