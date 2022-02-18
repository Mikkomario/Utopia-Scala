package utopia.exodus.rest.resource.organization

import utopia.access.http.Method.Post
import utopia.access.http.Status.{BadRequest, Forbidden}
import utopia.citadel.database.access.id.single.DbUserId
import utopia.citadel.database.access.single.organization.{DbMembership, DbOrganization}
import utopia.citadel.database.access.single.user.DbUser
import utopia.citadel.database.model.organization.InvitationModel
import utopia.exodus.database.access.single.auth.DbEmailValidationAttemptOld
import utopia.exodus.model.enumeration.StandardEmailValidationPurpose.OrganizationInvitation
import utopia.exodus.model.enumeration.StandardTask.InviteMembers
import utopia.exodus.rest.resource.scalable.{ExtendableOrganizationResource, ExtendableOrganizationResourceFactory, OrganizationUseCaseImplementation}
import utopia.exodus.util.ExodusContext
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.flow.util.StringExtensions._
import utopia.metropolis.model.partial.organization.InvitationData
import utopia.metropolis.model.post.NewInvitation
import utopia.metropolis.model.stored.organization.Invitation
import utopia.nexus.result.Result
import utopia.vault.database.Connection

import scala.util.{Failure, Success}

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
		.default(Post) { (session, membershipId, connection, context, _) =>
			implicit val c: Connection = connection
			val membershipAccess = DbMembership(membershipId)
			if (membershipAccess.allowsTaskWithId(InviteMembers.id)) {
				// Parses the posted invitation
				context.handlePost(NewInvitation) { newInvitation =>
					newInvitation.validated match
					{
						case Success(validInvitation) =>
							implicit val c: Connection = connection
							// Makes sure the user has a right to give the specified role to another user
							if (DbMembership(membershipId).canPromoteToRoleWithId(validInvitation.startingRoleId))
							{
								// Finds the user that is being invited (if registered)
								val recipientEmail = validInvitation.recipientEmail
								val recipientUserId = DbUserId.forEmail(recipientEmail)
								
								// Checks whether the user already is a member of this organization
								if (recipientUserId.exists { userId =>
									DbUser(userId).isMemberOfOrganizationWithId(organizationId) })
									Result.Success(invitationSendResultModel(wasInvitationSend = false,
										description = "The user was already a member of this organization"))
								else
								{
									// Makes sure the user hasn't blocked this organization from sending invites
									val historicalInvitationsAccess =
									{
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
												Result.Success(invitationSendResultModel(
													wasInvitationSend = false, Some(pending.invitation),
													"There already existed a pending invitation for that user"))
											case None =>
												// Creates a new invitation and saves it
												val invitation = InvitationModel.insert(InvitationData(organizationId,
													newInvitation.startingRoleId, Now + newInvitation.duration,
													recipientUserId, Some(recipientEmail), newInvitation.message.notEmpty,
													Some(session.userId)))
												// Records a new email validation attempt based on the invitation,
												// if possible
												ExodusContext.emailValidator.foreach { implicit validator =>
													DbEmailValidationAttemptOld.start(newInvitation.recipientEmail,
														OrganizationInvitation.id, invitation.recipientId)
												}
												// Returns the new invitation
												Result.Success(invitationSendResultModel(
													wasInvitationSend = true, Some(invitation)))
										}
									}
								}
							}
							else
								Result.Failure(Forbidden, s"You're not allowed to promote a user to role ${
									validInvitation.startingRoleId}")
						case Failure(error) => Result.Failure(BadRequest, error.getMessage)
					}
				}
			}
			else
				Result.Failure(Forbidden, "You're not allowed to invite users into this organization")
		}
	
	override protected val defaultUseCaseImplementations = Vector(defaultPost)
	
	
	// IMPLEMENTED  -----------------------
	
	override protected def defaultFollowImplementations = Vector()
	
	
	// OTHER	---------------------------
	
	private def invitationSendResultModel(wasInvitationSend: Boolean, invitation: Option[Invitation] = None,
	                                      description: String = "") =
		Model(Vector("was_sent" -> wasInvitationSend, "invitation" -> invitation.map { _.toModel },
			"description" -> description.notEmpty))
}
