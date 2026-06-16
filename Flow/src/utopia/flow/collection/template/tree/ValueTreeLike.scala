package utopia.flow.collection.template.tree

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.view.immutable.View
import utopia.flow.view.template.Extender

/**
  * Common trait for tree implementations where each node wraps a value
  * @author Mikko Hilpinen
  * @since 05.06.2026
  * @tparam A Type of item used when navigating through this tree. May also be considered the main content of this tree.
  * @tparam Repr Types of nodes in this tree
  */
trait ValueTreeLike[+A, +Repr <: ValueTreeLike[A, Repr]] extends TreeLike2[Repr] with View[A] with Extender[A]
{
	// COMPUTED    ----------------------
	
	/**
	 * @return An iterator that goes over all navigation elements within this tree, including this node's nav.
	 *         Each branch is fully traversed before moving to the sibling branch.
	 *         This node's nav is returned first.
	 */
	def valuesIterator = value +: valuesBelowIterator
	/**
	 * @return An iterator that goes over all navigation elements within this tree, excluding this node's nav.
	 *         Each branch is fully traversed before moving to the sibling branch.
	 */
	def valuesBelowIterator: Iterator[A] = nodesBelowIterator.map { _.value }
	/**
	 * @return All navigational elements within this tree, including this node's nav.
	 */
	def values = valuesIterator.toOptimizedSeq
	
	
	// IMPLEMENTED  ----------------
	
	override def wrapped: A = value
	
	override def toString: String = if (isEmpty) value.toString else s"$value: {${ children.mkString(", ") }}"
	
	
	// OTHER    --------------------
	
	/**
	 * @param value A value
	 * @param eq Implicit equality function to apply. Default = `==`
	 * @return Whether this node or any of this node's children contains the specified navigational element
	 */
	def containsValue[N >: A](value: N)(implicit eq: EqualsFunction[N] = EqualsFunction.default): Boolean =
		eq(value, this.value) || valuesBelowIterator.exists { eq(_, value) }
	
	/**
	 * Finds the shared parent of the specified values elements. Assumes that this tree contains unique values.
	 * @param values Searched values
	 * @return Node within this tree structure that's the shared parent of all the specified elements.
	 *         None if one or more of the specified elements could not be found from this tree
	 */
	def commonParentOfValues[N >: A](values: Iterable[N])(implicit eq: EqualsFunction[N] = EqualsFunction.default) =
		findCommonParentOf(values) { (node, value) => eq(node.value, value) }
	
	/**
	 * Finds The location of a specific value within this tree structure.
	 * Assumes that this tree contains unique values.
	 * @param value Searched value
	 * @return A path to a node containing the specified value.
	 *         Returns None if there doesn't exist any node in this tree with the specified value.
	 *         The path, if found, consists of actual nodes that need to be traversed, starting with this node.
	 *         The node containing the specified value is located at the end of this path.
	 */
	def pathToValue[N >: A](value: N)(implicit eq: EqualsFunction[N] = EqualsFunction.default) =
		findWithPath { node => eq(node.value, value) }
}
