package utopia.exodus.rest.resource.user

import utopia.access.http.Method.Post
import utopia.access.http.Status.{Forbidden, NotFound, Unauthorized}
import utopia.exodus.database.access.single.{DbInvitation, DbOrganization, DbUser}
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.combined.organization.InvitationWithResponse
import utopia.metropolis.model.post.NewInvitationResponse
import utopia.nexus.http.Path
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.Error
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * A rest resource that allows access to an invitation response
  * @author Mikko Hilpinen
  * @since 6.5.2020, v1
  */
case class InvitationResponseNode(invitationId: Int) extends Resource[AuthorizedContext]
{
	// IMPLEMENTED	--------------------------
	
	override val name = "response"
	
	override val allowedMethods = Vector(Post)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.sessionKeyAuthorized { (session, connection) =>
			// Parses the response
			context.handlePost(NewInvitationResponse) { newResponse =>
				// Makes sure the invitation exists, hasn't been answered or expired yet and is targeted for this user
				implicit val c: Connection = connection
				val accessInvitation = DbInvitation(invitationId)
				accessInvitation.pull match
				{
					case Some(invitation) =>
						val isForThisUser = invitation.recipient match
						{
							case Right(recipientId) => recipientId == session.userId
							case Left(recipientEmail) =>
								val myEmail = DbUser(session.userId).settings.map { _.email }
								myEmail.contains(recipientEmail)
						}
						if (isForThisUser)
						{
							if (invitation.hasExpired)
								Result.Failure(Forbidden, "This invitation has already expired")
							else
							{
								accessInvitation.response.pull match
								{
									case Some(earlierResponse) =>
										// If there was a response, will not create a new one
										if (earlierResponse.wasAccepted == newResponse.wasAccepted &&
											earlierResponse.wasBlocked == newResponse.wasBlocked)
											Result.Success(InvitationWithResponse(invitation, earlierResponse).toModel)
										else
											Result.Failure(Forbidden, "You've already responded to this invitation")
									case None =>
										// Saves the new response to DB
										val savedResponse = accessInvitation.response.insert(newResponse, session.userId)
										// Adds this user to the organization (if the invitation was accepted)
										if (savedResponse.wasAccepted)
											DbOrganization(invitation.organizationId).memberships.insert(
												session.userId, invitation.startingRoleId,
												invitation.creatorId.getOrElse(session.userId))
										// Returns the original invitation, along with the posted response
										Result.Success(InvitationWithResponse(invitation, savedResponse).toModel)
								}
							}
						}
						else
							Result.Failure(Unauthorized, "This invitation is not for you")
					case None => Result.Failure(NotFound, s"There doesn't exist an invitation with id $invitationId")
				}
			}
		}
	}
	
	override def follow(path: Path)(implicit context: AuthorizedContext) = Error(message = Some(
		"Invitation response doesn't have any child nodes"))
}
