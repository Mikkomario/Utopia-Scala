package utopia.ambassador.database.access.many.scope

import utopia.ambassador.model.combined.scope.DescribedScope
import utopia.ambassador.model.stored.scope.Scope
import utopia.citadel.database.access.many.description.ManyDescribedAccessByIds
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple Scopes at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object DbScopes extends ManyScopesAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted Scopes
	  * @return An access point to Scopes with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbScopesSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbScopesSubset(override val ids: Set[Int]) 
		extends ManyScopesAccess with ManyDescribedAccessByIds[Scope, DescribedScope]
}

