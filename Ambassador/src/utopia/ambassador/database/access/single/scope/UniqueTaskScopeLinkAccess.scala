package utopia.ambassador.database.access.single.scope

import java.time.Instant
import utopia.ambassador.database.factory.scope.TaskScopeLinkFactory
import utopia.ambassador.database.model.scope.TaskScopeLinkModel
import utopia.ambassador.model.stored.scope.TaskScopeLink
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct TaskScopeLinks.
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
trait UniqueTaskScopeLinkAccess 
	extends SingleRowModelAccess[TaskScopeLink] 
		with DistinctModelAccess[TaskScopeLink, Option[TaskScopeLink], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the linked task. None if no instance (or value) was found.
	  */
	def taskId(implicit connection: Connection) = pullColumn(model.taskIdColumn).int
	
	/**
	  * Id of the scope required to perform the task. None if no instance (or value) was found.
	  */
	def scopeId(implicit connection: Connection) = pullColumn(model.scopeIdColumn).int
	
	/**
	  * True whether this scope is always required to perform the linked task. False whether this scope can be replaced 
		with another optional scope.. None if no instance (or value) was found.
	  */
	def isRequired(implicit connection: Connection) = pullColumn(model.isRequiredColumn).boolean
	
	/**
	  * Time when this TaskScopeLink was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = TaskScopeLinkModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = TaskScopeLinkFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted TaskScopeLink instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any TaskScopeLink instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the isRequired of the targeted TaskScopeLink instance(s)
	  * @param newIsRequired A new isRequired to assign
	  * @return Whether any TaskScopeLink instance was affected
	  */
	def isRequired_=(newIsRequired: Boolean)(implicit connection: Connection) = 
		putColumn(model.isRequiredColumn, newIsRequired)
	
	/**
	  * Updates the scopeId of the targeted TaskScopeLink instance(s)
	  * @param newScopeId A new scopeId to assign
	  * @return Whether any TaskScopeLink instance was affected
	  */
	def scopeId_=(newScopeId: Int)(implicit connection: Connection) = putColumn(model.scopeIdColumn, 
		newScopeId)
	
	/**
	  * Updates the taskId of the targeted TaskScopeLink instance(s)
	  * @param newTaskId A new taskId to assign
	  * @return Whether any TaskScopeLink instance was affected
	  */
	def taskId_=(newTaskId: Int)(implicit connection: Connection) = putColumn(model.taskIdColumn, newTaskId)
}

