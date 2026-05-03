package utopia.vigil.database.access.scope.right

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.template.Filterable
import utopia.vault.sql.Condition
import utopia.vigil.database.props.scope.ScopeRightDbProps

/**
  * Common trait for access points which may be filtered based on scope right properties
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait FilterScopeRights[+Repr] extends Filterable[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Model that defines scope right database properties
	  */
	def model: ScopeRightDbProps
	
	/**
	 * @return Copy of this access limited to scopes that are directly usable
	 */
	def usable = withDirectlyUsable(true)
	
	
	// OTHER	--------------------
	
	/**
	  * @param scopeId scope id to target
	  * @return Copy of this access point that only includes scope rights with the specified scope id
	  */
	def toScope(scopeId: Int) = filter(model.scopeId.column <=> scopeId)
	/**
	  * @param scopeIds Targeted scope ids
	  * @return Copy of this access point that only includes scope rights where scope id is within the 
	  * specified value set
	  */
	def toScopes(scopeIds: IterableOnce[Int]) = filter(Condition.indexIn(model.scopeId, scopeIds))
	
	/**
	  * @param usable usable to target
	  * @return Copy of this access point that only includes scope rights with the specified usable
	  */
	def withDirectlyUsable(usable: Boolean) = filter(model.usable.column <=> usable)
	/**
	  * @param usable Targeted usable
	  * @return Copy of this access point that only includes scope rights where usable is within the 
	  * specified value set
	  */
	def withDirectlyUsable(usable: Iterable[Boolean]) = filter(model.usable.column.in(usable))
}

