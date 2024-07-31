package utopia.exodus.database.access.many.auth

import utopia.exodus.database.factory.auth.ScopeFactory
import utopia.exodus.model.stored.auth.Scope
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

object ManyScopesAccess extends ViewFactory[ManyScopesAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyScopesAccess = new _ManyScopesAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyScopesAccess(condition: Condition) extends ManyScopesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple scopes at a time
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
trait ManyScopesAccess extends ManyScopesAccessLike[Scope, ManyScopesAccess] with ManyRowModelAccess[Scope]
{
	// COMPUTED	--------------------
	
	/**
	  * A copy of this access point which includes token link information
	  */
	def withTokenLinks = {
		accessCondition match 
		{
			case Some(condition) => DbTokenScopes.filter(condition)
			case None => DbTokenScopes
		}
	}
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ScopeFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyScopesAccess = ManyScopesAccess(condition)
}

