package utopia.flow.datastructure.template

import utopia.flow.util.CollectionExtensions._

/**
 * Tree nodes form individual trees. They can also be used as subtrees in other tree nodes. Like 
 * other nodes, treeNodes contain / wrap certain type of content. A tree node can never contain 
 * itself below itself.
 * @author Mikko Hilpinen
 * @since 1.11.2016
 */
trait TreeLike[A, NodeType <: TreeLike[A, NodeType]] extends Node[A]
{
    // ABSTRACT   --------------------
    
    /**
     * The child nodes directly under this node
     */
    def children: Vector[NodeType]
    
    /**
      * @param content Content for the child node
      * @return A new node
      */
    protected def makeNode(content: A): NodeType
    
    
    // COMPUTED PROPERTIES    --------
    
    /**
     * All nodes below this node, in no specific order
     */
    def nodesBelow: Vector[NodeType] = children ++ children.flatMap { child => child.nodesBelow }
    
    /**
     * The size of this tree. In other words, the number of nodes below this node
     */
    def size: Int = children.foldLeft(children.size)((size, child) => size + child.size)
    
    /**
     * Whether this tree is empty and doesn't contain a single node below it
     */
    def isEmpty = children.isEmpty
    
    /**
     * @return Whether this tree has child nodes registered under it
     */
    def hasChildren = children.nonEmpty
    
    /**
     * The depth of this tree. A tree with no children has depth of 0, a tree with only direct
     * children has depth of 1, a tree with grand children has depth of 2 and so on.
     */
    def depth: Int = children.foldLeft(0)((maxDepth, child) => math.max(maxDepth, 1 + child.depth))
    
    /**
      * @return The leaf nodes anywhere <b>under</b> this node. Leaves are nodes that do not have any children
      */
    def leaves: Vector[NodeType] = children.flatMap { c => if (c.isEmpty) Vector(c) else c.leaves }
    
    /**
      * @return All content within this node and the nodes under
      */
    def allContent = content +: nodesBelow.map { _.content }
    
    /**
     * @return List of all "branches" <b>under</b> this node. Eg. If this node contains two children each of which
     *         have two children themselves, returns <b>4</b> vectors that each have a size of 2. The vectors only contain
     *         node content, not the nodes themselves. <b>This node is not included in any of the returned vectors</b>.
     *         In other words, the resulting number of vectors is the same as the number of leaves in this tree and
     *         the depth of each vector matches the length of each branch, including that leaf.
     */
    def allBranches: Vector[Vector[A]] =
    {
        // Lists branches starting from each of this tree's children (includes children in the branches they found)
        children.flatMap { child =>
            // Leaves form the ends of the branches
            if (child.hasChildren)
                child.allBranches.map { branch => child.content +: branch }
            else
                Vector(Vector(child.content))
        }
    }
    
    
    // IMPLEMENTED  ----------------
    
    override def toString: String =
    {
        if (isEmpty)
            s"{content: $content}"
        else
            s"{content: $content, children: [${ children.map { _.toString }.reduce { _ + ", " + _ } }]}"
    }
    
    
    // OPERATORS    ----------------
    
    /**
      * Finds or generates a node directly under this one that has the provided content
      * @param content The searched content
      * @return Either an existing node or a made-up one
      */
    def /(content: A) = get(content) getOrElse makeNode(content)
    
    
    // OTHER METHODS    ------------
    
    /**
      * Finds a child directly under this node that has the provided content
      * @param content The searched content of the child
      * @return The first child with the provided content or None if there is no direct child with
      * such content
      */
    def get(content: A) = children.find { _.content == content }
    
    /**
      * Performs a search over the whole tree structure
      * @param filter A predicate for finding a node
      * @return The first child that satisfies the predicate. None if no such child was found.
      */
    def find(filter: NodeType => Boolean): Option[NodeType] = children.find(filter) orElse children.findMap { _.find(filter) }
    
    /**
     * Checks whether a node exists below this node
     * @param node A node that may exist below this node
     * @return Whether the provided node exists below this node
     */
    def contains(node: TreeLike[_, _]): Boolean = { children.contains(node) || children.exists { _.contains(node) } }
    
    /**
      * Checks whether this node or any of this node's children contains the specified content
      * @param content The searched content
      * @return Whether this node or any node under this node contains the specified content
      */
    def contains(content: A): Boolean = this.content == content || children.exists { _.contains(content) }
    
    /**
     * Finds the first child node from this entire tree that matches the specified condition. Returns the whole path
     * to that node
     * @param filter A search condition
     * @return Path to the first node matching the specified condition, if such a node exists
     */
    def findWithPath(filter: NodeType => Boolean): Option[Vector[A]] =
    {
        children.find(filter).map { c => Vector(c.content) }.orElse { children.findMap { c => c.findWithPath(filter).map { c.content +: _ } } }
    }
    
    /**
      * Finds the top nodes under this node (whether they be direct children or grandchildren etc.) that satisfy the
      * specified filter. Includes the "path" to all of the selected nodes as well. If a node is selected, it's children are
      * not tested anymore. All of the returned values are within separate trees.
      * @param filter A filter function
      * @return Paths to all of the nodes that satisfy the specified filter function
      */
    def filterWithPaths(filter: NodeType => Boolean): Vector[Vector[A]] =
    {
        val (notAccepted, accepted) = children.divideBy(filter)
        // Adds accepted children as is and finds potential matches under the non-accepted children
        accepted.map { c => Vector(c.content) } ++ notAccepted.flatMap { _.filterWithPaths(filter) }.filterNot { _.isEmpty }
    }
}