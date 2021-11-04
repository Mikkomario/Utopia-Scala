package utopia.ambassador.database.access.single.scope

import utopia.ambassador.model.stored.scope.TaskScopeLink
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual TaskScopeLinks, based on their id
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class DbSingleTaskScopeLink(id: Int) 
	extends UniqueTaskScopeLinkAccess with SingleIntIdModelAccess[TaskScopeLink]

