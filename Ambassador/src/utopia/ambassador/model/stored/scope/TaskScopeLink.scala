package utopia.ambassador.model.stored.scope

import utopia.ambassador.database.access.single.scope.DbSingleTaskScopeLink
import utopia.ambassador.model.partial.scope.TaskScopeLinkData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a TaskScopeLink that has already been stored in the database
  * @param id id of this TaskScopeLink in the database
  * @param data Wrapped TaskScopeLink data
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class TaskScopeLink(id: Int, data: TaskScopeLinkData) extends StoredModelConvertible[TaskScopeLinkData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this TaskScopeLink in the database
	  */
	def access = DbSingleTaskScopeLink(id)
}

