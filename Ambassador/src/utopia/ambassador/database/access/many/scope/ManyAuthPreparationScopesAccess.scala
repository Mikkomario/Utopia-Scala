package utopia.ambassador.database.access.many.scope

import utopia.ambassador.database.factory.scope.AuthPreparationScopeFactory
import utopia.ambassador.model.combined.scope.AuthPreparationScope
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

object ManyAuthPreparationScopesAccess extends ViewFactory[ManyAuthPreparationScopesAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyAuthPreparationScopesAccess = 
		new _ManyAuthPreparationScopesAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyAuthPreparationScopesAccess(condition: Condition)
		 extends ManyAuthPreparationScopesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * Used for accessing many authentication preparation scopes at a time
  * @author Mikko Hilpinen
  * @since 26.10.2021, v2.0
  */
trait ManyAuthPreparationScopesAccess 
	extends ManyScopesAccessLike[AuthPreparationScope, ManyAuthPreparationScopesAccess] 
		with ManyRowModelAccess[AuthPreparationScope]
{
	// IMPLEMENTED	--------------------
	
	override def factory = AuthPreparationScopeFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyAuthPreparationScopesAccess = 
		ManyAuthPreparationScopesAccess(condition)
}

