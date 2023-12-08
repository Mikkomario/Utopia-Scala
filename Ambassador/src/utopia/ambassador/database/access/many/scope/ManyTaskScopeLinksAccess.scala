package utopia.ambassador.database.access.many.scope

import java.time.Instant
import utopia.ambassador.database.factory.scope.TaskScopeLinkFactory
import utopia.ambassador.database.model.scope.TaskScopeLinkModel
import utopia.ambassador.model.stored.scope.TaskScopeLink
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, SubView}
import utopia.vault.sql.Condition

object ManyTaskScopeLinksAccess
{
	// NESTED	--------------------
	
	private class ManyTaskScopeLinksSubView(override val parent: ManyRowModelAccess[TaskScopeLink], 
		override val filterCondition: Condition) 
		extends ManyTaskScopeLinksAccess with SubView
}

/**
  * A common trait for access points which target multiple TaskScopeLinks at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
trait ManyTaskScopeLinksAccess extends ManyRowModelAccess[TaskScopeLink] with Indexed
	with FilterableView[ManyTaskScopeLinksAccess]
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
	  * Factory used for constructing database the interaction models
	  */
	protected def model = TaskScopeLinkModel
	
	/**
	  * @return A copy of this access point with scope data included
	  */
	def withScopes = accessCondition match
	{
		case Some(c) => DbTaskScopes.filter(c)
		case None => DbTaskScopes
	}
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override def factory = TaskScopeLinkFactory
	
	override def filter(additionalCondition: Condition): ManyTaskScopeLinksAccess = 
		new ManyTaskScopeLinksAccess.ManyTaskScopeLinksSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * @param taskId Id of the linked task
	  * @return An access point to scope links connected to that task
	  */
	def withTaskId(taskId: Int) = filter(model.withTaskId(taskId).toCondition)
	
	/**
	  * @param taskIds Ids of the targeted tasks
	  * @return An access point to links connected to any of those tasks
	  */
	def forAnyOfTasks(taskIds: Iterable[Int]) = filter(model.taskIdColumn in taskIds)
	/**
	  * @param scopeIds Ids of the targeted scopes
	  * @return An access point to links connected to any of those scopes
	  */
	def forAnyOfScopes(scopeIds: Iterable[Int]) = filter(model.scopeIdColumn in scopeIds)
	
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
}

