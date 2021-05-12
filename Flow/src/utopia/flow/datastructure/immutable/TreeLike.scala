package utopia.flow.datastructure.immutable

import utopia.flow.datastructure.template


/**
 * This TreeNode implementation is immutable and safe to reference from multiple places
 * @author Mikko Hilpinen
 * @since 4.11.2016
 */
trait TreeLike[A, NodeType <: TreeLike[A, NodeType]] extends template.TreeLike[A, NodeType]
{
    // ABSTRACT --------------------
    
    protected def makeNode(content: A, children: Vector[NodeType]): NodeType
    
    override protected def makeNode(content: A): NodeType = makeNode(content, Vector())
    
    
    // COMPUTED --------------------
    
    /**
     * @return A copy of this tree without any child nodes included
     */
    def withoutChildren = makeNode(content)
    
    
    // OPERATORS    ----------------
    
    /**
     * Creates a new tree that contains a new child node
     * @param tree The child node in the new tree
     * @return a copy of this tree with the provided child tree
     */
    def +(tree: NodeType) = makeNode(content, children :+ tree)
    
    /**
      * Creates a new tree that contains a child node with specified content
      * @param nodeContent Child node content
      * @return A copy of this tree with child node added
      */
    def +(nodeContent: A): NodeType = this + makeNode(nodeContent)
    
    /**
     * Creates a new copy of this tree where the provided tree doesn't occur. Removes the element from even child
      * trees
     * @param tree The tree that is not included in the copy
     * @return A copy of this tree without the provided tree
     */
    def -(tree: template.TreeLike[_, _]): NodeType = makeNode(content, children.filterNot { _ == tree } map { _ - tree })
    
    /**
      * Creates a new copy of this tree where specified content never occurs. Removes the content from every child,
      * including grandchildren etc.
      * @param removedContent The content to be removed
      * @return A tree without the specified content
      */
    def -(removedContent: A) = filterContents { _ != removedContent }
    
    
    // OTHER METHODS    -------------
    
    /**
     * Creates a new copy of this tree without the provided direct child node
     * @param child The child node that is removed from the direct children under this tree
     */
    def withoutChild(child: TreeLike[_, _]) = makeNode(content, children.filterNot { _ == child })
    
    /**
      * Filters all content in this tree
      * @param f A filter function
      * @return A new tree with all nodes filtered
      */
    def filterContents(f: A => Boolean): NodeType = makeNode(content, children.filter { c => f(c.content) } map { _.filterContents(f) })
    
    /**
      * Filters the direct children of this tree
      * @param f A filter function
      * @return A filtered version of this tree
      */
    def filterChildren(f: NodeType => Boolean) = makeNode(content, children.filter(f))
    
    /**
      * Replaces a node with a new version within this tree
      * @param oldNode The old node
      * @param newNode A replacement node
      * @return A copy of this tree with the node(s) replaced
      */
    def replace(oldNode: NodeType, newNode: NodeType): NodeType =
    {
        // TODO: If necessary, modify this to replace ALL instances of oldNode and not just the first
        val replacementIndex = children.indexOf(oldNode)
        if (replacementIndex >= 0)
            makeNode(content, children.updated(replacementIndex, newNode))
        else
            makeNode(content, children.map { _.replace(oldNode, newNode) })
    }
    
    /**
      * Finds a node and replaces it with a new version
      * @param find A find function used for identifying the node to be replaced
      * @param map A mapping function that produces the replacement node
      * @return A copy of this tree with the node(s) replaced
      */
    def findAndReplace(find: NodeType => Boolean, map: NodeType => NodeType): NodeType =
    {
        val replacementIndex = children.indexWhere(find)
        if (replacementIndex >= 0)
            makeNode(content, children.updated(replacementIndex, map(children(replacementIndex))))
        else
            makeNode(content, children.map { _.findAndReplace(find, map) })
    }
}