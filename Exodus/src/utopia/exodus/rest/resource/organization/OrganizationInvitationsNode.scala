package utopia.exodus.rest.resource.organization

import utopia.access.http.Method.Post
import utopia.access.http.Status.{BadRequest, Forbidden, NotImplemented}
import utopia.exodus.database.access.id.UserId
import utopia.exodus.database.access.single.{DbMembership, DbOrganization, DbUser}
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.StringExtensions._
import utopia.flow.util.TimeExtensions._
import utopia.metropolis.model.enumeration.TaskType.InviteMembers
import utopia.metropolis.model.post.NewInvitation
import utopia.metropolis.model.stored.organization.Invitation
import utopia.nexus.http.Path
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.Error
import utopia.nexus.result.Result
import utopia.vault.database.Connection

import scala.util.{Failure, Success}

/**
  * An access point to an organization's invitations
  * @author Mikko Hilpinen
  * @since 5.5.2020, v1
  */
case class OrganizationInvitationsNode(organizationId: Int) extends Resource[AuthorizedContext]
{
	override val name = "invitations"
	
	override val allowedMethods = Vector(Post)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		// Checks authorization first
		context.authorizedForTask(organizationId, InviteMembers) { (session, membershipId, connection) =>
			// Parses the posted invitation
			context.handlePost(NewInvitation) { newInvitation =>
				newInvitation.validated match
				{
					case Success(validInvitation) =>
						implicit val c: Connection = connection
						// Makes sure the user has a right to give the specified role to another user
						if (DbMembership(membershipId).canPromoteTo(validInvitation.startingRole))
						{
							// Finds the user that is being invited (if registered)
							val recipientEmail = validInvitation.recipientEmail
							val recipientUserId = UserId.forEmail(recipientEmail)
							
							// Checks whether the user already is a member of this organization
							if (recipientUserId.exists { userId =>
								DbUser(userId).isMemberInOrganizationWithId(organizationId) })
								Result.Success(invitationSendResultModel(wasInvitationSend = false,
									description = "The user was already a member of this organization"))
							else
							{
								// Makes sure the user hasn't blocked this organization from sending invites
								// And that there are no pending invitations for this user
								val accessInvitations = DbOrganization(organizationId).invitations
								val blockedInvitations = accessInvitations.blocked
								val isBlocked = blockedInvitations.exists { i =>
									recipientUserId match
									{
										case Some(userId) => i.recipientId == userId
										case None => i.wrapped.recipientEmail.contains(recipientEmail)
									}
								}
								if (isBlocked)
									Result.Failure(Forbidden,
										"The recipient has blocked you from sending further invitations")
								else
								{
									accessInvitations.pending.find { i =>
										i.recipient match
										{
											case Right(userId) => recipientUserId.contains(userId)
											case Left(email) => email ~== recipientEmail
										}
									} match
									{
										case Some(pending) =>
											// If there was a pending invitation, won't send another but
											// registers this as a success
											Result.Success(invitationSendResultModel(
												wasInvitationSend = false, Some(pending),
												"There already existed a pending invitation for that user"))
										case None =>
											// Creates a new invitation and saves it
											val invitation = accessInvitations.send(
												recipientUserId.map { Right(_) }.getOrElse(Left(recipientEmail)),
												validInvitation.startingRole, session.userId,
												validInvitation.durationDays.days)
											Result.Success(invitationSendResultModel(wasInvitationSend = true,
												Some(invitation)))
									}
								}
							}
						}
						else
							Result.Failure(Forbidden, s"You're not allowed to promote a user to ${
								validInvitation.startingRole}")
					case Failure(error) => Result.Failure(BadRequest, error.getMessage)
				}
			}
		}
	}
	
	override def follow(path: Path)(implicit context: AuthorizedContext) = Error(NotImplemented,
		Some("Invitation access is not implemented yet"))
	
	
	// OTHER	---------------------------
	
	private def invitationSendResultModel(wasInvitationSend: Boolean, invitation: Option[Invitation] = None, description: String = "") =
	{
		Model(Vector("was_sent" -> wasInvitationSend, "invitation" -> invitation.map { _.toModel },
			"description" -> description.notEmpty))
	}
}
