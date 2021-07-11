package utopia.ambassador.database.access.many.scope

import utopia.ambassador.database.factory.scope.{ScopeFactory, TaskScopeFactory}
import utopia.ambassador.database.model.scope.TaskScopeLinkModel
import utopia.ambassador.model.combined.scope.TaskScope
import utopia.ambassador.model.stored.scope.Scope
import utopia.vault.nosql.access.ManyRowModelAccess

/**
  * Used for accessing multiple scopes at a time
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
object DbScopes extends ManyRowModelAccess[Scope]
{
	// COMPUTED ------------------------------------
	
	// private def model = ScopeModel
	private def linkModel = TaskScopeLinkModel
	
	
	// IMPLEMENTED  --------------------------------
	
	override def factory = ScopeFactory
	
	override protected def defaultOrdering = None
	override def globalCondition = None
	
	
	// OTHER    ------------------------------------
	
	/**
	  * @param taskId A task id
	  * @return An access point to that task's scopes
	  */
	def forTaskWithId(taskId: Int) = new DbSingleTaskScopes(taskId)
	
	
	// NESTED   ------------------------------------
	
	class DbSingleTaskScopes(val taskId: Int) extends ManyRowModelAccess[TaskScope]
	{
		// IMPLEMENTED  ----------------------------
		
		override def factory = TaskScopeFactory
		
		override protected def defaultOrdering = None
		override def globalCondition =
			Some(linkModel.withTaskId(taskId).toCondition && factory.nonDeprecatedCondition)
	}
}
