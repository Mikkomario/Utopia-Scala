package utopia.ambassador.database.access.many.scope

import utopia.ambassador.database.factory.scope.{ScopeFactory, TaskScopeFactory}
import utopia.ambassador.database.model.scope.{ScopeModel, TaskScopeLinkModel}
import utopia.ambassador.database.model.token.TokenScopeLinkModel
import utopia.ambassador.model.combined.scope.TaskScope
import utopia.ambassador.model.stored.scope.Scope
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.{Select, Where}
import utopia.vault.sql.SqlExtensions._

/**
  * Used for accessing multiple scopes at a time
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
object DbScopes extends ManyRowModelAccess[Scope]
{
	// COMPUTED ------------------------------------
	
	private def model = ScopeModel
	
	
	// IMPLEMENTED  --------------------------------
	
	override def factory = ScopeFactory
	
	override protected def defaultOrdering = None
	override def globalCondition = None
	
	
	// OTHER    ------------------------------------
	
	/**
	  * @param serviceId Id of the targeted service
	  * @return An access point to that service's specified scopes
	  */
	def forServiceWithId(serviceId: Int) = new DbServiceScopes(serviceId)
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
	
	class DbServiceScopes(val serviceId: Int) extends ManyRowModelAccess[Scope] with SubView
	{
		// IMPLEMENTED  ----------------------------
		
		override protected def parent = DbScopes
		override protected def defaultOrdering = parent.defaultOrdering
		override def factory = parent.factory
		
		override def filterCondition = model.withServiceId(serviceId).toCondition
		
		
		// OTHER    ---------------------------------
		
		/**
		  * @param scopeNames A set of service side scope names
		  * @param connection Implicit DB connection
		  * @return scopes within this service that match the specified names
		  */
		def matchingAnyOf(scopeNames: Iterable[String])(implicit connection: Connection) =
			find(model.serviceSideNameColumn.in(scopeNames))
	}
	
	class DbSingleTaskScopes(val taskId: Int) extends ManyRowModelAccess[TaskScope]
	{
		// COMPUTED --------------------------------
		
		private def linkModel = TaskScopeLinkModel
		
		
		// OTHER    --------------------------------
		
		/**
		  * @param serviceId Id of the targeted service
		  * @param connection Implicit DB Connection
		  * @return All scopes linked with this task that belong to the specified service
		  */
		def forServiceWithId(serviceId: Int)(implicit connection: Connection) =
			find(model.withServiceId(serviceId).toCondition)
		
		
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
