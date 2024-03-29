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
	// COMPUTED ------------------------
	
	/**
	  * @return A copy of this access point which includes authentication preparation linking
	  */
	def withAuthPreparationLinks = accessCondition match
	{
		case Some(c) => DbAuthPreparationScopes.filter(c)
		case None=> DbAuthPreparationScopes
	}
	/**
	  * @return A copy of this access point which includes task linking
	  */
	def withTaskLinks = accessCondition match
	{
		case Some(c) => DbTaskScopes.filter(c)
		case None => DbTaskScopes
	}
	/**
	  * @return A copy of this access point which includes authentication token linking
	  */
	def withTokenLinks = accessCondition match
	{
		case Some(c) => DbAuthTokenScopes.filter(c)
		case None => DbAuthTokenScopes
	}
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override def factory = ScopeFactory
	
	override protected def describedFactory = DescribedScope
	
	override protected def manyDescriptionsAccess = DbScopeDescriptions
	
	override def _filter(additionalCondition: Condition): ManyScopesAccess =
		new ManyScopesAccess.ManyScopesSubView(this, additionalCondition)
	
	override def idOf(item: Scope) = item.id
}

