package utopia.ambassador.database.access.many.scope

import utopia.ambassador.database.access.many.description.DbScopeDescriptions
import utopia.ambassador.database.factory.scope.ScopeFactory
import utopia.ambassador.model.combined.scope.DescribedScope
import utopia.ambassador.model.stored.scope.Scope
import utopia.citadel.database.access.many.description.ManyDescribedAccess
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyScopesAccess
{
	// NESTED	--------------------
	
	private class ManyScopesSubView(override val parent: ManyRowModelAccess[Scope], 
		override val filterCondition: Condition) 
		extends ManyScopesAccess with SubView
}

/**
  * A common trait for access points which target multiple Scopes at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
trait ManyScopesAccess
	extends ManyScopesAccessLike[Scope, ManyScopesAccess] with ManyRowModelAccess[Scope]
		with ManyDescribedAccess[Scope, DescribedScope]
{
	// IMPLEMENTED	--------------------
	
	override def factory = ScopeFactory
	
	override protected def defaultOrdering = None
	
	override protected def describedFactory = DescribedScope
	
	override protected def manyDescriptionsAccess = DbScopeDescriptions
	
	override def _filter(additionalCondition: Condition): ManyScopesAccess =
		new ManyScopesAccess.ManyScopesSubView(this, additionalCondition)
	
	override def idOf(item: Scope) = item.id
}

