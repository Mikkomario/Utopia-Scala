package utopia.ambassador.database.model.scope

import utopia.ambassador.database.factory.scope.TaskScopeLinkFactory
import utopia.ambassador.model.partial.scope.TaskScopeLinkData
import utopia.ambassador.model.stored.scope.TaskScopeLink
import utopia.citadel.database.model.DeprecatableAfter
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.model.template.DataInserter

import java.time.Instant

object TaskScopeLinkModel extends DataInserter[TaskScopeLinkModel, TaskScopeLink, TaskScopeLinkData]
	with DeprecatableAfter[TaskScopeLinkModel]
{
	// ATTRIBUTES   -------------------------
	
	/**
	  * Name of the property that refers to a task
	  */
	val taskIdAttName = "taskId"
	
	
	// COMPUTED -----------------------------
	
	/**
	  * @return The factory used by this model
	  */
	def factory = TaskScopeLinkFactory
	
	/**
	  * Column that refers to a task
	  */
	def taskIdColumn = table(taskIdAttName)
	
	
	// IMPLEMENTED  -------------------------
	
	override def table = factory.table
	
	override def withDeprecatedAfter(deprecation: Instant) =
		apply(deprecatedAfter = Some(deprecation))
	
	override def apply(data: TaskScopeLinkData) =
		apply(None, Some(data.taskId), Some(data.scopeId), Some(data.isRequired), Some(data.created),
			data.deprecatedAfter)
	
	override protected def complete(id: Value, data: TaskScopeLinkData) = TaskScopeLink(id.getInt, data)
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param taskId Id of the linked task
	  * @return A model with that task id
	  */
	def withTaskId(taskId: Int) = apply(taskId = Some(taskId))
}

/**
  * Used for interacting with task-scope-links in the DB
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
case class TaskScopeLinkModel(id: Option[Int] = None, taskId: Option[Int] = None, scopeId: Option[Int] = None,
                              isRequired: Option[Boolean] = None, created: Option[Instant] = None,
                              deprecatedAfter: Option[Instant] = None) extends StorableWithFactory[TaskScopeLink]
{
	import TaskScopeLinkModel._
	
	override def factory = TaskScopeLinkModel.factory
	
	override def valueProperties = Vector("id" -> id, taskIdAttName -> taskId, "scopeId" -> scopeId,
		"isRequired" -> isRequired, "created" -> created, deprecationAttName -> deprecatedAfter)
}
