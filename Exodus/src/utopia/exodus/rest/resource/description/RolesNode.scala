package utopia.exodus.rest.resource.description

import utopia.access.http.Method.Get
import utopia.citadel.database.access.many.description.DbDescriptionRoles
import utopia.citadel.database.access.many.organization.DbUserRoles
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.enumeration.ModelStyle.{Full, Simple}
import utopia.nexus.http.Path
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.Error
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * This node is used for listing all possible user roles and their descriptions, including associated task ids
  * @author Mikko Hilpinen
  * @since 20.5.2020, v1
  */
object RolesNode extends Resource[AuthorizedContext]
{
	override val name = "user-roles"
	
	override val allowedMethods = Vector(Get)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.sessionTokenAuthorized { (session, connection) =>
			implicit val c: Connection = connection
			implicit val languageIds: LanguageIds = session.languageIds
			// Reads all user roles and their allowed tasks
			val roles = DbUserRoles.detailed
			// Supports simple model style if needed
			session.modelStyle match
			{
				case Full => Result.Success(roles.map { _.toModel })
				case Simple =>
					val descriptionRoles = DbDescriptionRoles.all
					Result.Success(roles.map { _.toSimpleModelUsing(descriptionRoles) })
			}
		}
	}
	
	override def follow(path: Path)(implicit context: AuthorizedContext) = Error(message = Some(
		"user-roles doesn't contain any sub-nodes at this time"))
}
