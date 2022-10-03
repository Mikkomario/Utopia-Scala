package utopia.flow.collection.immutable

import utopia.flow.collection.template
import utopia.flow.collection.template.TreeLike.AnyTree
import utopia.flow.operator.EqualsExtensions.ApproxEquals

/**
  * A common trait for immutable trees
  * @author Mikko Hilpinen
  * @since 4.11.2016
  */
trait TreeLike[A, Repr <: TreeLike[A, Repr]] extends template.TreeLike[A, Repr]
{
	// ABSTRACT --------------------
	
	/**
	  * Creates a copy of this tree
	  * @param nav  New navigational element to assign to this node (default = current nav)
	  * @param children New children to assign (default = current children)
	  * @return A (modified) copy of this node
	  */
	protected def createCopy(nav: A = nav, children: Seq[Repr] = children): Repr
	
	
	// COMPUTED --------------------
	
	/**
	  * @return A copy of this tree without any child nodes included
	  */
	def withoutChildren = createCopy(children = Vector[Repr]())
	
	
	// OTHER    ----------------
	
	/**
	  * Creates a copy of this tree that contains a new child node
	  * @param tree A child node to add
	  * @return a copy of this tree with the provided child tree added
	  */
	def +(tree: Repr) = createCopy(children = children :+ tree)
	/**
	  * Creates a new tree that contains a child node with specified content
	  * @param nodeContent Child node content
	  * @return A copy of this tree with child node added
	  */
	def +(nodeContent: A): Repr = this + newNode(nodeContent)
	
	/**
	  * Creates a new copy of this tree where the specified tree doesn't occur anywhere.
	  * @param tree The tree that is not included in the copy
	  * @return A copy of this tree without the provided tree
	  */
	def -(tree: AnyTree): Repr =
		createCopy(children = children.filterNot { _ == tree } map { _ - tree })
	/**
	  * Creates a new copy of this tree where specified navigational element never occurs.
	  * Removes the content from every child, including grand children etc.
	  * @param removedNav The navigational element to be removed
	  * @return A tree without the nodes matching the specified navigational element
	  */
	def -(removedNav: A): Repr =
		createCopy(children = children.filterNot { _.nav ~== removedNav }.map { _ - removedNav })
	
	/**
	  * Creates a new copy of this tree without the provided direct child node
	  * @param child The child node that is removed from the direct children under this tree
	  */
	def withoutChild(child: AnyTree) = createCopy(children = children.filterNot { _ == child })
	/**
	  * Creates a copy of this tree without direct children matching the specified navigational element
	  * @param nav A navigational element to exclude directly from under this node
	  * @return A copy of this tree without that nav item included in the direct children
	  */
	def withoutDirect(nav: A) = createCopy(children = children.filterNot { _.nav ~== nav })
	
	/**
	  * Creates a copy of this node with modified direct child nodes
	  * @param f A function that accepts the child nodes of this node and returns a modified copy
	  * @return A modified copy of this node
	  */
	def modifyChildren(f: Seq[Repr] => Seq[Repr]): Repr = createCopy(children = f(children))
	/**
	  * Creates a copy of this node with its direct children mapped
	  * @param f A mapping function for direct child nodes
	  * @return A mapped copy of this node
	  */
	def mapChildren(f: Repr => Repr) = createCopy(children = children.map(f))
	
	/**
	  * Maps children which are reachable using the specified content path
	  * @param path path to the child or children being targeted, where each item represents targeted content.
	  *             An empty path represents this node.
	  * @param f    A mapping function that modifies the node(s) at the end of the specified path
	  * @return A modified copy of this tree
	  */
	def mapPath(path: Seq[A])(f: Repr => Repr): Repr = {
		path.headOption match {
			case Some(nextStep) =>
				if (path.size == 1)
					mapChildren { c => if (c.nav ~== nextStep) f(c) else c }
				else {
					val remaining = path.tail
					mapChildren { c => if (c.nav ~== nextStep) c.mapPath(remaining)(f) else c }
				}
			case None => f(repr)
		}
	}
	/**
	  * Maps children which are reachable using the specified content path
	  * @param start First step (content) on the path
	  * @param next  The next step (content) on the path
	  * @param more  Additional steps
	  * @param f     A mapping function that modifies the node(s) at the end of the specified path
	  * @return A modified copy of this tree
	  */
	def mapPath(start: A, next: A, more: A*)(f: Repr => Repr): Repr = mapPath(Vector(start, next) ++ more)(f)
	
	/**
	  * Filters the children directly under this node
	  * @param f A function that determines whether a direct child should be kept attached to this node
	  * @return A filtered copy of this tree
	  */
	def filterDirect(f: Repr => Boolean) = createCopy(children = children.filter(f))
	/**
	  * Filters this whole tree structure using the specified filter function
	  * @param f A filtering function that determines which nodes should be kept
	  * @return A filtered copy of this tree
	  */
	def filter(f: Repr => Boolean): Repr = createCopy(children = children.filter(f).map { _.filter(f) })
	/**
	  * Filters the children directly under this node
	  * @param f A function that determines whether a direct child should be kept attached to this node.
	  *          Accepts the navigational element representing a child tree.
	  * @return A filtered copy of this tree
	  */
	def filterDirectByNav(f: A => Boolean) = filterDirect { n => f(n.nav) }
	/**
	  * Filters this whole tree structure using the specified filter function
	  * @param f A filtering function that determines which nodes should be kept.
	  *          Accepts the navigational element representing a node.
	  * @return A filtered copy of this tree
	  */
	def filterByNav(f: A => Boolean) = filter { n => f(n.nav) }
	/**
	  * Filters all content in this tree
	  * @param f A filter function
	  * @return A new tree with all nodes filtered
	  */
	@deprecated("Replaced with .filterByNav(...)", "v2.0")
	def filterContents(f: A => Boolean): Repr = filterByNav(f)
	/**
	  * Filters the direct children of this tree
	  * @param f A filter function
	  * @return A filtered version of this tree
	  */
	@deprecated("Replaced with .filterDirect(...)", "v2.0")
	def filterChildren(f: Repr => Boolean) = filterDirect(f)
}