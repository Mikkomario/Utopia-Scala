package utopia.ambassador.model.combined.scope

import utopia.ambassador.model.stored.scope.{Scope, TaskScopeLink}
import utopia.flow.util.Extender

/**
  * Appends scope data to task-scope-link
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
case class TaskScope(link: TaskScopeLink, scope: Scope) extends Extender[Scope]
{
	// COMPUTED -------------------------------
	
	/**
	  * @return Whether this scope is required for the task at hand (false if one of possible scopes)
	  */
	def isRequired = link.isRequired
	
	
	// IMPLEMENTED  ---------------------------
	
	override def wrapped = scope
}
