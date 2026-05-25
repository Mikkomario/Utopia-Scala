package utopia.vigil.database.store

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.operator.Identity
import utopia.vault.database.{Connection, Store}
import utopia.vault.store.StoreResult
import utopia.vigil.database.access.scope.relation.AccessScopeRelations
import utopia.vigil.database.access.scope.{AccessScope, AccessScopes}
import utopia.vigil.database.storable.scope.{ScopeDbModel, ScopeRelationDbModel}
import utopia.vigil.model.combined.scope.ChildScope
import utopia.vigil.model.partial.scope.{ScopeData, ScopeRelationData}

/**
 * Used for interacting with scope information in the DB
 * @author Mikko Hilpinen
 * @since 03.05.2026, v0.1
 */
object ScopeDb
{
	// ATTRIBUTES   -----------------------
	
	private val _store = Store(ScopeDbModel) { key: String => ScopeData(key = key) }
	private val _storeLinks = Store(ScopeRelationDbModel) { link: Pair[Int] =>
		ScopeRelationData(parentScopeId = link.first, grantedScopeId = link.second)
	}
	
	
	// OTHER    ---------------------------
	
	/**
	 * Stores an individual scope entry to the DB
	 * @param scope Scope key to store
	 * @param connection Implicit DB connection
	 * @return Scope-storing result
	 */
	def store(scope: String)(implicit connection: Connection) =
		_store.single(scope, AccessScope.forKey(scope).pull)
	/**
	 * Stores a scope graph to the database
	 * @param scopeLinks Scope relations to store,
	 *                   where first values are parent scope keys and second values are granted scope keys
	 * @param unrelated Scope keys that are not related with any other scope (default = empty)
	 * @param exclusive Whether this is an exclusive data-set (other scopes and relations will be deleted)
	 * @param connection Implicit DB connection
	 */
	def store(scopeLinks: Iterable[Pair[String]], unrelated: Iterable[String] = Empty, exclusive: Boolean = false)
	         (implicit connection: Connection) =
	{
		// Stores the referenced scopes
		val existingScopes = AccessScopes.pull
		val storeMap = _store.keyMap((scopeLinks.iterator.flatten ++ unrelated).distinct, existingScopes) {
			_.toLowerCase } { _.key.toLowerCase }
		
		// Deletes other scopes, if appropriate
		if (exclusive) {
			val validScopeIds = storeMap.valuesIterator.map { _.id }.toSet
			AccessScopes(existingScopes.iterator.map { _.id }.filterNot(validScopeIds.contains)).delete()
		}
		
		// Stores scope relations
		val existingScopeRelations = {
			if (exclusive)
				AccessScopeRelations.pull
			else
				AccessScopeRelations.ofScopes(storeMap.valuesIterator.filter { _.existed }.map { _.id }).pull
		}
		val storedLinks = _storeLinks.keyMap(scopeLinks.iterator.map { _.map { key => storeMap(key.toLowerCase).id } },
			existingScopeRelations)(Identity) { r => Pair(r.parentScopeId, r.grantedScopeId) }
		
		// Deletes other relations, if appropriate
		if (exclusive) {
			val validRelationIds = storedLinks.valuesIterator.map { _.id }.toSet
			AccessScopeRelations(existingScopeRelations.iterator.map { _.id }.filterNot(validRelationIds.contains))
				.delete()
		}
	}
	
	/**
	 * Sets / overwrites the scopes granted by a specific scope
	 * @param parentScopeId ID of the scope that grants other scopes
	 * @param granted Granted scope keys
	 * @param connection Implicit DB connection
	 * @return Returns 2 values:
	 *         1. Stored child scopes
	 *         1. Deleted scope relation links
	 */
	//noinspection ConvertibleToMethodValue
	def setGrantsOf(parentScopeId: Int, granted: Set[String])(implicit connection: Connection) = {
		// Stores the referenced scopes
		val storeMap = _store.keyMap(granted, AccessScopes.forKeys(granted).pull) {
			_.toLowerCase } { _.key.toLowerCase }
		val scopeById = storeMap.valuesIterator.map { s => s.id -> s }.toMap
		val grantedScopeIds = scopeById.keySet
		
		// Checks for existing grants
		val (linksToRemove, linksToKeep) = AccessScopeRelations.withParent(parentScopeId).pull
			.divideBy { link => grantedScopeIds.contains(link.grantedScopeId) }.toTuple
		val existingGrantedIds = linksToKeep.iterator.map { _.grantedScopeId }.toSet
		
		// Deletes grants that are no longer present
		if (linksToRemove.nonEmpty)
			AccessScopeRelations(linksToRemove.iterator.map { _.id }).delete()
		
		// Inserts missing grants
		val inserted = ScopeRelationDbModel.insert(
			(grantedScopeIds -- existingGrantedIds).iterator
				.map { grantedId => ScopeRelationData(parentScopeId = parentScopeId, grantedScopeId = grantedId) }
				.toOptimizedSeq)
		
		(linksToKeep.iterator.map { StoreResult.existed(_) } ++ inserted.iterator.map { StoreResult.inserted(_) })
			.map { link => link.map { link => ChildScope(scopeById(link.grantedScopeId), link) } }
			.toOptimizedSeq -> linksToRemove
	}
	
	/**
	 * Deletes specific scope grants
	 * @param parentScopeId ID of the scope from which grants are removed
	 * @param deletedGrants Granted scope keys to remove
	 * @param connection Implicit DB connection
	 */
	def deleteGrantsOf(parentScopeId: Int, deletedGrants: Set[String])(implicit connection: Connection) =
		AccessScopeRelations.withParent(parentScopeId).whereGrantedScopes.forKeys(deletedGrants).delete()
	/**
	 * Deletes all grants of a specific scope
	 * @param parentScopeId ID of the scope from which grants are removed
	 * @param connection Implicit DB connection
	 */
	def deleteGrantsOf(parentScopeId: Int)(implicit connection: Connection) =
		AccessScopeRelations.withParent(parentScopeId).delete()
}
