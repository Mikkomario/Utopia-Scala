package utopia.ambassador.database.access.single.scope

import utopia.ambassador.database.factory.scope.TaskScopeLinkFactory
import utopia.ambassador.database.model.scope.TaskScopeLinkModel
import utopia.ambassador.model.stored.scope.TaskScopeLink
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual TaskScopeLinks
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object DbTaskScopeLink extends SingleRowModelAccess[TaskScopeLink] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = TaskScopeLinkModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = TaskScopeLinkFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted TaskScopeLink instance
	  * @return An access point to that TaskScopeLink
	  */
	def apply(id: Int) = DbSingleTaskScopeLink(id)
}

