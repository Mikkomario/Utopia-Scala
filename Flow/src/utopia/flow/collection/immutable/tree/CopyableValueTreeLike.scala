package utopia.flow.collection.immutable.tree

import utopia.flow.collection.template.tree.ValueTreeLike
import utopia.flow.operator.equality.EqualsExtensions.ImplicitApproxEquals
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.util.Mutate

/**
 * Common trait for copyable (immutable) trees, where each node wraps a value
 * @author Mikko Hilpinen
 * @since 04.08.2026, v2.9
 */
// TODO: Review and refactor (these methods were collected from previous immutable tree version)
trait CopyableValueTreeLike[A, Repr <: CopyableTreeLike[Repr] with ValueTreeLike[A, Repr]]
	extends CopyableTreeLike[Repr] with ValueTreeLike[A, Repr]
{
	protected def wrap(value: A): Repr
	
	/*
	def withValue(value: A) = {
		val mid = wrap(value)
		if (hasChildren) mid.withChildren(children) else mid
	}
	def mapValue(f: Mutate[A]) = withValue(f(value))
	*/
	
	/**
	 * @param value Child node value to add
	 * @return A copy of this tree including a new child with that value
	 */
	def :+(value: A): Repr = withChildren(children :+ wrap(value))
	/**
	 * Creates a new tree that contains a child node with specified value.
	 * If this node already contains such a child, doesn't add a new one.
	 * @param value Child node value
	 * @return A copy of this tree including a child with that value
	 */
	def including(value: A)(implicit eq: EqualsFunction[A] = EqualsFunction.default) = {
		// Case: This node already contains that child => ignores
		if (children.exists { _.value ~== value })
			self
		// Case: New child => inserts
		else
			this :+ value
	}
	
	/**
	 * Creates a new copy of this tree where specified value never occurs.
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
	 * Maps children which are reachable using the specified content path
	 * @param path path to the child or children being targeted, where each item represents targeted content.
	 *             An empty path represents this node.
	 * @param f    A mapping function that modifies the node(s) at the end of the specified path
	 * @return A modified copy of this tree
	 */
	def mapPath(path: Seq[A])(f: Repr => Repr): Repr = {
		path.headOption match {
			case Some(nextStep) =>
				if (path hasSize 1)
					mapChildren { c => if (c.nav ~== nextStep) f(c) else c }
				else {
					val remaining = path.tail
					mapChildren { c => if (c.nav ~== nextStep) c.mapPath(remaining)(f) else c }
				}
			case None => f(self)
		}
	}
	/**
	 * Maps children which are reachable using the specified content path
	 * @param start First step (content) on the path
	 * @param next  The next step (content) on the path
	 * @param more  Additional steps
	 * @param f     A mapping function that modifies the node(s) at the end of the specified path
	 * @return A modified copy of this tree
	 */
	def mapPath(start: A, next: A, more: A*)(f: Repr => Repr): Repr = mapPath(Pair(start, next) ++ more)(f)
	
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
}
