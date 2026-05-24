package utopia.vigil.database.access.scope.relation

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.template.Filterable
import utopia.vault.sql.Condition
import utopia.vigil.database.storable.scope.ScopeRelationDbModel

/**
  * Common trait for access points which may be filtered based on scope relation properties
  * @author Mikko Hilpinen
  * @since 24.05.2026, v0.1
  */
trait FilterScopeRelations[+Repr] extends Filterable[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines scope relation database properties
	  */
	def model = ScopeRelationDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param grantedScopeId granted scope id to target
	  * @return Copy of this access point that only includes scope relations with the specified granted scope id
	  */
	def granting(grantedScopeId: Int) = filter(model.grantedScopeId.column <=> grantedScopeId)
	/**
	  * @param grantedScopeIds Targeted granted scope ids
	  * @return Copy of this access point that only includes scope relations where granted scope id is within 
	  * the specified value set
	  */
	def grantingScopes(grantedScopeIds: IterableOnce[Int]) = 
		filter(Condition.indexIn(model.grantedScopeId, grantedScopeIds))
	
	/**
	  * @param parentScopeId parent scope id to target
	  * @return Copy of this access point that only includes scope relations with the specified parent scope id
	  */
	def withParent(parentScopeId: Int) = filter(model.parentScopeId.column <=> parentScopeId)
	/**
	  * @param parentScopeIds Targeted parent scope ids
	  * @return Copy of this access point that only includes scope relations where parent scope id is within 
	  * the specified value set
	  */
	def withParents(parentScopeIds: IterableOnce[Int]) = 
		filter(Condition.indexIn(model.parentScopeId, parentScopeIds))
	
	/**
	 * @param scopeIds IDs of the targeted scopes
	 * @return Access to all relations of that scope
	 */
	def ofScopes(scopeIds: IterableOnce[Int]) = {
		val idSet = scopeIds.toIntSet
		if (idSet.isEmpty)
			filter(Condition.alwaysFalse)
		else
			filter(model.parentScopeId.in(idSet) || model.grantedScopeId.in(idSet))
	}
}

