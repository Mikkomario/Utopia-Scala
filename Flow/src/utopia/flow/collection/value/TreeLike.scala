package utopia.flow.collection.value

import utopia.flow.collection

/**
  * This TreeNode implementation is immutable and safe to reference from multiple places
  * @author Mikko Hilpinen
  * @since 4.11.2016
  */
trait TreeLike[A, NodeType <: TreeLike[A, NodeType]] extends collection.template.TreeLike[A, NodeType]
{
	// ABSTRACT --------------------
	
	/**
	  * @return "This" node
	  */
	def repr: NodeType
	
	/**
	  * Creates a copy of this node
	  * @param content  New content to assign (default = current content)
	  * @param children New children to assign (default = current children)
	  * @return A (modified) copy of this node
	  */
	protected def createCopy(content: A = content, children: Vector[NodeType] = children): NodeType
	
	
	// COMPUTED --------------------
	
	/**
	  * @return A copy of this tree without any child nodes included
	  */
	def withoutChildren = createCopy(children = Vector())
	
	
	// OPERATORS    ----------------
	
	/**
	  * Creates a new tree that contains a new child node
	  * @param tree The child node in the new tree
	  * @return a copy of this tree with the provided child tree
	  */
	def +(tree: NodeType) = createCopy(children = children :+ tree)
	
	/**
	  * Creates a new tree that contains a child node with specified content
	  * @param nodeContent Child node content
	  * @return A copy of this tree with child node added
	  */
	def +(nodeContent: A): NodeType = this + newNode(nodeContent)
	
	/**
	  * Creates a new copy of this tree where the provided tree doesn't occur. Removes the element from even child
	  * trees
	  * @param tree The tree that is not included in the copy
	  * @return A copy of this tree without the provided tree
	  */
	def -(tree: collection.template.TreeLike[_, _]): NodeType = createCopy(children = children.filterNot { _ == tree } map { _ - tree })
	
	/**
	  * Creates a new copy of this tree where specified content never occurs. Removes the content from every child,
	  * including grandchildren etc.
	  * @param removedContent The content to be removed
	  * @return A tree without the specified content
	  */
	def -(removedContent: A) = filterChildren { !_.containsDirect(removedContent) }
	
	
	// OTHER METHODS    -------------
	
	/**
	  * Creates a new copy of this tree without the provided direct child node
	  * @param child The child node that is removed from the direct children under this tree
	  */
	def withoutChild(child: TreeLike[_, _]) = createCopy(children = children.filterNot { _ == child })
	
	/**
	  * Creates a copy of this node with the direct child nodes modified
	  * @param f A function that accepts the child nodes of this node and returns a modified list
	  * @return A modified copy of this node
	  */
	def modifyChildren(f: Vector[NodeType] => Vector[NodeType]) = createCopy(children = f(children))
	
	/**
	  * Creates a copy of this node with its direct children mapped
	  * @param f A mapping function for direct child nodes
	  * @return A mapped copy of this node
	  */
	def mapChildren(f: NodeType => NodeType) = createCopy(children = children.map(f))
	
	/**
	  * Maps children which are reachable using the specified content path
	  * @param path path to the child or children being targeted, where each item represents targeted content.
	  *             An empty path represents this node.
	  * @param f    A mapping function that modifies the node(s) at the end of the specified path
	  * @return A modified copy of this tree
	  */
	def mapPath(path: Seq[A])(f: NodeType => NodeType): NodeType = {
		path.headOption match {
			case Some(nextStep) =>
				if (path.size == 1)
					mapChildren { c => if (c.containsDirect(nextStep)) f(c) else c }
				else {
					val remaining = path.tail
					mapChildren { c => if (c.containsDirect(nextStep)) c.mapPath(remaining)(f) else c }
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
	def mapPath(start: A, next: A, more: A*)(f: NodeType => NodeType): NodeType =
		mapPath(Vector(start, next) ++ more)(f)
	
	/**
	  * Filters all content in this tree
	  * @param f A filter function
	  * @return A new tree with all nodes filtered
	  */
	def filterContents(f: A => Boolean): NodeType =
		createCopy(children = children.filter { c => f(c.content) } map { _.filterContents(f) })
	
	/**
	  * Filters the direct children of this tree
	  * @param f A filter function
	  * @return A filtered version of this tree
	  */
	def filterChildren(f: NodeType => Boolean) = createCopy(children = children.filter(f))
	
	/**
	  * Replaces a node with a new version within this tree
	  * @param oldNode The old node
	  * @param newNode A replacement node
	  * @return A copy of this tree with the node(s) replaced
	  */
	@deprecated("This function behaves unpredictably and will be removed", "v1.15")
	def replace(oldNode: NodeType, newNode: NodeType): NodeType =
	{
		val replacementIndex = children.indexOf(oldNode)
		if (replacementIndex >= 0)
			createCopy(children = children.updated(replacementIndex, newNode))
		else
			createCopy(children = children.map { _.replace(oldNode, newNode) })
	}
	
	/**
	  * Finds a node and replaces it with a new version
	  * @param find A find function used for identifying the node to be replaced
	  * @param map  A mapping function that produces the replacement node
	  * @return A copy of this tree with the node(s) replaced
	  */
	@deprecated("This function behaves in an unpredictable manner and will be removed", "v1.15")
	def findAndReplace(find: NodeType => Boolean, map: NodeType => NodeType): NodeType =
	{
		val replacementIndex = children.indexWhere(find)
		if (replacementIndex >= 0)
			createCopy(children = children.updated(replacementIndex, map(children(replacementIndex))))
		else
			createCopy(children = children.map { _.findAndReplace(find, map) })
	}
}
