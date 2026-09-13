package utopia.flow.collection.immutable.tree

import utopia.flow.collection.template.tree.{Tree, TreeLike2}

/**
  * A common trait for tree implementations which support filtering / pruning
  * @author Mikko Hilpinen
  * @since 05.06.2026, v2.9
  */
trait FilterableTreeLike[+Repr <: FilterableTreeLike[Repr]] extends TreeLike2[Repr]
{
	// ABSTRACT --------------------
	
	/**
	 * @return A copy of this tree without any child nodes included
	 */
	def withoutChildren: Repr
	
	/**
	 * Filters the children directly under this node
	 * @param f A function that determines whether a direct child should be kept attached to this node
	 * @return A filtered copy of this tree
	 */
	def filterDirect(f: Repr => Boolean): Repr
	/**
	 * Filters this whole tree structure using the specified filter function
	 * @param f A filtering function that determines which nodes should be kept
	 * @return A filtered copy of this tree
	 */
	def filter(f: Repr => Boolean): Repr
	
	
	// OTHER    ----------------
	
	/**
	  * Creates a new copy of this tree where the specified tree doesn't occur anywhere.
	  * @param node The tree that is not included in the copy
	  * @return A copy of this tree without the provided tree
	  */
	def -(node: Tree): Repr = if (isEmpty) self else filter { _ != node }
	
	/**
	 * Creates a new copy of this tree without the provided direct child node
	 * @param child The child node that is removed from the direct children under this tree
	 */
	def withoutDirect(child: TreeLike2[_]) = filterDirect { _ == child }
}
