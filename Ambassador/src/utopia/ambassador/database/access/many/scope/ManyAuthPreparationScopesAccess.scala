package utopia.ambassador.database.access.many.scope

import utopia.ambassador.database.access.many.scope.ManyAuthPreparationScopesAccess.SubAccess
import utopia.ambassador.database.factory.scope.AuthPreparationScopeFactory
import utopia.ambassador.model.combined.scope.AuthPreparationScope
import utopia.vault.nosql.access.many.model.{ManyModelAccess, ManyRowModelAccess}
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyAuthPreparationScopesAccess
{
	private class SubAccess(override val parent: ManyModelAccess[AuthPreparationScope],
	                        override val filterCondition: Condition)
		extends ManyAuthPreparationScopesAccess with SubView
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
	// IMPLEMENTED  ----------------------------
	
	override protected def _filter(condition: Condition): ManyAuthPreparationScopesAccess =
		new SubAccess(this, condition)
	override def factory = AuthPreparationScopeFactory
}
