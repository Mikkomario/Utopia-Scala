package utopia.exodus.rest.resource.description

import utopia.access.http.Method.Get
import utopia.exodus.database.access.many.DbDescriptions
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.combined.organization.DescribedTask
import utopia.metropolis.model.enumeration.TaskType
import utopia.nexus.http.Path
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.Error
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * Used for describing all available task types
  * @author Mikko Hilpinen
  * @since 21.5.2020, v1
  */
object TasksNode extends Resource[AuthorizedContext]
{
	override val name = "tasks"
	
	override val allowedMethods = Vector(Get)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.sessionKeyAuthorized { (session, connection) =>
			implicit val c: Connection = connection
			val languageIds = context.languageIdListFor(session.userId)
			// Reads task descriptions
			val descriptions = DbDescriptions.ofAllTasks.inLanguages(languageIds)
			// Combines the descriptions with the tasks and returns them
			val describedTasks = TaskType.values.map { task => DescribedTask(task,
				descriptions.getOrElse(task.id, Set()).toSet) }
			Result.Success(describedTasks.map { _.toModel })
		}
	}
	
	override def follow(path: Path)(implicit context: AuthorizedContext) = Error(message = Some(
		"Tasks currently doesn't contain any sub-nodes"))
}
