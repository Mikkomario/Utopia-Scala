package utopia.ambassador.database.factory.scope

import utopia.ambassador.model.combined.scope.TaskScope
import utopia.ambassador.model.stored.scope.{Scope, TaskScopeLink}
import utopia.vault.nosql.factory.row.linked.CombiningFactory

/**
  * Used for reading TaskScopes from the database
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object TaskScopeFactory extends CombiningFactory[TaskScope, Scope, TaskScopeLink]
{
	// IMPLEMENTED	--------------------
	
	override def childFactory = TaskScopeLinkFactory
	
	override def parentFactory = ScopeFactory
	
	override def apply(scope: Scope, taskLink: TaskScopeLink) = TaskScope(scope, taskLink)
}

