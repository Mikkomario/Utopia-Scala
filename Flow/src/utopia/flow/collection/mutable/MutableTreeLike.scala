package utopia.flow.collection.mutable

import utopia.flow.collection
import utopia.flow.collection.immutable.Empty

/**
  * Tree nodes form individual trees. They can also be used as subtrees in other tree nodes. Like
  * other nodes, treeNodes contain / wrap certain type of content. A tree node can never contain
  * itself below itself.
  * @author Mikko Hilpinen
  * @since 1.11.2016
  */
trait MutableTreeLike[A, Repr <: MutableTreeLike[A, Repr]] extends collection.template.TreeLike[A, Repr]
{
	// ABSTRACT ---------------------
	
	/**
	  * Updates the children of this treelike
	  * @param newChildren The new children
	  */
	protected def setChildren(newChildren: Seq[Repr]): Unit
	
	
	// OPERATORS    -----------------
	
	/**
	  * Adds a treeNode directly under this node. The node won't be added if a) it already exists as
	  * a direct child of this node or b) This node exists under the provided node
	  * @param child The node that is added under this node
	  * @return Whether the node was successfully added under this node
	  */
	def +=(child: Repr) =
	{
		// Makes sure the child doesn't already exist in the direct children
		// And that this node won't end up under a child node
		if (this != child && !children.contains(child) && !child.containsNode(this)) {
			setChildren(children :+ child)
			true
		}
		else
			false
	}
	
	/**
	  * Removes a node from this tree. If it appears in multiple locations, all occurrences will be
	  * removed
	  * @param node The node that is removed from under this node
	  */
	def -=(node: MutableTree[A]): Unit =
	{
		removeChild(node)
		children.foreach { child => child -= node }
	}
	
	
	// OTHER METHODS    ------------
	
	/**
	  * Clears the node, removing any nodes below it
	  */
	def clear() = setChildren(Empty)
	
	/**
	  * Removes the direct children of this node that match the specified function
	  * @param f A function that returns true for children that should be removed
	  */
	def clearChildrenWhere(f: Repr => Boolean) = setChildren(children.filterNot(f))
	
	/**
	  * Removes all children (direct and indirect) based on the specified function result
	  * @param f A function that returns true for children that should be removed
	  */
	def clearNestedWhere(f: Repr => Boolean): Unit =
		setChildren(children.filterNot(f).map { c => c.clearNestedWhere(f); c })
	
	/**
	  * Removes a node from the direct children under this node
	  * @param child The node that is removed from under this node
	  */
	def removeChild(child: MutableTreeLike[A, _]) = setChildren(children.filterNot { _ == child })
}
