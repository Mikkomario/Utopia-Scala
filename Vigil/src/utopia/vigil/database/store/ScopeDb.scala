package utopia.vigil.database.store

import utopia.vault.database.{Connection, Store}
import utopia.vigil.database.access.scope.AccessScope
import utopia.vigil.database.storable.scope.ScopeDbModel
import utopia.vigil.model.cached.scope.ScopeTarget
import utopia.vigil.model.partial.scope.ScopeData

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
}
