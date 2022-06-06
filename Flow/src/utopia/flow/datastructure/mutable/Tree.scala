package utopia.flow.datastructure.mutable

import utopia.flow.datastructure.immutable

object Tree
{
    def apply[T](content: T, children: Vector[Tree[T]] = Vector()) = new Tree(content, children)
    
    def apply[T](content: T, child: Tree[T]) = new Tree(content, Vector(child))
    
    def apply[T](content: T, firstC: Tree[T], secondC: Tree[T], more: Tree[T]*) = new Tree(content,
        Vector(firstC, secondC) ++ more)
}

/**
 * Tree nodes form individual trees. They can also be used as subtrees in other tree nodes. Like 
 * other nodes, treeNodes contain / wrap certain type of content. A tree node can never contain 
 * itself below itself.
 * @param content The contents of this node
 * @author Mikko Hilpinen
 * @since 1.11.2016
 */
class Tree[A](var content: A, initialChildren: Vector[Tree[A]] = Vector()) extends TreeLike[A, Tree[A]]
{
    // ATTRIBUTES    -----------------
    
    private var _children = initialChildren
    
    
    // COMP. PROPERTIES    -----------
    
    /**
     * Creates an immutable copy of this tree
     * @return An immutable copy of this tree
     */
    def immutableCopy: immutable.Tree[A] = immutable.Tree(content, children.map { _.immutableCopy })
    
    
    // IMPLEMENTED PROPERTIES    -----
    
    def children = _children
    
    // Creates a new child node and attaches it to this tree
    override protected def newNode(content: A) = {
        val node = new Tree(content)
        _children :+= node
        node
    }
    
    override protected def setChildren(newChildren: Vector[Tree[A]]) = _children = newChildren
}