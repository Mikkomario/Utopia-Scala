package utopia.ambassador.database.access.many.scope

import utopia.ambassador.database.factory.scope.TaskScopeFactory
import utopia.ambassador.model.combined.scope.TaskScope
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

object ManyTaskScopesAccess extends ViewFactory[ManyTaskScopesAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyTaskScopesAccess = new _ManyTaskScopesAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyTaskScopesAccess(condition: Condition) extends ManyTaskScopesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points that return multiple task scopes at a time
  * @author Mikko Hilpinen
  * @since 27.10.2021, v2.0
  */
trait ManyTaskScopesAccess 
	extends ManyScopesAccessLike[TaskScope, ManyTaskScopesAccess] with ManyRowModelAccess[TaskScope]
{
	// IMPLEMENTED	--------------------
	
	override def factory = TaskScopeFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyTaskScopesAccess = ManyTaskScopesAccess(condition)
}

