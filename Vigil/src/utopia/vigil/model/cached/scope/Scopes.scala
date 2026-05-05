package utopia.vigil.model.cached.scope

import utopia.flow.collection.immutable.OptimizedIndexedSeq
import utopia.flow.collection.template.factory.FromCollectionFactory
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.operator.MaybeEmpty
import utopia.flow.view.template.Extender

object Scopes extends FromCollectionFactory[ScopeTarget, Scopes]
{
	// IMPLICIT ---------------------
	
	implicit def from(scopes: Iterable[ScopeTarget]): Scopes = new Scopes(OptimizedIndexedSeq.from(scopes))
	
	
	// IMPLEMENTED  -----------------
	
	override def from(items: IterableOnce[ScopeTarget]): Scopes = new Scopes(OptimizedIndexedSeq.from(items))
}

/**
 * Represents a set of scopes
 * @author Mikko Hilpinen
 * @since 05.05.2026, v0.1
 */
class Scopes private(override val wrapped: Seq[ScopeTarget])
	extends Extender[Seq[ScopeTarget]] with ValueConvertible with MaybeEmpty[Scopes]
{
	// ATTRIBUTES   -----------------
	
	/**
	 * IDs of the scopes in this set
	 */
	lazy val ids = wrapped.view.map { _.id }.toSet
	
	
	// IMPLEMENTED  -----------------
	
	override def self: Scopes = this
	override def isEmpty: Boolean = wrapped.isEmpty
	
	override def toValue: Value = wrapped.map { _.key }
	
	
	// OTHER    ---------------------
	
	/**
	 * @param scope A scope
	 * @return Whether that scope is covered by these scopes
	 */
	def contains(scope: ScopeTarget) = scope.isContainedWithin(ids)
	
	/**
	 * @param other Another set of scopes
	 * @return Scopes that are present in this set, which are not
	 */
	def --(other: Scopes) = {
		if (isEmpty || other.isEmpty)
			this
		else
			new Scopes(wrapped.filterNot(other.contains))
	}
	
	/**
	 * @param scopeIds A set of scope IDs
	 * @return A subset of these scopes, that are not covered by scopes with those IDs
	 */
	def notContainedWithin(scopeIds: Set[Int]) = {
		if (isEmpty || scopeIds.isEmpty)
			this
		else
			new Scopes(wrapped.filterNot { _.isContainedWithin(scopeIds) })
	}
}
