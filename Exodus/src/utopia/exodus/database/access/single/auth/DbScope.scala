package utopia.exodus.database.access.single.auth

import utopia.exodus.database.factory.auth.ScopeFactory
import utopia.exodus.database.model.auth.ScopeModel
import utopia.exodus.model.stored.auth.Scope
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual scopes
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
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
	  * @param id Database id of the targeted scope
	  * @return An access point to that scope
	  */
	def apply(id: Int) = DbSingleScope(id)
}

