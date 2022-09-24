package utopia.ambassador.model.partial.scope

import java.time.Instant
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now

/**
  * Links tasks with the scopes that are required to perform them
  * @param taskId Id of the linked task
  * @param scopeId Id of the scope required to perform the task
  * @param isRequired True whether this scope is always required to perform the linked task. False whether this scope can be replaced 
	with another optional scope.
  * @param created Time when this TaskScopeLink was first created
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class TaskScopeLinkData(taskId: Int, scopeId: Int, isRequired: Boolean = false, created: Instant = Now) 
	extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("task_id" -> taskId, "scope_id" -> scopeId, "is_required" -> isRequired, 
			"created" -> created))
}

