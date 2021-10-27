package utopia.ambassador.model.combined.scope

import utopia.ambassador.model.partial.scope.ScopeData
import utopia.ambassador.model.stored.scope.{Scope, TaskScopeLink}
import utopia.flow.util.Extender

/**
  * Combines Scope with taskLink data
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class TaskScope(scope: Scope, taskLink: TaskScopeLink) extends Extender[ScopeData]
{
	// COMPUTED	--------------------
	
	/**
	  * Id of this Scope in the database
	  */
	def id = scope.id
	
	/**
	  * @return Whether this scope is required for the task at hand (false if one of possible scopes)
	  */
	def isRequired = taskLink.isRequired
	/**
	  * @return Whether this scope is optional for the task at hand (meaning, it's one of possible scopes)
	  */
	def isOptional = !isRequired
	
	
	// IMPLEMENTED	--------------------
	
	override def wrapped = scope.data
}

