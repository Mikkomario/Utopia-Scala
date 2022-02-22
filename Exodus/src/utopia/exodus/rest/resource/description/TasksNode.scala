package utopia.exodus.rest.resource.description

import utopia.citadel.database.access.many.organization.DbTasks
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.combined.organization.DescribedTask
import utopia.nexus.http.Path
import utopia.nexus.rest.ResourceSearchResult.{Error, Follow}
import utopia.vault.database.Connection

/**
  * Used for describing all available task types
  * @author Mikko Hilpinen
  * @since 21.5.2020, v1.0
  */
object TasksNode extends GeneralDataNode[DescribedTask]
{
	override val name = "tasks"
	
	override protected def describedItems(implicit connection: Connection, languageIds: LanguageIds) =
		DbTasks.described
	
	// Provides access to individual task nodes
	override def follow(path: Path)(implicit context: AuthorizedContext) =
		path.head.int match
		{
			case Some(taskId) => Follow(TaskNode(taskId), path.tail)
			case None => Error(message = Some(s"${path.head} is not a valid task id"))
		}
}
