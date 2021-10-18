package utopia.exodus.rest.resource.description

import utopia.access.http.Method.Get
import utopia.citadel.database.access.id.many.DbTaskIds
import utopia.citadel.database.access.many.description.{DbDescriptionRoles, DbTaskDescriptions}
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.combined.organization.DescribedTask
import utopia.metropolis.model.enumeration.ModelStyle.{Full, Simple}
import utopia.nexus.http.Path
import utopia.nexus.rest.ResourceSearchResult.{Error, Follow}
import utopia.nexus.rest.Resource
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * Used for describing all available task types
  * @author Mikko Hilpinen
  * @since 21.5.2020, v1.0
  */
object TasksNode extends Resource[AuthorizedContext]
{
	override val name = "tasks"
	
	override val allowedMethods = Vector(Get)
	
	// Provides access to individual task nodes
	override def follow(path: Path)(implicit context: AuthorizedContext) =
		path.head.int match
		{
			case Some(taskId) => Follow(TaskNode(taskId), path.tail)
			case None => Error(message = Some(s"${path.head} is not a valid task id"))
		}
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.sessionKeyAuthorized { (session, connection) =>
			implicit val c: Connection = connection
			implicit val languageIds: LanguageIds = context.languageIdListFor(session.userId)
			// Reads task descriptions
			val taskIds = DbTaskIds.all
			val descriptions = DbTaskDescriptions(taskIds.toSet).inPreferredLanguages
			// Combines the descriptions with the tasks and returns them
			val describedTasks = taskIds.map { taskId => DescribedTask(taskId,
				descriptions.getOrElse(taskId, Set()).toSet) }
			// May use simpler model style
			session.modelStyle match
			{
				case Full => Result.Success(describedTasks.map { _.toModel })
				case Simple =>
					val roles = DbDescriptionRoles.all
					Result.Success(describedTasks.map { _.toSimpleModelUsing(roles) })
			}
		}
	}
}
