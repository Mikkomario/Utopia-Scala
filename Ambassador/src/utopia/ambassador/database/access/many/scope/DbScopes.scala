package utopia.ambassador.database.access.many.scope

import utopia.ambassador.database.factory.scope.{ScopeFactory, TaskScopeFactory}
import utopia.ambassador.database.model.scope.TaskScopeLinkModel
import utopia.ambassador.database.model.token.TokenScopeLinkModel
import utopia.ambassador.model.combined.scope.TaskScope
import utopia.ambassador.model.stored.scope.Scope
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.sql.{Select, Where}

/**
  * Used for accessing multiple scopes at a time
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
object DbScopes extends ManyRowModelAccess[Scope]
{
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
	
	/**
	  * @param tokenId A token id
	  * @return An access point to that token's scopes
	  */
	def forTokenWithId(tokenId: Int) = new DbSingleTokenScopes(tokenId)
	
	
	// NESTED   ------------------------------------
	
	class DbSingleTaskScopes(val taskId: Int) extends ManyRowModelAccess[TaskScope]
	{
		// COMPUTED --------------------------------
		
		private def linkModel = TaskScopeLinkModel
		
		
		// IMPLEMENTED  ----------------------------
		
		override def factory = TaskScopeFactory
		
		override protected def defaultOrdering = None
		override def globalCondition =
			Some(linkModel.withTaskId(taskId).toCondition && factory.nonDeprecatedCondition)
	}
	
	class DbSingleTokenScopes(val tokenId: Int)
	{
		// COMPUTED --------------------------------
		
		private def linkModel = TokenScopeLinkModel
		
		private def target = factory.target join linkModel.table
		private def condition = linkModel.withTokenId(tokenId)
		
		/**
		  * @param connection Implicit db connection
		  * @return Ids of all the scopes of this token
		  */
		def ids(implicit connection: Connection) =
			connection(Select(target, factory.index) + Where(condition)).rowIntValues
	}
}
