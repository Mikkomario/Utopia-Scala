package utopia.exodus.rest.resource.user.me

import utopia.access.http.Method.Post
import utopia.access.http.Status.{Forbidden, NotFound, Unauthorized}
import utopia.citadel.database.access.single.organization.DbInvitation
import utopia.citadel.database.access.single.user.{DbUser, DbUserSettings}
import utopia.citadel.database.model.organization.InvitationResponseModel
import utopia.exodus.model.enumeration.ExodusScope.JoinOrganization
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.metropolis.model.combined.organization.InvitationWithResponse
import utopia.metropolis.model.partial.organization.InvitationResponseData
import utopia.metropolis.model.post.NewInvitationResponse
import utopia.nexus.http.Path
import utopia.nexus.rest.LeafResource
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
 * A rest resource that allows access to an invitation response
 * @author Mikko Hilpinen
 * @since 6.5.2020, v1
 */
case class InvitationResponseNode(invitationId: Int) extends LeafResource[AuthorizedContext]
{
	// IMPLEMENTED	--------------------------
	
	override def name = "response"
	override def allowedMethods = Vector(Post)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.authorizedForScope(JoinOrganization) { (token, connection) =>
			// Parses the invitation response
			context.handlePost(NewInvitationResponse) { newResponse =>
				// Makes sure the token is tied to a specific user (may use an email validation)
				implicit val c: Connection = connection
				token.pullOwnerId match {
					case Some(userId) =>
						// Makes sure the invitation exists,
						// hasn't been answered or expired yet and is targeted for this user
						val invitationAccess = DbInvitation(invitationId)
						invitationAccess.pull match {
							case Some(invitation) =>
								// Tests whether the invitation is for this user
								if (invitation.recipientId.contains(userId) || invitation.recipientEmail
									.exists { email => DbUserSettings.forUserWithId(userId).email.contains(email) })
								{
									if (invitation.hasExpired)
										Result.Failure(Forbidden, "This invitation has already expired")
									else
									{
										invitationAccess.response.pull match
										{
											case Some(earlierResponse) =>
												// If there was a response, will not create a new one
												if (earlierResponse.accepted == newResponse.wasAccepted &&
													earlierResponse.blocked == newResponse.wasBlocked &&
													earlierResponse.message == newResponse.message)
													Result.Success(
														InvitationWithResponse(invitation, Some(earlierResponse))
															.toModel)
												else
													Result.Failure(Forbidden,
														"You've already responded to this invitation")
											case None =>
												// Saves the new response to DB
												val response = InvitationResponseModel.insert(
													InvitationResponseData(invitation.id,
														newResponse.message.map { _.trim }.filter { _.nonEmpty },
														Some(userId), accepted = newResponse.wasAccepted,
														blocked = newResponse.wasBlocked))
												val invitationWithResponse = invitation + response
												val style = token.modelStyle
												// Adds this user to the organization (if the invitation was accepted)
												if (response.accepted)
												{
													val membership = DbUser(userId)
														.membershipInOrganizationWithId(invitation.organizationId)
														.start(invitation.startingRoleId,
															Some(invitation.senderId.getOrElse(userId)))
													Result.Success(Model(Vector(
														"invitation" -> invitationWithResponse.toModelWith(style),
														"membership" -> membership.toModelWith(style))))
												}
												// Returns the original invitation, along with the posted response
												else
													Result.Success(invitationWithResponse.toModelWith(style))
										}
									}
								}
								else
									Result.Failure(Unauthorized, "This invitation is not for you")
							case None =>
								Result.Failure(NotFound, s"There doesn't exist an invitation with id $invitationId")
						}
					case None => Result.Failure(Unauthorized, "Your current session doesn't specify who you are")
				}
			}
		}
	}
}
