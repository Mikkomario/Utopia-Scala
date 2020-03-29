package utopia.flow.datastructure.immutable

/**
 * This TreeNode implementation is immutable and safe to reference from multiple places
 * @author Mikko Hilpinen
 * @since 4.11.2016
 */
case class Tree[A](override val content: A, override val children: Vector[Tree[A]] = Vector()) extends TreeLike[A, Tree[A]]
{
    // IMPLEMENTED    ---------------
    
    override protected def makeNode(content: A, children: Vector[Tree[A]]) = Tree(content, children)
    
    
    // OTHER METHODS    -------------
    
    /**
      * Maps the content of all nodes in this tree
      * @param f A mapping function
      * @tparam B Target content type
      * @return A mapped version of this tree
      */
    def map[B](f: A => B): Tree[B] = Tree(f(content), children.map { _.map(f) })
    
    /**
      * Maps the content of all nodes in this tree. May map to None where the node is removed.
      * @param f A mapping function
      * @tparam B Target content type
      * @return A mapped version of this tree. None if the content of this node mapped to None.
      */
    def flatMap[B](f: A => Option[B]): Option[Tree[B]] = f(content).map { c => Tree(c, children.flatMap { _.flatMap(f) }) }
}