package utopia.vigil.database.store

import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Tree}
import utopia.vault.database.{Connection, Store}
import utopia.vault.store.StoreResult
import utopia.vigil.database.access.scope.{AccessScope, AccessScopes}
import utopia.vigil.database.storable.scope.ScopeDbModel
import utopia.vigil.model.cached.scope.ScopeTarget
import utopia.vigil.model.partial.scope.ScopeData
import utopia.vigil.model.stored.scope.Scope

/**
 * Used for interacting with scope information in the DB
 * @author Mikko Hilpinen
 * @since 03.05.2026, v0.1
 */
object ScopeDb
{
	// ATTRIBUTES   -----------------------
	
	private val _store = Store
		.apply(ScopeDbModel) { data: (String, Option[Int]) => ScopeData(key = data._1, parentId = data._2) }
		// May update the parent ID
		.updating { (input, existing, connection) =>
			if (existing.parentId != input._2)
				Some(connection.use { implicit c =>
					val prop = existing.access.parentId
					input._2 match {
						case Some(newParentId) =>
							prop.set(newParentId)
							existing.withParentId(newParentId)
							
						case None =>
							prop.clear()
							existing.withoutParent
					}
				})
			else
				None
		}
	
	
	// OTHER    ---------------------------
	
	/**
	 * Stores an individual scope entry to the DB
	 * @param scope Scope key to store
	 * @param parent Scope that should directly contain this scope (optional)
	 * @param connection Implicit DB connection
	 * @return Scope-storing result
	 */
	def store(scope: String, parent: Option[ScopeTarget] = None)(implicit connection: Connection) =
		_store.single(scope -> parent.filter { _.isValid }.map { _.id }, AccessScope.forKey(scope).pull)
	/**
	 * Stores scope trees to the database
	 * @param scopeTrees Scope trees to store, where navs are scope keys.
	 * @param connection Implicit DB connection
	 * @return Scope store results
	 */
	def store(scopeTrees: Iterable[Tree[String]])(implicit connection: Connection) = {
		if (scopeTrees.nonEmpty) {
			// Pulls the existing scope info in order to avoid duplicates
			val existing = AccessScopes.pull
			val resultBuilder = OptimizedIndexedSeq.newBuilder[StoreResult[Scope]]
			
			// Stores one scope layer at a time
			var nextScopes = scopeTrees.map[(Tree[String], Option[Int])] { _ -> None }
			while (nextScopes.nonEmpty) {
				val stored = _store.keyMap(nextScopes.map { case (node, parentId) => node.nav -> parentId }, existing) {
					_._1.toLowerCase } { _.key.toLowerCase }
				
				resultBuilder ++= stored.valuesIterator
				nextScopes = nextScopes.flatMap { case (node, _) =>
					val parentId = stored(node.nav.toLowerCase).id
					node.children.map { _ -> Some(parentId) }
				}
			}
			
			resultBuilder.result()
		}
		// Case: Nothing to store
		else
			Empty
	}
}
