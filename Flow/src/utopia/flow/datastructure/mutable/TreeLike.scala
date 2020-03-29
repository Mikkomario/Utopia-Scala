package utopia.flow.datastructure.mutable

import utopia.flow.datastructure.template


/**
 * Tree nodes form individual trees. They can also be used as subtrees in other tree nodes. Like 
 * other nodes, treeNodes contain / wrap certain type of content. A tree node can never contain 
 * itself below itself.
 * @author Mikko Hilpinen
 * @since 1.11.2016
 */
trait TreeLike[A, NodeType <: TreeLike[A, NodeType]] extends template.TreeLike[A, NodeType]
{
    // ABSTRACT ---------------------
    
    /**
      * Updates the childs of this treelike
      * @param newChildren The new children
      */
    protected def setChildren(newChildren: Vector[NodeType]): Unit
    
    
    // OPERATORS    -----------------
    
    /**
     * Adds a treeNode directly under this node. The node won't be added if a) it already exists as 
     * a direct child of this node or b) This node exists under the provided node
     * @param child The node that is added under this node
     * @return Whether the node was successfully added under this node
     */
    def +=(child: NodeType) =
    {   
        // Makes sure the child doesn't already exist in the direct children
        // And that this node won't end up under a child node
        if (!children.contains(child) && !child.contains(this))
        {
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
    def -=(node: Tree[A]): Unit =
    {
        removeChild(node)
        children.foreach { child => child -= node }
    }
    
    
    // OTHER METHODS    ------------
    
    /**
     * Clears the node, removing any nodes below it
     */
    def clear() = setChildren(Vector())
    
    /**
     * Removes a node from the direct children under this node
     * @param child The node that is removed from under this node
     */
    def removeChild(child: Tree[A]) = setChildren(children.filterNot { _ == child })
}