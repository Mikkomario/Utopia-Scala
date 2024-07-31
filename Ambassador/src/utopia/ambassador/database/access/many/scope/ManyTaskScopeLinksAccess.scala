package utopia.ambassador.database.access.many.scope

import utopia.ambassador.database.factory.scope.TaskScopeLinkFactory
import utopia.ambassador.database.model.scope.TaskScopeLinkModel
import utopia.ambassador.model.stored.scope.TaskScopeLink
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, ViewFactory}
import utopia.vault.sql.Condition

import java.time.Instant

object ManyTaskScopeLinksAccess extends ViewFactory[ManyTaskScopeLinksAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyTaskScopeLinksAccess = 
		 new _ManyTaskScopeLinksAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyTaskScopeLinksAccess(condition: Condition) extends ManyTaskScopeLinksAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple TaskScopeLinks at a time
  * @author Mikko Hilpinen
  * @since 26.10.2021
  */
trait ManyTaskScopeLinksAccess 
	extends ManyRowModelAccess[TaskScopeLink] with Indexed with FilterableView[ManyTaskScopeLinksAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * taskIds of the accessible TaskScopeLinks
	  */
	def taskIds(implicit connection: Connection) = pullColumn(model.taskIdColumn)
		.flatMap { value => value.int }
	
	/**
	  * scopeIds of the accessible TaskScopeLinks
	  */
	def scopeIds(implicit connection: Connection) = pullColumn(model.scopeIdColumn)
		.flatMap { value => value.int }
	
	/**
	  * areRequired of the accessible TaskScopeLinks
	  */
	def areRequired(implicit connection: Connection) = 
		pullColumn(model.isRequiredColumn).flatMap { value => value.boolean }
	
	/**
	  * creationTimes of the accessible TaskScopeLinks
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * A copy of this access point with scope data included
	  */
	def withScopes = {
		accessCondition match
		{
			case Some(c) => DbTaskScopes.filter(c)
			case None => DbTaskScopes
		}
	}
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = TaskScopeLinkModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = TaskScopeLinkFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyTaskScopeLinksAccess = ManyTaskScopeLinksAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the isRequired of the targeted TaskScopeLink instance(s)
	  * @param newIsRequired A new isRequired to assign
	  * @return Whether any TaskScopeLink instance was affected
	  */
	def areRequired_=(newIsRequired: Boolean)(implicit connection: Connection) = 
		putColumn(model.isRequiredColumn, newIsRequired)
	
	/**
	  * Updates the created of the targeted TaskScopeLink instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any TaskScopeLink instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * @param scopeIds Ids of the targeted scopes
	  * @return An access point to links connected to any of those scopes
	  */
	def forAnyOfScopes(scopeIds: Iterable[Int]) = filter(model.scopeIdColumn in scopeIds)
	
	/**
	  * @param taskIds Ids of the targeted tasks
	  * @return An access point to links connected to any of those tasks
	  */
	def forAnyOfTasks(taskIds: Iterable[Int]) = filter(model.taskIdColumn in taskIds)
	
	/**
	  * Updates the scopeId of the targeted TaskScopeLink instance(s)
	  * @param newScopeId A new scopeId to assign
	  * @return Whether any TaskScopeLink instance was affected
	  */
	def scopeIds_=(newScopeId: Int)(implicit connection: Connection) = putColumn(model.scopeIdColumn,
		newScopeId)
	
	/**
	  * Updates the taskId of the targeted TaskScopeLink instance(s)
	  * @param newTaskId A new taskId to assign
	  * @return Whether any TaskScopeLink instance was affected
	  */
	def taskIds_=(newTaskId: Int)(implicit connection: Connection) = putColumn(model.taskIdColumn, newTaskId)
	
	/**
	  * @param taskId Id of the linked task
	  * @return An access point to scope links connected to that task
	  */
	def withTaskId(taskId: Int) = filter(model.withTaskId(taskId).toCondition)
}

