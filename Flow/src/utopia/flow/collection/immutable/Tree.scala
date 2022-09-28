package utopia.flow.collection.immutable

import utopia.flow.operator.EqualsFunction

/**
  * This TreeNode implementation is immutable and safe to reference from multiple places
  * @author Mikko Hilpinen
  * @since 4.11.2016
  */
case class Tree[A](override val nav: A, override val children: Vector[Tree[A]] = Vector())
                  (implicit override val navEquals: EqualsFunction[A] = EqualsFunction.default)
	extends TreeLike[A, Tree[A]]
{
	// IMPLEMENTED    ---------------
	
	override def repr = this
	
	override protected def createCopy(content: A, children: Seq[Tree[A]]) = Tree(content, children.toVector)
	
	override protected def newNode(content: A) = Tree(content)
	
	
	// OTHER METHODS    -------------
	
	/**
	  * Maps the content of all nodes in this tree
	  * @param f A mapping function
	  * @tparam B Target content type
	  * @return A mapped version of this tree
	  */
	def map[B](f: A => B)(implicit equals: EqualsFunction[B]): Tree[B] =
		Tree(f(nav), children.map { _.map(f) })(equals)
	
	/**
	  * Maps the content of all nodes in this tree. May map to None where the node is removed.
	  * @param f A mapping function
	  * @tparam B Target content type
	  * @return A mapped version of this tree. None if the content of this node mapped to None.
	  */
	def flatMap[B](f: A => Option[B])(implicit equals: EqualsFunction[B]): Option[Tree[B]] =
		f(nav).map { c => Tree(c, children.flatMap { _.flatMap(f) })(equals) }
}
