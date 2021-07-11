package utopia.ambassador.database.factory.scope

import utopia.ambassador.model.combined.scope.TaskScope
import utopia.ambassador.model.stored.scope.{Scope, TaskScopeLink}
import utopia.vault.nosql.factory.{CombiningFactory, Deprecatable}

/**
  * Used for accessing a task's scope information
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
object TaskScopeFactory extends CombiningFactory[TaskScope, TaskScopeLink, Scope] with Deprecatable
{
	// IMPLEMENTED  ---------------------------------
	
	override def parentFactory = TaskScopeLinkFactory
	
	override def childFactory = ScopeFactory
	
	override def nonDeprecatedCondition = parentFactory.nonDeprecatedCondition
	
	override def apply(parent: TaskScopeLink, child: Scope) = TaskScope(parent, child)
}
