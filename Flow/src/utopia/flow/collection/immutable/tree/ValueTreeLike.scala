package utopia.flow.collection.immutable.tree

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.iterable.CachingSeq
import utopia.flow.collection.immutable.{OptimizedIndexedSeq, Single}
import utopia.flow.collection.template

import scala.language.implicitConversions

/**
 * Common trait for immutable trees where each node wraps some kind of value
 * @tparam A Type of the wrapped values
 * @tparam N Type of the accepted input trees (generic)
 * @tparam CC Generic type of the implementing collection
 * @tparam Repr Type-specific implementing collection type
 * @author Mikko Hilpinen
 * @since 10.08.2026, v2.9
 */
trait ValueTreeLike[+A, N[+_], +CC[X] <: N[X], +Repr <: ValueTreeLike[A, N, CC, Repr] with N[A]]
	extends template.tree.ValueTreeLike[A, CC, Repr] with FilterableTreeLike[Repr]
{
	// ABSTRACT -----------------------------
	
	/**
	 * @param newValue New value to assign to this node
	 * @tparam B Type of the new value
	 * @return Copy of this node with the specified value
	 */
	def withValue[B >: A](newValue: B): CC[B]
	/**
	 * Maps all values in this tree
	 * @param f A mapping function to apply to each value in this tree
	 * @tparam B Mapping result type
	 * @return Copy of this tree where every value has been mapped
	 */
	def mapValues[B >: A](f: A => B): CC[B]
	
	/**
	 * Adds n new child nodes directly under this node
	 * @param newChildren New children to add under this node
	 * @return A copy of this tree that includes the specified child nodes
	 */
	def ++[B >: A](newChildren: IterableOnce[N[B]]): CC[B]
	
	/**
	 * @param newChildren Children to replace this node's current child nodes with
	 * @tparam B Type of the values within the child nodes
	 * @return Copy of this tree with the specified child nodes
	 */
	def withChildren[B >: A](newChildren: IterableOnce[N[B]]): CC[B]
	
	
	// OTHER    -----------------------------
	
	/**
	 * Adds a new direct child node to this tree
	 * @param node A child node to add
	 * @return a copy of this tree with the specified child added
	 */
	def :+[B >: A](node: N[B]): CC[B] = this ++ Single(node)
	
	/**
	 * @param f A mapping function to apply to this node's value
	 * @tparam B Type of mapping results
	 * @return Copy of this node with the wrapped value mapped
	 */
	def mapLocalValue[B >: A](f: A => B) = withValue(f(value))
	
	/**
	 * @param f A mapping function to apply to the values of this node's children
	 * @tparam B Type of mapping results
	 * @return Copy of this tree with the values of the direct child nodes mapped
	 */
	def mapDirectValues[B >: A](f: A => B): CC[B] = mapDirect[B] { _.mapLocalValue(f): N[B] }
	/**
	 * Creates a copy of this node with its direct children mapped
	 * @param f A mapping function for direct child nodes
	 * @return A mapped copy of this node
	 */
	def mapDirect[B >: A](f: Repr => N[B]): CC[B] = withChildren(children.map(f))
	
	/**
	 * Replaces one of the child nodes of this tree
	 * @param original Child node to replace
	 * @param replacement Replacing node
	 * @return Copy of this tree with 'original' replaced with 'replacement'.
	 *         Note: If this tree didn't directly contain 'original', 'replacement' is still added.
	 */
	def replaceChild[B >: A](original: Any, replacement: N[B]) =
		withChildren[B](children.mapOrAppend[N[B], Seq[N[B]]] { node =>
			if (original == node) Some(replacement) else None
		}(replacement))
	
	/**
	 * Maps the topmost nodes in this tree, which match the specified filter function.
	 * @param filter A filter function that yields true for nodes that should be mapped (using 'f')
	 * @param f A function that performs the mapping for nodes for which 'filter' yielded true.
	 * @return A mapped copy of this tree
	 */
	def mapRootsWhere[B >: A](filter: Repr => Boolean)(f: Repr => N[B]): CC[B] =
		mapDirect { c => if (filter(c)) f(c) else c.mapRootsWhere[B](filter)(f): N[B] }
	
	/**
	 * Maps the first node that satisfies the specified search condition.
	 * Targets all nodes under this one.
	 * @param find A search function that yields true for the node to map
	 * @param map A mapping function applied to the found node
	 * @return Either:
	 *         - Left: This tree, if no node satisfied the specified search condition, or
	 *         - Right: A copy of this tree with a single node mapped
	 */
	def mapFirstWhere[B >: A](find: Repr => Boolean)(map: Repr => N[B]) = {
		val (result, mapped) = _mapFirstWhere[B](find)(map)
		if (mapped)
			Right(result)
		else
			Left(this)
	}
	private def _mapFirstWhere[B >: A](find: Repr => Boolean)(map: Repr => N[B]): (N[B], Boolean) = {
		// Only iterates as far as needed
		val iter = children.iterator
		val builder = OptimizedIndexedSeq.newBuilder[N[B]]
		var searching = true
		
		while (searching && iter.hasNext) {
			val child = iter.next()
			// Case: Found the child node to map => Performs the mapping and stops iterating
			if (find(child)) {
				searching = false
				builder += map(child)
			}
			// Case: Not the targeted node => Checks whether this child contains said node
			else {
				val (deepMapped, changed) = child._mapFirstWhere[B](find)(map)
				builder += deepMapped
				// Case: The targeted node existed deeper inside the child tree => Stops iterating the child nodes
				if (changed)
					searching = false
			}
		}
		
		// Case: No targeted node was found => Yields self
		if (searching)
			self -> false
		// Case: Targeted node was found => Appends the unmodified child nodes and returns
		else
			(withChildren(CachingSeq(iter, builder.result())): N[B], true)
	}
}