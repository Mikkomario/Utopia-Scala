package utopia.vigil.model.cached.scope

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Tree}
import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.view.mutable.eventful.CopyOnDemand
import utopia.vault.store.{EqualsById, HasId}
import utopia.vigil.database.VigilContext._
import utopia.vigil.database.access.scope.AccessScopes
import utopia.vigil.model.stored.scope.Scope

object ScopeTarget
{
	// ATTRIBUTES   -----------------------
	
	/**
	 * A pointer that contains all registered scopes. May be updated / refreshed.
	 */
	private val _pointer = CopyOnDemand { connectionPool.logging { implicit c => AccessScopes.pull }.getOrElse(Empty) }
	/**
	 * A pointer that contains all registered scopes as trees.
	 */
	private val treesP = _pointer.strongMap { scopes =>
		implicit val eq: EqualsFunction[Scope] = EqualsById
		scopes.iterator.filter { _.parentId.isEmpty }
			.map { root => Tree.iterate(root) { parent => scopes.filter { _.parentId.contains(parent.id) } } }
			.toOptimizedSeq
	}
	/**
	 * A pointer that contains all valid targets, mapped to their keys
	 */
	private val byKeyP = mapTreesBy { _.key.toLowerCase }
	/**
	 * A pointer that contains all valid targets, mapped to their IDs
	 */
	private val byIdP = mapTreesBy { _.id }
	
	
	// OTHER    ---------------------------
	
	/**
	 * @param scopeId ID of the targeted scope
	 * @return A scope target matching that ID
	 */
	def id(scopeId: Int): ScopeTarget = byIdP.value.getOrElse(scopeId, InvalidScope)
	/**
	 * @param key Key of the targeted scope (case-insensitive)
	 * @return A scope target matching that key
	 */
	def apply(key: String): ScopeTarget = byKeyP.value.getOrElse(key.toLowerCase, InvalidScope)
	
	/**
	 * Updates the cached data.
	 * Should be called if new scopes are added.
	 */
	def update() = _pointer.update()
	
	private def mapTreesBy[K](f: Scope => K) =
		treesP.strongMap { _.iterator.flatMap { _.allNodesIterator }.map { s => f(s) -> new NodeWrapper(s) }.toMap }
	
	
	// NESTED   ---------------------------
	
	/**
	 * Represents an invalid scope target
	 */
	object InvalidScope extends ScopeTarget
	{
		override val isValid: Boolean = false
		override val id: Int = -1
		override val key: String = ""
		
		override val parentId: Option[Int] = None
		override val childrenIterator: Iterator[ScopeTarget] = Iterator.empty
	}
	
	private class NodeWrapper(node: Tree[Scope]) extends ScopeTarget
	{
		// ATTRIBUTES   --------------------
		
		override val isValid: Boolean = true
		
		
		// IMPLEMENTED  --------------------
		
		override def id: Int = node.id
		override def key: String = node.nav.key
		
		override def parentId: Option[Int] = node.nav.parentId
		override def childrenIterator: Iterator[ScopeTarget] = node.children.iterator.map { new NodeWrapper(_) }
	}
}

/**
 * Used for targeting specific scopes. Data may be acquired lazily.
 * @author Mikko Hilpinen
 * @since 01.05.2026, v0.1
 */
trait ScopeTarget extends HasId[Int]
{
	// ABSTRACT --------------------------
	
	/**
	 * @return Whether this is a valid scope
	 */
	def isValid: Boolean
	/**
	 * @return The key / identifier of this scope
	 */
	def key: String
	
	/**
	 * @return ID of the scope directly above this scope
	 */
	def parentId: Option[Int]
	/**
	 * @return An iterator that yields the scopes directly under this scope
	 */
	def childrenIterator: Iterator[ScopeTarget]
	
	
	// COMPUTED --------------------------
	
	/**
	 * @return Scope directly above this one
	 */
	def parent: Option[ScopeTarget] = parentId.map(ScopeTarget.id)
	
	/**
	 * @return An iterator that yields IDs of the scopes above this one,
	 *         starting from the closes and ending in the root scope.
	 *         Empty if this is a root scope.
	 */
	def parentIdsIterator = OptionsIterator.iterate(parentId) { ScopeTarget.id(_).parentId }
	/**
	 * @return An iterator that yields the parent scopes, starting from the closes and ending in the root scope.
	 *         Empty if this is a root scope.
	 */
	def parentsIterator = OptionsIterator.iterate(parent) { _.parent }
	
	
	// IMPLEMENTED  ----------------------
	
	override def toString: String = key
	
	
	// OTHER    --------------------------
	
	/**
	 * @param other Another scope
	 * @return Whether this scope is contained within the specified scope
	 */
	def isUnder(other: ScopeTarget): Boolean = parentIdsIterator.contains(other.id)
	/**
	 * @param other Another scope
	 * @return Whether this scope contains the specified scope
	 */
	def contains(other: ScopeTarget) = other.isUnder(this)
	
	/**
	 * @param grantedScopeIds A set of scope IDs
	 * @return Whether this scope, or one of the parent scopes, is included in the specified set
	 */
	def isContainedWithin(grantedScopeIds: Set[Int]): Boolean =
		grantedScopeIds.contains(id) || parentIdsIterator.exists(grantedScopeIds.contains)
}
