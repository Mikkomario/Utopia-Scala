package utopia.ambassador.database.access.many.scope

import utopia.ambassador.database.factory.scope.AuthTokenScopeFactory
import utopia.ambassador.model.combined.scope.AuthTokenScope
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

object ManyAuthTokenScopesAccess extends ViewFactory[ManyAuthTokenScopesAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyAuthTokenScopesAccess = 
		new _ManyAuthTokenScopesAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyAuthTokenScopesAccess(condition: Condition) extends ManyAuthTokenScopesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * Used for accessing multiple authetication token -linked scopes at a time
  * @author Mikko Hilpinen
  * @since 27.10.2021, v2.0
  */
trait ManyAuthTokenScopesAccess 
	extends ManyScopesAccessLike[AuthTokenScope, ManyAuthTokenScopesAccess] 
		with ManyRowModelAccess[AuthTokenScope]
{
	// IMPLEMENTED	--------------------
	
	override def factory = AuthTokenScopeFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyAuthTokenScopesAccess = ManyAuthTokenScopesAccess(condition)
}

