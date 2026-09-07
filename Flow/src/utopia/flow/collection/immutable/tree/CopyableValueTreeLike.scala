package utopia.flow.collection.immutable.tree

import utopia.flow.collection.template.tree.ValueTreeLike

/**
 * Common trait for copyable (immutable) trees, where each node wraps a value
 * @author Mikko Hilpinen
 * @since 04.08.2026, v2.9
 */
trait CopyableValueTreeLike[A, -N, +Repr <: CopyableTreeLike[N, Repr] with ValueTreeLike[A, Repr]]
	extends CopyableTreeLike[N, Repr] with ValueTreeLike[A, Repr]
{
	// OTHER    -------------------------
	
	/**
	 * Creates a new copy of this tree where the specified value never occurs.
	 * Removes the content from every child, including grand children etc.
	 * @param valueToRemove The value to remove
	 * @return A copy of this tree without nodes containing the specified value
	 */
	def withoutValue(valueToRemove: A): Repr = filterValues { _ != valueToRemove }
	/**
	 * Creates a copy of this tree without direct children that would contain the specified value
	 * @param valueToRemove A value to exclude from nodes directly under this one
	 * @return A copy of this tree without that value included in its direct children
	 */
	def withoutDirectValue(valueToRemove: A) = filterDirectValues { _ != valueToRemove }
	
	/**
	 * Filters the children directly under this node
	 * @param f A function that determines whether a direct child should be kept attached to this node.
	 *          Accepts the navigational element representing a child tree.
	 * @return A filtered copy of this tree
	 */
	def filterDirectValues(f: A => Boolean) = filterDirect { n => f(n.value) }
	/**
	 * Filters this whole tree structure using the specified filter function
	 * @param f A filtering function that determines which nodes should be kept.
	 *          Accepts the navigational element representing a node.
	 * @return A filtered copy of this tree
	 */
	def filterValues(f: A => Boolean) = filter { n => f(n.value) }
	
	/*
	/**
	 * Replaces a single branch within this tree with the specified branch, based on the branch root nav element.
	 * @param newBranch A tree to replace an existing branch with
	 * @return Either:
	 *             Left: This tree, if it didn't contain a node that could be replaced with the specified tree
	 *             Right: A copy of this tree where the highest node
	 *             with a nav element matching that of the specified node has been replaced with the specified node
	 */
	def replaceBranch(newBranch: Repr) = mapFirstWhere { _.nav ~== newBranch.nav } { _ => newBranch }
	/**
	 * Merges a branch into this tree at the first node that has a matching nav element
	 * as the root of the specified branch.
	 * When merging the branch into this tree, preserves all nodes of this tree and adds missing nodes from
	 * the specified branch.
	 * @param branch A branch to merge into this tree, if possible
	 * @return Either:
	 *             Left: This tree if it didn't contain a node with a nav element
	 *             matching the root of the specified branch, or
	 *             Right: A copy of this tree with the specified branch merged with the first node that had a
	 *             matching nav element.
	 */
	def mergeBranch(branch: Repr) = {
		if (branch.hasChildren)
			mapFirstWhere { _.nav ~== branch.nav } { _ :++ branch.children }
		else if (containsNav(branch.nav))
			Right(self)
		else
			Left(self)
	}
	 */
}
