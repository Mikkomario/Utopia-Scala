package utopia.flow.collection.mutable

import utopia.flow.collection.immutable
import utopia.flow.operator.EqualsFunction

object Tree
{
    def apply[T](content: T, children: Vector[Tree[T]] = Vector())(implicit equals: EqualsFunction[T]) =
        new Tree(content, children)
    
    def apply[T](content: T, child: Tree[T])(implicit equals: EqualsFunction[T]) =
        new Tree(content, Vector(child))
    
    def apply[T](content: T, firstC: Tree[T], secondC: Tree[T], more: Tree[T]*)(implicit equals: EqualsFunction[T]) =
        new Tree(content, Vector(firstC, secondC) ++ more)
}

/**
 * Tree nodes form individual trees. They can also be used as subtrees in other tree nodes. Like 
 * other nodes, treeNodes contain / wrap certain type of content. A tree node can never contain 
 * itself below itself.
 * @param nav The contents of this node
 * @author Mikko Hilpinen
 * @since 1.11.2016
 */
class Tree[A](var nav: A, initialChildren: Vector[Tree[A]] = Vector())
             (implicit override val navEquals: EqualsFunction[A] = EqualsFunction.default)
    extends TreeLike[A, Tree[A]]
{
    // ATTRIBUTES    -----------------
    
    private var _children = initialChildren
    
    
    // COMP. PROPERTIES    -----------
    
    /**
     * Creates an immutable copy of this tree
     * @return An immutable copy of this tree
     */
    def immutableCopy: immutable.Tree[A] = immutable.Tree(nav, children.map { _.immutableCopy })
    
    
    // IMPLEMENTED PROPERTIES    -----
    
    override def repr = this
    
    def children = _children
    
    // Creates a new child node and attaches it to this tree
    override protected def newNode(content: A) = {
        val node = new Tree(content)
        _children :+= node
        node
    }
    
    override protected def setChildren(newChildren: Seq[Tree[A]]) = _children = newChildren.toVector
}