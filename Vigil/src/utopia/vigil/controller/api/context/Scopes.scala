package utopia.vigil.controller.api.context

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Tree}
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.view.mutable.eventful.CopyOnDemand
import utopia.vault.store.EqualsById
import utopia.vigil.database.VigilContext._
import utopia.vigil.database.access.scope.AccessScopes
import utopia.vigil.model.stored.scope.Scope

/**
 * Interface for managing scopes
 * @author Mikko Hilpinen
 * @since 03.05.2026, v0.1
 */
object Scopes
{
	// ATTRIBUTES   ------------------------
	
	private val _pointer = CopyOnDemand { connectionPool.logging { implicit c => AccessScopes.pull }.getOrElse(Empty) }
	private val treesP = _pointer.strongMap { scopes =>
		implicit val eq: EqualsFunction[Scope] = EqualsById
		scopes.iterator.filter { _.parentId.isEmpty }
			.map { root => Tree.iterate(root) { parent => scopes.filter { _.parentId.contains(parent.id) } } }
			.toOptimizedSeq
	}
	private val nodeByKeyP = mapTreesBy { _.key.toLowerCase }
	private val nodeByIdP = mapTreesBy { _.id }
		
	
	// OTHER    ----------------------------
	
	def nodeForId(scopeId: Int) = nodeByIdP.value.get(scopeId)
	def nodeForKey(key: String) = nodeByKeyP.value.get(key.toLowerCase)
	
	private def mapTreesBy[K](f: Scope => K) =
		treesP.strongMap { _.iterator.flatMap { _.allNodesIterator }.map { s => f(s) -> s }.toMap }
}
