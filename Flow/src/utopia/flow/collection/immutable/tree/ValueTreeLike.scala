package utopia.flow.collection.immutable.tree

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.iterable.CachingSeq
import utopia.flow.collection.immutable.{OptimizedIndexedSeq, Single}
import utopia.flow.collection.template
import utopia.flow.collection.template.tree.TreeLike2
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.view.immutable.View

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
// NB: A lot of the editor's errors related to typing are false
// Repr <: CC[B] <: N[B] when B >: A, but the editor doesn't pick this up.
trait ValueTreeLike[+A, N[+_], +CC[+X] <: N[X], +Repr <: ValueTreeLike[A, N, CC, Repr] with CC[A]]
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
	def replaceChild[B >: A](original: Any, replacement: N[B]): CC[B] =
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
			Left(self)
	}
	private def _mapFirstWhere[B >: A](find: Repr => Boolean)(map: Repr => N[B]): (CC[B], Boolean) = {
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
				builder += (deepMapped: N[B])
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
			(withChildren(CachingSeq[N[B]](iter, builder.result())), true)
	}
	
	/**
	 * Replaces a single branch within this tree with the specified branch, based on the branch root nav element.
	 * @param newBranch A tree to replace an existing branch with
	 * @return Either:
	 *             Left: This tree, if it didn't contain a node that could be replaced with the specified tree
	 *             Right: A copy of this tree where the highest node
	 *             with a nav element matching that of the specified node has been replaced with the specified node
	 */
	def replaceBranch[B >: A](newBranch: N[B] with View[B])(implicit eq: EqualsFunction[B] = EqualsFunction.default) =
		mapFirstWhere[B] { c => eq(c.value, newBranch.value) } { _ => newBranch }
	/**
	 * Merges a branch into this tree at the first direct child node that shares a value with the specified branch root.
	 *
	 * Applies the merge as a join; I.e. doesn't add duplicate nodes,
	 * but adds all missing nodes even from the lower layers.
	 *
	 * @param branch A branch to merge into this tree, if possible
	 * @return Either:
	 *          - Left(self) if this tree didn't contain a node with matching value
	 *          - Right: A copy of this tree with the specified branch
	 *            merged with the first child node that contained a matching value.
	 */
	def mergeBranch[B >: A, T <: N[B] with TreeLike2[T] with View[B]](branch: T)
	                                                                 (implicit eq: EqualsFunction[B] = EqualsFunction.default): Either[Repr, CC[B]] =
	{
		// Case: Branch has child nodes => Attempts to merge it with one of the existing nodes
		if (branch.hasChildren)
			mapFirstWhere[B] { n => eq(n.value, branch.value) } { _.joinBranches[B, T](branch.children): N[B] }
		// Case: Already contains a matching node => No changes are needed
		else if (containsDirectValue(branch.value))
			Right(self)
		// Case: No matching node to merge with => Yields Left.
		else
			Left(self)
	}
	
	/**
	 * Joins a new branch to this tree.
	 * Nodes that match existing children are joined with them.
	 * Nodes that don't match any existing child are appended.
	 * @param branch Branch to join to this tree.
	 * @param eq Implicit equals function used when matching child nodes. Default = use ==.
	 * @tparam B Type of values in the joined branches
	 * @tparam T Type of the joined branch nodes
	 * @return A copy of this tree with the specified branch joined.
	 */
	def joinBranch[B >: A, T <: N[B] with TreeLike2[T] with View[B]](branch: T)
	                                                                (implicit eq: EqualsFunction[B] = EqualsFunction.default): CC[B] =
		joinBranches[B, T](Single(branch))
	/**
	 * Joins n new branches to this tree.
	 * Nodes that match existing children are joined with them.
	 * Nodes that don't match any existing child are appended.
	 * @param branches Branches to join to this tree. Not empty.
	 * @param eq Implicit equals function used when matching child nodes. Default = use ==.
	 * @tparam B Type of values in the joined branches
	 * @tparam T Type of the joined branch nodes
	 * @return A copy of this tree with the specified branches joined.
	 */
	def joinBranches[B >: A, T <: N[B] with TreeLike2[T] with View[B]](branches: IterableOnce[T])
	                                                                  (implicit eq: EqualsFunction[B] = EqualsFunction.default): CC[B] =
	{
		// Divides the new nodes into those that match existing children and those that don't
		val (newBranches, matches) = branches.divideWith { branch =>
			children.find { child => eq(child.value, branch.value) } match {
				// Case: Match with an existing child node => Only applies it if there are grandchildren to join, also
				case Some(matchingChild) =>
					if (branch.hasChildren)
						Right(Some(matchingChild -> branch))
					else
						Right(None)
				// Case: Not a match
				case None => Left(branch)
			}
		}
		val appliedMatches = matches.flatten
		// Case: No matches => Only adds the new branches
		if (appliedMatches.isEmpty)
			this ++ newBranches
		// Case: Matches => Recursively merges them with the existing children
		else {
			val merges = appliedMatches.iterator
				.map { case (child, branch) => child -> child.joinBranches[B, T](branch.children) }.toMap
			val updatedChildren: Seq[N[B]] = children.map[N[B]] { child =>
				merges.get(child) match {
					case Some(merged) => merged
					case None => child
				}
			}
			
			// Includes the completely new child nodes, if appropriate
			if (newBranches.isEmpty)
				withChildren[B](updatedChildren)
			else
				withChildren[B](updatedChildren ++ newBranches)
		}
	}
}