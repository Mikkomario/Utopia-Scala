package utopia.exodus.rest.resource.user.me

import utopia.access.http.Method.{Get, Post}
import utopia.access.http.Status.Forbidden
import utopia.citadel.database.access.many.description.DbDescriptionRoles
import utopia.citadel.database.access.many.organization.DbInvitations
import utopia.citadel.database.access.single.user.{DbUser, DbUserSettings}
import utopia.citadel.database.model.organization.InvitationResponseModel
import utopia.exodus.model.enumeration.ExodusScope.{JoinOrganization, ReadOrganizationData, ReadPersonalData}
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext
import utopia.flow.collection.value.typeless.Model
import utopia.flow.generic.ValueConversions._
import utopia.flow.operator.EqualsExtensions._
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.enumeration.ModelStyle.{Full, Simple}
import utopia.metropolis.model.partial.organization.InvitationResponseData
import utopia.metropolis.model.post.NewInvitationResponse
import utopia.nexus.http.Path
import utopia.nexus.rest.{LeafResource, Resource}
import utopia.nexus.rest.ResourceSearchResult.{Error, Follow}
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
 * A rest resource for accessing invitations that are pending for the logged user
 * @author Mikko Hilpinen
 * @since 6.5.2020, v1
 */
object MyInvitationsNode extends Resource[AuthorizedContext]
{
	override val name = "invitations"
	override val allowedMethods = Vector(Get)
	
	override def follow(path: Path)(implicit context: AuthorizedContext) =
	{
		val next = path.head
		if (next ~== MyInvitationResponsesNode.name)
			Follow(MyInvitationResponsesNode, path.tail)
		else
			path.head.int match {
				case Some(id) => Follow(InvitationNode(id), path.tail)
				case None => Error(message = Some(s"${path.head} is not a valid invitation id"))
			}
	}
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.authorizedForScopes(ReadOrganizationData, ReadPersonalData) { (token, connection) =>
			implicit val c: Connection = connection
			implicit val languageIds: LanguageIds = token.languageIds
			// Returns the currently open invitations with contextual information
			val emailAddress = token.token.pullValidatedEmailAddress
			val invitations = openInvitationsAccess(token.ownerId, emailAddress) match {
				case Some(access) => access.detailed
				case None => Vector()
			}
			val modelStyle = context.modelStyle.getOrElse(ExodusContext.defaultModelStyle)
			val invitationModels = modelStyle match {
				case Simple =>
					val descriptionRoles = DbDescriptionRoles.pull
					invitations.map { _.toSimpleModelUsing(descriptionRoles) }
				case Full => invitations.map { _.toModel }
			}
			Result.Success(invitationModels)
		}
	}
	
	
	// OTHER    --------------------------------
	
	// Finds invitations using user id and/or email address
	private def openInvitationsAccess(userId: Option[Int], emailAddress: Option[String])
	                                 (implicit connection: Connection) =
	{
		lazy val access = DbInvitations.withResponses.notAnswered
		userId match {
			case Some(userId) =>
				emailAddress match {
					case Some(email) => Some(access.forRecipient(userId, email))
					case None => Some(access.forRecipientWithId(userId))
				}
			case None =>
				emailAddress match {
					case Some(email) => Some(access.forEmailAddress(email))
					case None => None
				}
		}
	}
	
	
	// NESTED   ------------------------------
	
	/**
	  * A rest node which is used for answering all pending invitations at once
	  */
	object MyInvitationResponsesNode extends LeafResource[AuthorizedContext]
	{
		// ATTRIBUTES   ------------------------
		
		override val name = "responses"
		override val allowedMethods = Vector(Post)
		
		
		// IMPLEMENTED  ------------------------
		
		override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
		{
			context.authorizedForScope(JoinOrganization) { (token, connection) =>
				implicit val c: Connection = connection
				
				lazy val emailAddress = token.pullValidatedEmailAddress
				lazy val invitations = openInvitationsAccess(token.ownerId, emailAddress) match {
					case Some(access) => access.pull
					case None => Vector()
				}
				lazy val modelStyle = token.modelStyle
				
				def _joinAll(userId: Int) = invitations.map { invitation =>
					DbUser(userId).membershipInOrganizationWithId(invitation.organizationId)
						.start(invitation.startingRoleId, Some(invitation.senderId.getOrElse(userId)))
				}
				
				// Checks whether the invitation is linked to an existing user, which greatly affects request handling
				token.ownerId.orElse { emailAddress.flatMap { DbUserSettings.withEmail(_).userId } } match {
					// Case: Is linked with an existing user => Answers that user's invitation(s)
					case Some(userId) =>
						context.handlePost(NewInvitationResponse) { newResponse =>
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
					// Case: Not linked with an existing user => Fails
					case None => Result.Failure(Forbidden, "You need to create a user account first")
				}
			}
		}
	}
}
