package utopia.ambassador.database.access.single.scope

import utopia.ambassador.database.factory.scope.ScopeFactory
import utopia.ambassador.database.model.scope.ScopeModel
import utopia.ambassador.model.stored.scope.Scope
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual Scopes
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object DbScope extends SingleRowModelAccess[Scope] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ScopeModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ScopeFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted Scope instance
	  * @return An access point to that Scope
	  */
	def apply(id: Int) = DbSingleScope(id)
}

