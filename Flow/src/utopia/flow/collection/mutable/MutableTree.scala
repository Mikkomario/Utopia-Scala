package utopia.flow.collection.mutable

import utopia.flow.collection.immutable
import utopia.flow.operator.equality.EqualsFunction

object MutableTree
{
    def apply[T](content: T, children: Vector[MutableTree[T]] = Vector())(implicit equals: EqualsFunction[T]) =
        new MutableTree(content, children)
    
    def apply[T](content: T, child: MutableTree[T])(implicit equals: EqualsFunction[T]) =
        new MutableTree(content, Vector(child))
    
    def apply[T](content: T, firstC: MutableTree[T], secondC: MutableTree[T], more: MutableTree[T]*)(implicit equals: EqualsFunction[T]) =
        new MutableTree(content, Vector(firstC, secondC) ++ more)
}

/**
 * Tree nodes form individual trees. They can also be used as subtrees in other tree nodes. Like 
 * other nodes, treeNodes contain / wrap certain type of content. A tree node can never contain 
 * itself below itself.
 * @param nav The contents of this node
 * @author Mikko Hilpinen
 * @since 1.11.2016
 */
class MutableTree[A](var nav: A, initialChildren: Vector[MutableTree[A]] = Vector())
                    (implicit override val navEquals: EqualsFunction[A] = EqualsFunction.default)
    extends MutableTreeLike[A, MutableTree[A]]
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
    
    override def self = this
    
    def children = _children
    
    // Creates a new child node and attaches it to this tree
    override protected def newNode(content: A) = {
        val node = new MutableTree(content)
        _children :+= node
        node
    }
    
    override protected def setChildren(newChildren: Seq[MutableTree[A]]) = _children = newChildren.toVector
}