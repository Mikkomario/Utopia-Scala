package utopia.ambassador.database.access.many.scope

import utopia.ambassador.database.access.many.scope.ManyAuthTokenScopesAccess.SubAccess
import utopia.ambassador.database.factory.scope.AuthTokenScopeFactory
import utopia.ambassador.model.combined.scope.AuthTokenScope
import utopia.vault.nosql.access.many.model.{ManyModelAccess, ManyRowModelAccess}
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyAuthTokenScopesAccess
{
	private class SubAccess(override val parent: ManyModelAccess[AuthTokenScope],
	                        override val filterCondition: Condition)
		extends ManyAuthTokenScopesAccess with SubView
}

/**
  * Used for accessing multiple authetication token -linked scopes at a time
  * @author Mikko Hilpinen
  * @since 27.10.2021, v2.0
  */
trait ManyAuthTokenScopesAccess
	extends ManyScopesAccessLike[AuthTokenScope, ManyAuthTokenScopesAccess] with ManyRowModelAccess[AuthTokenScope]
{
	override def factory = AuthTokenScopeFactory
	override protected def defaultOrdering = None
	
	override protected def _filter(condition: Condition): ManyAuthTokenScopesAccess =
		new SubAccess(this, condition)
}
