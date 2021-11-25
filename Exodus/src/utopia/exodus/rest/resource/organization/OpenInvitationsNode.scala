package utopia.exodus.rest.resource.organization

import utopia.access.http.Method.{Get, Post}
import utopia.citadel.database.access.many.organization.DbInvitations
import utopia.citadel.database.access.single.user.DbUser
import utopia.citadel.database.model.organization.InvitationResponseModel
import utopia.exodus.model.enumeration.StandardEmailValidationPurpose.OrganizationInvitation
import utopia.exodus.rest.resource.user.UsersNode
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.organization.InvitationResponseData
import utopia.metropolis.model.post.NewInvitationResponse
import utopia.nexus.http.Path
import utopia.nexus.rest.{LeafResource, Resource, ResourceWithChildren}
import utopia.nexus.result.Result
import utopia.vault.database.Connection

import scala.util.Success

/**
  * Used for answering an open organization invitation using an email validation token,
  * which results in a new user being created. Intended to appear as: invitations/open
  * @author Mikko Hilpinen
  * @since 25.11.2021, v3.1
  */
object OpenInvitationsNode
	extends ResourceWithChildren[AuthorizedContext]
{
	// ATTRIBUTES  -----------------------------
	
	override val name = "open"
	override val allowedMethods = Vector(Get)
	
	
	// IMPLEMENTED  ----------------------------
	
	override def children = Vector[Resource[AuthorizedContext]](ResponsesNode)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.emailAuthorized(OrganizationInvitation.id) { (attempt, connection) =>
			implicit val c: Connection = connection
			// Returns the currently open invitations
			val invitations = openInvitationsForEmail(attempt.email)
			val modelStyle = context.modelStyle.getOrElse(ExodusContext.defaultModelStyle)
			// Doesn't close the email validation
			false -> Result.Success(invitations.map { _.toModelWith(modelStyle) })
		}
	}
	
	
	// OTHER    --------------------------------
	
	private def openInvitationsForEmail(emailAddress: String)(implicit connection: Connection) =
		DbInvitations.withResponses.notAnswered.forEmailAddress(emailAddress).pull
	
	
	// NESTED   --------------------------------
	
	private object ResponsesNode extends LeafResource[AuthorizedContext]
	{
		// ATTRIBUTES   ------------------------
		
		override val name = "responses"
		override val allowedMethods = Vector(Post)
		
		
		// IMPLEMENTED  ------------------------
		
		override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
		{
			// Authorizes using an email token
			context.emailAuthorized(OrganizationInvitation.id) { (validation, connection) =>
				implicit val c: Connection = connection
				
				lazy val invitations = openInvitationsForEmail(validation.email)
				lazy val modelStyle = context.modelStyle.getOrElse(ExodusContext.defaultModelStyle)
				
				def _joinAll(userId: Int) = invitations.map { invitation =>
					DbUser(userId).membershipInOrganizationWithId(invitation.organizationId)
						.start(invitation.startingRoleId, invitation.senderId.getOrElse(userId))
				}
				
				// Checks whether the invitation is linked to an existing user, which greatly affects request handling
				validation.userId match {
					// Case: Is linked with an existing user => Answers that user's invitation(s)
					case Some(userId) =>
						val responseResult = context.handlePost(NewInvitationResponse) { newResponse =>
							// Finds all open invitations and answers them
							val responses = InvitationResponseModel.insert(
								invitations.map { invitation =>
									InvitationResponseData(invitation.id, newResponse.message, Some(userId),
										accepted = newResponse.wasAccepted, blocked = newResponse.wasBlocked) })
							
							val invitationModels = (invitations zip responses)
								.map { case (invitation, response) => (invitation + response).toModelWith(modelStyle) }
							// If invitations were accepted, joins the user to those organizations
							if (newResponse.wasAccepted) {
								val memberships = _joinAll(userId)
								// In that case, returns { invitations: [...], memberships: [...] }
								Result.Success(Model(Vector(
									"invitations" -> invitationModels,
									"memberships" -> memberships.map { _.toModelWith(modelStyle) })))
							}
							// If rejected, returns [Invitation & Response]
							else
								Result.Success(invitationModels)
						}
						// Closes this validation if request was handled successfully
						responseResult.isSuccess -> responseResult
					// Case: Not linked with an existing user =>
					// Creates a new user and answers the invitation(s) positively
					case None =>
						val creationResult = UsersNode.insertUser { newUser =>
							Success(newUser.copy(email = Some(validation.email))) } { result =>
							// Answers all of those invitations positively
							val responses = InvitationResponseModel.insert(invitations.map { invitation =>
								InvitationResponseData(invitation.id, creatorId = Some(result.userId), accepted = true)
							})
							val newMemberships = _joinAll(result.userId)
							// Includes invitations and responses in the result
							Result.Success(result.toModelWith(modelStyle) +
								Constant("invitations", (invitations zip responses).map { case (invitation, response) =>
									(invitation + response).toModelWith(modelStyle) }) +
								Constant("memberships", newMemberships.map { _.toModelWith(modelStyle) }))
						}
						// Closes the email validation on successful user creation
						creationResult.isSuccess -> creationResult
				}
			}
		}
	}
}
