package utopia.exodus.rest.resource.user

import utopia.access.http.Method.Get
import utopia.exodus.database.access.many.DbDescriptions
import utopia.exodus.database.access.single.DbUser
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.combined.organization.DescribedInvitation
import utopia.nexus.http.Path
import utopia.nexus.rest.Resource
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
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.sessionKeyAuthorized { (session, connection) =>
			implicit val c: Connection = connection
			// Reads invitations from DB
			val pendingInvitations = DbUser(session.userId).receivedInvitations.pending
			// Attaches metadata to the invitations
			if (pendingInvitations.nonEmpty)
			{
				// Reads organization descriptions
				val languageIds = context.languageIdListFor(session.userId)
				val organizationIds = pendingInvitations.map { _.organizationId }.toSet
				val organizationDescriptions = DbDescriptions.ofOrganizationsWithIds(organizationIds)
					.inLanguages(languageIds)
				
				// Attaches sender data to each invitation where applicable
				val enrichedInvitations = pendingInvitations.map { invitation =>
					val sender = invitation.creatorId.flatMap { DbUser(_).settings.pull }
					DescribedInvitation(invitation,
						organizationDescriptions.getOrElse(invitation.organizationId, Set()).toSet, sender)
				}
				
				Result.Success(enrichedInvitations.map { _.toModel })
			}
			else
				Result.Empty
		}
	}
	
	override def follow(path: Path)(implicit context: AuthorizedContext) =
	{
		path.head.int match
		{
			case Some(id) => Follow(InvitationNode(id), path.tail)
			case None => Error(message = Some(s"${path.head} is not a valid invitation id"))
		}
	}
}
