package utopia.ambassador.database.access.many.scope

import utopia.ambassador.database.access.many.scope.ManyTaskScopesAccess.SubAccess
import utopia.ambassador.database.factory.scope.TaskScopeFactory
import utopia.ambassador.model.combined.scope.TaskScope
import utopia.vault.nosql.access.many.model.{ManyModelAccess, ManyRowModelAccess}
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyTaskScopesAccess
{
	private class SubAccess(override val parent: ManyModelAccess[TaskScope], override val filterCondition: Condition)
		extends ManyTaskScopesAccess with SubView
}

/**
  * A common trait for access points that return multiple task scopes at a time
  * @author Mikko Hilpinen
  * @since 27.10.2021, v2.0
  */
trait ManyTaskScopesAccess
	extends ManyScopesAccessLike[TaskScope, ManyTaskScopesAccess] with ManyRowModelAccess[TaskScope]
{
	override def factory = TaskScopeFactory
	override protected def defaultOrdering = None
	
	override protected def _filter(condition: Condition): ManyTaskScopesAccess = new SubAccess(this, condition)
}
