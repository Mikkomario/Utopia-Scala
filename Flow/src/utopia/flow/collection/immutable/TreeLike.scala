package utopia.flow.collection.immutable

import utopia.flow.collection.template
import utopia.flow.collection.template.TreeLike.AnyTree
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.operator.equality.{ApproxSelfEquals, EqualsFunction}
import utopia.flow.operator.equality.EqualsExtensions._
/**
  * A common trait for immutable trees
  * @author Mikko Hilpinen
  * @since 4.11.2016
  */
trait TreeLike[A, Repr <: TreeLike[A, Repr]] extends template.TreeLike[A, Repr] with ApproxSelfEquals[Repr]
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
	def withoutChildren = createCopy(children = Empty)
	
	
	// IMPLEMENTED  ------------
	
	override implicit def equalsFunction: EqualsFunction[Repr] =
		EqualsFunction[Repr] { (a, b) => (a.nav ~== b.nav) && a.children.hasEqualContentWith(b.children)(equalsFunction) }
	
	
	// OTHER    ----------------
	
	/**
	  * Creates a copy of this tree that contains the specified tree as one of its child nodes.
	  * If this node already contains a matching child, merges it with this new node so that the children of both
	  * are kept.
	  * @param tree A child node to insert
	  * @return a copy of this tree with the specified child included / merged in
	  */
	def :+(tree: Repr) =
		createCopy(children = children.mergeOrAppend(tree) { _.nav ~== tree.nav } { _ :++ _.children })
	/**
	  * Creates a new tree that contains a child node with specified content.
	  * If this node already contains such a child, doesn't add a new one.
	  * @param nodeContent Child node content / nav
	  * @return A copy of this tree including a child with that content / nav
	  */
	def :+(nodeContent: A): Repr = {
		// Case: This node already contains that child => ignores
		if (children.exists { _.nav ~== nodeContent })
			self
		// Case: New child => inserts
		else
			createCopy(children = children :+ newNode(nodeContent))
	}
	
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
	  * Merges these two trees together, if possible
	  * @param other Another tree
	  * @return Either:
	  *         Left: This tree and the other tree, if there is no common root
	  *         Right: A combination of these trees where all the children of the specified tree are included
	  *         in this tree. Only returned if these trees have equal root nav.
	  */
	def +(other: Repr) = {
		// Case: Identical root => merges children
		if (nav ~== other.nav)
			Right(this :++ other.children)
		// Case: Different roots => returns as separate trees
		else
			Left(Pair(self, other))
	}
	/**
	  * Merges this tree with another set of trees, where possible.
	  * The roots that have equal nav elements are merged together.
	  * @param other Other trees
	  * @return Merged trees. One entry for each unique root nav element.
	  */
	def ++(other: IterableOnce[Repr]) = {
		// Combines the trees by nav, where possible (one tree at a time)
		other.iterator.foldLeft(Single(self): IndexedSeq[Repr]) { (existingTrees, newTree) =>
			existingTrees.mergeOrAppend(newTree) { _.nav ~== newTree.nav } { _ :++ _.children }
		}
	}
	/**
	  * Adds new children to this node using merging.
	  * If there already exist children that have equal nav elements, they are combined with the newly introduced
	  * children, yielding a single node per unique nav element, containing the children from both versions, merged
	  * @param newChildren New children to merge into this tree
	  * @return A copy of this tree that includes the new child tree structures
	  */
	def :++(newChildren: IterableOnce[Repr]): Repr = {
		val iter = newChildren.iterator
		// Case: There are actually some children to add => merges them in
		if (iter.hasNext)
			// Merges one new child at once
			createCopy(children = iter.foldLeft(children) { (existingTrees, newTree) =>
				existingTrees.mergeOrAppend(newTree) { _.nav ~== newTree.nav } { _ :++ _.children }
			})
		// Case: No children to add => No operation
		else
			self
	}
	
	/**
	  * @param child A new child node
	  * @return A copy of this tree with that child node added
	  */
	def withChildAdded(child: Repr) = createCopy(children = children :+ child)
	/**
	  * @param newChildren New child nodes
	  * @return A copy of this tree with those child nodes added inder it
	  */
	def withChildrenAdded(newChildren: IterableOnce[Repr]) = createCopy(children = children ++ newChildren)
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
				if (path hasSize 1)
					mapChildren { c => if (c.nav ~== nextStep) f(c) else c }
				else {
					val remaining = path.tail
					mapChildren { c => if (c.nav ~== nextStep) c.mapPath(remaining)(f) else c }
				}
			case None => f(self)
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
	def mapPath(start: A, next: A, more: A*)(f: Repr => Repr): Repr = mapPath(Pair(start, next) ++ more)(f)
	
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
	  * @param node A node to search
	  * @return Whether this tree structure contains the specified node, or a node very similar to it
	  */
	def contains(node: Repr) = allNodesIterator.exists { _ ~== node }
	/**
	  * @param node A searched node
	  * @return Path to a node within this tree that's equal to the specified node.
	  *         None if this tree doesn't contain such a node.
	  *         The resulting path will always start with this node and end with a node equal to the specified node.
	  */
	def pathTo(node: Repr) = findWithPath { _ ~== node }
	
	/**
	  * Maps all (top level) nodes within this tree that satisfy the specified search condition
	  * @param find A function that tells whether a node should be mapped (true) or not (false).
	  * @param map A mapping function
	  * @return A copy of this tree where all (highest) nodes that satisfy the specified search
	  *         condition have been mapped.
	  */
	def mapAllWhere(find: Repr => Boolean)(map: Repr => Repr): Repr = {
		if (find(self))
			map(self)
		else
			createCopy(children  = children.map { _.mapAllWhere(find)(map) })
	}
	/**
	  * Maps the first node that satisfies the specified search condition
	  * @param find A search function that yields true for the node to map
	  * @param map A mapping function applied to the found node
	  * @return Either:
	  *             Left: This tree, if no node satisfied the specified search condition, or
	  *             Right: A copy of this tree with a single node mapped
	  */
	def mapFirstWhere(find: Repr => Boolean)(map: Repr => Repr) =
		_mapFirstWhere(find)(map).toRight(self)
	private def _mapFirstWhere(find: Repr => Boolean)(map: Repr => Repr): Option[Repr] = {
		if (find(self))
			Some(map(self))
		else
			children.iterator.zipWithIndex.findMap { case (c, i) =>
				c._mapFirstWhere(find)(map).map { c2 => createCopy(children = children.updated(i, c2)) }
			}
	}
	
	/**
	  * Replaces a single branch within this tree with the specified branch, based on the branch root nav element.
	  * @param newBranch A tree to replace an existing branch with
	  * @return Either:
	  *             Left: This tree, if it didn't contain a node that could be replaced with the specified tree
	  *             Right: A copy of this tree where the highest node
	  *             with a nav element matching that of the specified node has been replaced with the specified node
	  */
	def replaceBranch(newBranch: Repr) = mapFirstWhere { _.nav ~== newBranch.nav } { _ => newBranch }
	/**
	  * Merges a branch into this tree at the first node that has a matching nav element
	  * as the root of the specified branch.
	  * When merging the branch into this tree, preserves all nodes of this tree and adds missing nodes from
	  * the specified branch.
	  * @param branch A branch to merge into this tree, if possible
	  * @return Either:
	  *             Left: This tree if it didn't contain a node with a nav element
	  *             matching the root of the specified branch, or
	  *             Right: A copy of this tree with the specified branch merged with the first node that had a
	  *             matching nav element.
	  */
	def mergeBranch(branch: Repr) = {
		if (branch.hasChildren)
			mapFirstWhere { _.nav ~== branch.nav } { _ :++ branch.children }
		else if (containsNav(branch.nav))
			Right(self)
		else
			Left(self)
	}
}
