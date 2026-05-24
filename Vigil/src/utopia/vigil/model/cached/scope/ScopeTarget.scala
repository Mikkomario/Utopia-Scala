package utopia.vigil.model.cached.scope

import utopia.flow.collection.immutable.{Empty, Graph, Pair}
import utopia.flow.operator.enumeration.End
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.view.immutable.caching.Lazy
import utopia.vault.database.{Connection, DatabaseCache}
import utopia.vault.store.HasId
import utopia.vigil.database.VigilContext._
import utopia.vigil.database.access.scope.AccessScope
import utopia.vigil.database.access.scope.relation.AccessScopeRelations
import utopia.vigil.model.cached.scope.ScopeTarget.lazyGraphs

object ScopeTarget
{
	// ATTRIBUTES   -----------------------
	
	private val idByKey = DatabaseCache { (key: String, connection) =>
		implicit val c: Connection = connection
		AccessScope.forKey(key).id.pull
	}
	private val keyById = DatabaseCache { (id: Int, connection) =>
		implicit val c: Connection = connection
		AccessScope(id).key.pull
	}
	
	private val lazyGraphs = Lazy.resettable {
		val links = connectionPool.logging { implicit c => AccessScopeRelations.values.parentAndChildIds }
			.getOrElse(Empty)
		val childGraph = Graph(links.iterator.map { link => (link.first, (), link.second) }.toSet)
		val parentGraph = Graph(links.iterator.map { link => (link.second, (), link.first) }.toSet)
		
		Pair(parentGraph, childGraph)
	}
	
	
	// OTHER    ---------------------------
	
	/**
	 * @param scopeId ID of the targeted scope
	 * @return A scope target matching that ID
	 */
	def id(scopeId: Int): ScopeTarget = new ScopeById(scopeId, validated = false)
	/**
	 * @param key Key of the targeted scope (case-insensitive)
	 * @return A scope target matching that key
	 */
	def apply(key: String): ScopeTarget = new ScopeByKey(key)
	
	/**
	 * Updates the cached data.
	 * Should be called if new scopes are added.
	 */
	def update() = {
		lazyGraphs.reset()
		idByKey.cachedValues.foreach { _.reset() }
		keyById.cachedValues.foreach { _.reset() }
	}
	
	private def validId(scopeId: Int): ScopeTarget = new ScopeById(scopeId, validated = true)
	
	
	// NESTED   ---------------------------
	
	/**
	 * Represents an invalid scope target
	 */
	object InvalidScope extends ScopeTarget
	{
		override val isValid: Boolean = false
		override val id: Int = -1
		override val key: String = ""
		
		override val parentIdsIterator: Iterator[Int] = Iterator.empty
		override val grantedScopesIterator: Iterator[ScopeTarget] = Iterator.empty
	}
	
	private class ScopeById(override val id: Int, validated: Boolean) extends ScopeTarget
	{
		override lazy val key: String = keyById(id).value
		override lazy val isValid: Boolean = validated || key.nonEmpty
	}
	private class ScopeByKey(override val key: String) extends ScopeTarget
	{
		private lazy val _id = idByKey(key.toLowerCase).value
		
		override def id: Int = _id.getOrElse(-1)
		override def isValid: Boolean = _id.isDefined
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
	
	
	// COMPUTED --------------------------
	
	/**
	 * @return An iterator that yields the IDs of all scopes granted by this scope
	 */
	def grantedScopeIdsIterator: Iterator[Int] = relatedIdsIterator(Last)
	/**
	 * @return An iterator that yields all scopes granted by this scope
	 */
	def grantedScopesIterator = grantedScopeIdsIterator.map(ScopeTarget.validId)
	/**
	 * @return An iterator that yields the IDs of all scopes that grant this scope
	 */
	def parentIdsIterator = relatedIdsIterator(First)
	/**
	 * @return An iterator that yields all scopes that grant this scope
	 */
	def parentsIterator = parentIdsIterator.map(ScopeTarget.validId)
	
	
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
		
	private def relatedIdsIterator(targetSide: End) =
		lazyGraphs.value(targetSide)(id).allNodesIterator.drop(1).map { _.value }
}
