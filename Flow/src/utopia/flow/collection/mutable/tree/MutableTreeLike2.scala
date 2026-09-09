package utopia.flow.collection.mutable.tree

import utopia.flow.collection.template.tree.{Tree, TreeLike2}

/**
  * Common trait for mutable tree implementations
  * @tparam N Type of the input accepted for node-addition
 * @tparam Repr Type of the nodes in this tree
 * @author Mikko Hilpinen
  * @since 1.11.2016, rewritten 8.9.2026 in v2.9
  */
trait MutableTreeLike2[-N, +Repr <: MutableTreeLike2[_, Repr]] extends TreeLike2[Repr]
{
	// ABSTRACT ---------------------
	
	/**
	 * Adds a new node under this one
	 * @param child The node to add under this node
	 */
	def +=(child: N): Unit
	/**
	 * Adds n nodes under this one
	 * @param children New child nodes to add
	 */
	def ++=(children: IterableOnce[N]): Unit
	
	/**
	 * Conditionally removes nodes directly under this node
	 * @param f A function that yields true for child nodes that should be kept
	 */
	def filterDirect(f: Repr => Boolean): Unit
	/**
	 * Conditionally removes nodes under this node, also targeting nodes further down this tree.
	 * @param f A function that yields true for child nodes that should be kept
	 */
	def filter(f: Repr => Boolean): Unit
	
	/**
	 * Removes all nodes under this one
	 */
	def clear(): Unit
	
	
	// OTHER    ------------
	
	/**
	 * Removes a node from this tree. If it appears in multiple locations, all occurrences will be
	 * removed
	 * @param node The node that is removed from under this node
	 */
	def -=(node: Tree): Unit = {
		removeChild(node)
		children.foreach { child => child -= node }
	}
	/**
	 * Removes a node from the direct children under this node
	 * @param child The node that is removed from under this node
	 */
	def removeChild(child: Tree) = filterDirect { _ != child }
	
	/**
	  * Removes the direct children of this node that match the specified function
	  * @param f A function that returns true for children that should be removed
	  */
	@deprecated("Renamed to filterDirect(...)", "v2.9")
	def clearChildrenWhere(f: Repr => Boolean) = filterDirect(f)
	/**
	  * Removes all children (direct and indirect) based on the specified function result
	  * @param f A function that returns true for children that should be removed
	  */
	@deprecated("Renamed to filter(...)", "v2.9")
	def clearNestedWhere(f: Repr => Boolean): Unit = filter(f)
}
