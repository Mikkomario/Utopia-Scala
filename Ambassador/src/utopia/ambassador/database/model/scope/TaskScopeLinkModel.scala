package utopia.ambassador.database.model.scope

import java.time.Instant
import utopia.ambassador.database.factory.scope.TaskScopeLinkFactory
import utopia.ambassador.model.partial.scope.TaskScopeLinkData
import utopia.ambassador.model.stored.scope.TaskScopeLink
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing TaskScopeLinkModel instances and for inserting TaskScopeLinks to the database
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object TaskScopeLinkModel extends DataInserter[TaskScopeLinkModel, TaskScopeLink, TaskScopeLinkData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains TaskScopeLink taskId
	  */
	val taskIdAttName = "taskId"
	
	/**
	  * Name of the property that contains TaskScopeLink scopeId
	  */
	val scopeIdAttName = "scopeId"
	
	/**
	  * Name of the property that contains TaskScopeLink isRequired
	  */
	val isRequiredAttName = "isRequired"
	
	/**
	  * Name of the property that contains TaskScopeLink created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains TaskScopeLink taskId
	  */
	def taskIdColumn = table(taskIdAttName)
	
	/**
	  * Column that contains TaskScopeLink scopeId
	  */
	def scopeIdColumn = table(scopeIdAttName)
	
	/**
	  * Column that contains TaskScopeLink isRequired
	  */
	def isRequiredColumn = table(isRequiredAttName)
	
	/**
	  * Column that contains TaskScopeLink created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = TaskScopeLinkFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: TaskScopeLinkData) = 
		apply(None, Some(data.taskId), Some(data.scopeId), Some(data.isRequired), Some(data.created))
	
	override def complete(id: Value, data: TaskScopeLinkData) = TaskScopeLink(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this TaskScopeLink was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param id A TaskScopeLink id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param isRequired True whether this scope is always required to perform the linked task. False whether this scope can be replaced 
		with another optional scope.
	  * @return A model containing only the specified isRequired
	  */
	def withIsRequired(isRequired: Boolean) = apply(isRequired = Some(isRequired))
	
	/**
	  * @param scopeId Id of the scope required to perform the task
	  * @return A model containing only the specified scopeId
	  */
	def withScopeId(scopeId: Int) = apply(scopeId = Some(scopeId))
	
	/**
	  * @param taskId Id of the linked task
	  * @return A model containing only the specified taskId
	  */
	def withTaskId(taskId: Int) = apply(taskId = Some(taskId))
}

/**
  * Used for interacting with TaskScopeLinks in the database
  * @param id TaskScopeLink database id
  * @param taskId Id of the linked task
  * @param scopeId Id of the scope required to perform the task
  * @param isRequired True whether this scope is always required to perform the linked task. False whether this scope can be replaced 
	with another optional scope.
  * @param created Time when this TaskScopeLink was first created
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class TaskScopeLinkModel(id: Option[Int] = None, taskId: Option[Int] = None, 
	scopeId: Option[Int] = None, isRequired: Option[Boolean] = None, created: Option[Instant] = None) 
	extends StorableWithFactory[TaskScopeLink]
{
	// IMPLEMENTED	--------------------
	
	override def factory = TaskScopeLinkModel.factory
	
	override def valueProperties = 
	{
		import TaskScopeLinkModel._
		Vector("id" -> id, taskIdAttName -> taskId, scopeIdAttName -> scopeId, 
			isRequiredAttName -> isRequired, createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param isRequired A new isRequired
	  * @return A new copy of this model with the specified isRequired
	  */
	def withIsRequired(isRequired: Boolean) = copy(isRequired = Some(isRequired))
	
	/**
	  * @param scopeId A new scopeId
	  * @return A new copy of this model with the specified scopeId
	  */
	def withScopeId(scopeId: Int) = copy(scopeId = Some(scopeId))
	
	/**
	  * @param taskId A new taskId
	  * @return A new copy of this model with the specified taskId
	  */
	def withTaskId(taskId: Int) = copy(taskId = Some(taskId))
}

