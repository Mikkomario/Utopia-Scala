package utopia.ambassador.model.stored.scope

import utopia.ambassador.model.partial.scope.TaskScopeLinkData
import utopia.vault.model.template.Stored

/**
  * Represents a stored link between a task and a scope it requires
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
case class TaskScopeLink(id: Int, data: TaskScopeLinkData) extends Stored[TaskScopeLinkData, Int]
