package utopia.exodus.rest.resource.user.me

import utopia.access.http.Method.Get
import utopia.citadel.database.access.many.description.DbDescriptionRoles
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.enumeration.ModelStyle.{Full, Simple}
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
		context.sessionTokenAuthorized { (session, connection) =>
			implicit val c: Connection = connection
			implicit val languageIds: LanguageIds = session.languageIds
			// Reads invitations from DB
			val invitations = session.userAccess.receivedInvitations.notAnswered.detailed
			// May use simple model format
			Result.Success(session.modelStyle match {
				case Full => invitations.map { _.toModel }
				case Simple =>
					val roles = DbDescriptionRoles.pull
					invitations.map { _.toSimpleModelUsing(roles) }
			})
		}
	}
	
	override def follow(path: Path)(implicit context: AuthorizedContext) =
	{
		path.head.int match {
			case Some(id) => Follow(InvitationNode(id), path.tail)
			case None => Error(message = Some(s"${path.head} is not a valid invitation id"))
		}
	}
}
