package utopia.flow.collection.template

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.flow.collection.mutable.iterator.{BottomToTopIterator, OrderedDepthIterator, PollableOnce}
import utopia.flow.operator.equality.EqualsExtensions.ImplicitApproxEquals
import utopia.flow.operator.MaybeEmpty
import utopia.flow.operator.equality.EqualsFunction

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.immutable.VectorBuilder

object TreeLike
{
	/**
	  * Any implementation of TreeLike, regardless of the type of individual nodes
	  */
	type Tree[A] = TreeLike[A, _ <: TreeLike[A, _]]
	/**
	  * Any implementation of TreeLike, regardless of type
	  */
	type AnyTree = TreeLike[_, _]
}

/**
  * Tree nodes form individual trees. They can also be used as subtrees in other tree nodes. Like
  * other nodes, treeNodes contain / wrap certain type of content. A tree node can never contain
  * itself below itself.
  * @author Mikko Hilpinen
  * @since 1.11.2016
  * @tparam A Type of item used when navigating through this tree. May also be considered the main content of this tree.
  * @tparam Repr Types of nodes in this tree
  */
trait TreeLike[A, +Repr <: TreeLike[A, Repr]] extends MaybeEmpty[Repr]
{
	// ABSTRACT   --------------------
	
	/**
	  * @return Equality function used when navigating through tree nodes.
	  *         Used for comparing navigational ('nav') elements.
	  */
	implicit def navEquals: EqualsFunction[A]
	
	/**
	  * @return The navigation element of this tree node.
	  */
	def nav: A
	
	/**
	  * The child nodes directly under this node
	  */
	def children: Seq[Repr]
	
	/**
	  * Tests whether this node directly contains the specified content
	  * @param nav Content to test
	  * @return Whether this node's content matches the specified content
	  */
	@deprecated("Please use .nav ~== nav instead", "< v2.3")
	def containsDirect(nav: A): Boolean = navEquals(this.nav, nav)
	
	/**
	  * Creates a new (child) node
	  * @param content Content for the child node
	  * @return A new (child) node
	  */
	protected def newNode(content: A): Repr
	
	
	// COMPUTED PROPERTIES    --------
	
	/**
	  * @return Whether this tree has child nodes registered under it
	  */
	def hasChildren = children.nonEmpty
	
	/**
	  * The size of this tree. In other words, the number of nodes below this node
	  */
	def size: Int = children.foldLeft(children.size)((size, child) => size + child.size)
	
	/**
	  * The depth of this tree. A tree with no children has depth of 0, a tree with only direct
	  * children has depth of 1, a tree with grand children has depth of 2 and so on.
	  */
	def depth: Int = children.foldLeft(0)((maxDepth, child) => math.max(maxDepth, 1 + child.depth))
	
	/**
	  * @return An iterator that returns this node, every child node, their children and so on.
	  *         Each branch is fully traversed before moving to its sibling.
	  *         Starts with this node.
	  */
	def allNodesIterator: Iterator[Repr] = self +: nodesBelowIterator
	/**
	  * @return An iterator that goes over all the nodes below this node.
	  *         Will not include this node.
	  *         Each branch is fully traversed before moving to its sibling.
	  *         I.e. every child of the first child is returned before the second child of this node is returned.
	  */
	def nodesBelowIterator: Iterator[Repr] = children.iterator.flatMap { c => c +: c.nodesBelowIterator }
	/**
	  * @return All nodes that belong to this tree structure, including this node, as an iterable collection.
	  */
	def allNodes: Vector[Repr] = allNodesIterator.toVector
	/**
	  * All nodes below this node as an iterable collection
	  */
	def nodesBelow: Vector[Repr] = nodesBelowIterator.toVector
	
	/**
	  * @return An iterator that goes over all navigation elements within this tree, including this node's nav.
	  *         Each branch is fully traversed before moving to the sibling branch.
	  *         This node's nav is returned first.
	  */
	def allNavsIterator = nav +: navsBelowIterator
	/**
	  * @return An iterator that goes over all navigation elements within this tree, excluding this node's nav.
	  *         Each branch is fully traversed before moving to the sibling branch.
	  */
	def navsBelowIterator: Iterator[A] = nodesBelowIterator.map { _.nav }
	/**
	  * @return All navigational elements within this tree, including this node's nav.
	  */
	def allNavs = allNavsIterator.toVector
	
	/**
	  * @return An iterator that returns this node, then all children of this node, then all grandchildren
	  *         of the children of this node, then the children of the grandchildren, and so on.
	  *         The nodes returned by this iterator are returned in an order
	  *         from top to bottom (primary) and left to right (secondary).
	  *
	  *         If the ordering of the children is not as important, one should rather call .allNodesIterator,
	  *         as it is more memory-efficient.
	  */
	def topDownNodesIterator = self +: topDownNodesBelowIterator
	/**
	  * @return An iterator that returns all children of this node, then all grandchildren
	  *         of those children, then the children of the grandchildren, and so on.
	  *         The nodes returned by this iterator are returned in an order
	  *         from top to bottom (primary) and left to right (secondary).
	  *
	  *         If the ordering of the children is not as important, one should rather call .nodesBelowIterator,
	  *         as it is more memory-efficient.
	  */
	def topDownNodesBelowIterator: Iterator[Repr] = OrderedDepthIterator(children) { _.children }
	/**
	  * @return An ordered collection that contains all children of this node, all grandchildren
	  *         of those children, the children of the grandchildren, and so on.
	  *         The nodes returned are ordered from top to bottom first,
	  *         and then from left to right within a single "layer".
	  */
	def topDownNodesBelow: Vector[Repr] = topDownNodesBelowIterator.toVector
	/**
	  * @return An iterator that returns this node and all the children of this node,
	  *         returning leaf nodes before the rest of the branches.
	  *         I.e. A node is not returned until all of its children have been returned first.
	  *
	  *         If the ordering of the items is not important, please call [[nodesBelowIterator]] instead,
	  *         as that implementation is more more efficient in terms of memory and speed.
	  */
	def bottomToTopNodesIterator: Iterator[Repr] = BottomToTopIterator(self) { _.children }
	
	/**
	  * @return An iterator that returns all leaf nodes that appear within this tree.
	  *         Leaves are nodes that don't have any children, i.e. the "end" nodes.
	  */
	def leavesIterator = if (isEmpty) PollableOnce(self) else leavesBelowIterator
	/**
	  * @return An iterator that returns all leaf nodes that appear under this node.
	  *         This node is never included, even when it is a leaf node.
	  *         Leaves are nodes that don't have any children, i.e. the "end" nodes.
	  */
	def leavesBelowIterator = nodesBelowIterator.filter { _.isEmpty }
	/**
	  * @return The leaf nodes that appear within this tree.
	  *         Leaves are nodes that don't have any children, i.e. the "end" nodes.
	  *         If this node is empty, returns a sequence containing this node only.
	  */
	def leaves: IndexedSeq[Repr] = if (isEmpty) Single(self) else leavesBelow
	/**
	  * @return All leaf nodes that appear under this node.
	  *         This node is never included, even when it is a leaf node.
	  *         Leaves are nodes that don't have any children, i.e. the "end" nodes.
	  */
	def leavesBelow = leavesBelowIterator.toVector
	
	/**
	  * @return An iterator that returns all branches that appear within this tree.
	  *         Branches are "routes" of nodes from this node to each leaf node.
	  *         This node appears as the first item of reach resulting branch.
	  *
	  *         Please note that this iterator will yield a large number of items for large trees,
	  *         and will also use a lot of memory, as incomplete branches are buffered until they are completed.
	  *         If this node had 2 children, that each had 2 children, 4 branches of length 3 would be returned.
	  *         As the size of the tree increases, the number of resulting branches increases exponentially.
	  */
	def branchesIterator: Iterator[IndexedSeq[Repr]] = {
		if (isEmpty)
			PollableOnce(Single(self))
		else
			_branchesIterator.map { _.result().reverse }
	}
	/**
	  * @return An iterator that returns all branches that leave from this node.
	  *         Branches are "routes" of nodes from this node to each leaf node.
	  *         This node will not appear in any resulting branch.
	  *
	  *         Please note that this iterator will yield a large number of items for large trees,
	  *         and will also use a lot of memory, as incomplete branches are buffered until they are completed.
	  *         If this node had 2 children, that each had 2 children, 4 branches of length 2 would be returned.
	  *         As the size of the tree increases, the number of resulting branches increases exponentially.
	  */
	def branchesBelowIterator = children.iterator.flatMap { _.branchesIterator }
	// Returns branch builders. The branch builders are in reverse order (from leaf to root)
	// Unchecked variance because only instances of Repr are added to the resulting builders
	private def _branchesIterator: Iterator[VectorBuilder[Repr @uncheckedVariance]] = {
		// Case: This is a leaf node => Start a new reverse branch builder
		if (isEmpty) {
			val builder = new VectorBuilder[Repr]()
			builder += self
			PollableOnce(builder)
		}
		// Case: This is not a leaf node => Continues building the branches that were started below
		else
			children.iterator.flatMap { _._branchesIterator.map { b => b += self; b } }
	}
	/**
	  * @return All branches that appear within this tree.
	  *         Branches are "routes" of nodes from this node to each leaf node.
	  *         This node appears as the first item of reach resulting branch.
	  *
	  *         Please note that this collection will contain a large number of items for large trees.
	  *         If this node had 2 children, that each had 2 children, 4 branches of length 3 would be returned.
	  *         As the size of the tree increases, the number of resulting branches increases exponentially.
	  */
	def branches = branchesIterator.toVector
	/**
	  * @return All branches that leave from this node.
	  *         Branches are "routes" of nodes from this node to each leaf node.
	  *         This node will not appear in any resulting branch.
	  *
	  *         Please note that this collection will contain a large number of items for large trees.
	  *         If this node had 2 children, that each had 2 children, 4 branches of length 2 would be returned.
	  *         As the size of the tree increases, the number of resulting branches increases exponentially.
	  */
	def branchesBelow = branchesBelowIterator.toVector
	
	
	// IMPLEMENTED  ----------------
	
	/**
	  * Whether this tree is empty and doesn't contain a single node below it
	  */
	override def isEmpty = children.isEmpty
	
	override def toString: String = if (isEmpty) nav.toString else s"$nav: {${ children.mkString(", ") }}"
	
	
	// OTHER METHODS    ------------
	
	/**
	  * Finds or generates a node directly under this one
	  * @param nav The next navigation "step"
	  * @return Either an existing node or a made-up one
	  */
	def /(nav: A) = get(nav).getOrElse { newNode(nav) }
	/**
	  * @param path A path of navigational steps to take. Ordered.
	  * @return Node at the end of that path. May be generated.
	  */
	def /(path: Seq[A]): Repr = path.foldLeft(self) { _ / _ }
	/**
	  * Finds or generates a node directly under this one
	  * @param nav The next navigation "step"
	  * @return Either an existing node or a made-up one
	  */
	def apply(nav: A) = this/nav
	/**
	  * Finds or generates a node under this one
	  * @param first The next navigation "step"
	  * @param second The step after that
	  * @param more Additional steps to take
	  * @return Node at the end of the specified navigation path. May be generated.
	  */
	def apply(first: A, second: A, more: A*) = this / (Pair(first, second) ++ more)
	
	/**
	  * Finds a child directly under this node that matches the specified navigational step
	  * @param nav The navigational step to take next, if a matching node is found
	  * @return The first child that matches the specified step. None if no such (direct) child was found.
	  */
	def get(nav: A) = children.find { _.nav ~== nav }
	
	/**
	 * @param maxDepth Maximum search depth, where 1 represents the direct children under this node,
	 *                 2 represents the children of those nodes, and so on.
	 * @return An iterator that returns all nodes within this tree structure up to a certain depth level.
	 *         Won't include this node.
	 */
	def nodesBelowIteratorUpToDepth(maxDepth: Int): Iterator[Repr] = {
		// Case: Maximum depth reached already => Returns no more nodes
		if (maxDepth <= 0)
			Iterator.empty
		// Case: This level is the last level => Returns direct children
		else if (maxDepth == 1)
			children.iterator
		// Case: More levels included => Uses recursion
		else
			children.iterator.flatMap { c => c +: c.nodesBelowIteratorUpToDepth(maxDepth - 1) }
	}
	
	/**
	  * @param node Searched node
	  * @return Whether this tree structure contains that node
	  */
	def containsNode(node: Any): Boolean =  nodesBelowIterator.contains(node)
	/**
	  * @param nav A navigational element
	  * @return Whether this node or any of this node's children contains the specified navigational element
	  */
	def containsNav(nav: A): Boolean = (this.nav ~== nav) || navsBelowIterator.exists { _ ~== nav }
	
	/**
	 * @param filter A filter/search function
	 * @return An iterator that returns all top level nodes which satisfy the specified filter.
	 *         Top level means that children of the returned nodes are not included separately.
	 *        If this node satisfies the specified predicate, returns this node only.
	 */
	def rootsWhereIterator(filter: Repr => Boolean) =
		if (filter(self)) Iterator.single(self) else rootsBelowWhereIterator(filter)
	/**
	 * @param filter A filter/search function
	 * @return An iterator that returns all top level nodes which satisfy the specified filter.
	 *         Top level means that children of the returned nodes are not included separately.
	 *         Will never include this node.
	 */
	def rootsBelowWhereIterator(filter: Repr => Boolean): Iterator[Repr] = {
		children.iterator.flatMap { n =>
			// Case: The child matches the filter function => Accepts it and won't go deeper
			if (filter(n))
				Some(n)
			// Case: Child not accepted => Checks whether any node below is
			else
				n.rootsBelowWhereIterator(filter)
		}
	}
	
	/**
	  * Finds the first child node from this entire tree that matches the specified condition.
	  * Returns the path to that node.
	  * @param filter A search condition
	  * @return Path to the first node matching the specified condition.
	  *         None if no such node was found.
	  *         The path contains all nodes that need to be traversed in order to reach the target node.
	  *         The node that first fulfilled the specified search condition always lies at the end of the path.
	  *         This node is always located at the beginning of the resulting path.
	  */
	def findWithPath(filter: Repr => Boolean): Option[IndexedSeq[Repr]] = {
		if (filter(self))
			Some(Single(self))
		else
			children.findMap { _.findWithPath(filter) }.map { self +: _ }
	}
	/**
	 * Finds the top nodes within this tree (whether this node, direct children or grandchildren etc.) that satisfy the
	 * specified filter. Includes the "path" to all of the selected nodes as well.
	 * If a node is selected, it's children are not tested anymore.
	 * @param filter A filter function
	 * @return Returns paths to all of the nodes that satisfy the specified filter function.
	 *         Every path will start with this node and end with the node that fulfilled the specified function.
	 *         If this node fulfills the specified function, returns a single path consisting only of this node.
	 *         The result is lazily computed and cached.
	 */
	def pathsToRootsWhereIterator(filter: Repr => Boolean) = _filterWithPaths(filter).map { _.result().reverse }
	/**
	 * Finds the top nodes within this tree (whether this node, direct children or grandchildren etc.) that satisfy the
	 * specified filter. Includes the "path" to all of the selected nodes as well.
	 * If a node is selected, it's children are not tested anymore.
	 * @param filter A filter function
	 * @return An iterator that returns paths to all of the nodes that satisfy the specified filter function.
	 *         Every path will start with this node and end with the node that fulfilled the specified function.
	 *         If this node fulfills the specified function, returns a single path consisting only of this node.
	 */
	def pathsToRootsWhere(filter: Repr => Boolean) = pathsToRootsWhereIterator(filter).caching
	/**
	  * Finds the top nodes under this node (whether they be direct children or grandchildren etc.) that satisfy the
	  * specified filter. Includes the "path" to all of the selected nodes as well.
	  * If a node is selected, it's children are not tested anymore.
	  * @param filter A filter function
	  * @return Paths to all of the nodes that satisfy the specified filter function.
	  *         Every path will start with this node and end with the node that fulfilled the specified function.
	  *         If this node fulfills the specified function, returns a single path consisting only of this node.
	  *         The result is lazily computed and cached.
	  */
	@deprecated("Please use .pathsToRootsWhere(...) instead, as the term filter is ambiguous in this setting", "v2.4")
	def filterWithPaths(filter: Repr => Boolean) = pathsToRootsWhere(filter)
	private def _filterWithPaths(filter: Repr => Boolean): Iterator[VectorBuilder[Repr @uncheckedVariance]] = {
		// Case: This node represents a search result => Starts a new branch to it
		if (filter(self)) {
			val builder = new VectorBuilder[Repr]()
			builder += self
			Iterator.single(builder)
		}
		// Case: This node is not a search result => looks from below and builds the paths if found
		else
			children.iterator.flatMap { _._filterWithPaths(filter) }.map { _ += self }
	}
	/**
	  * Finds The location of a specific nav element within this tree structure.
	  * Assumes that this tree contains unique nav elements.
	  * @param nav Searched nav element
	  * @return A path to a node containing the specified nav element.
	  *         Returns None if there doesn't exist any node in this tree with the specified nav element.
	  *         The path consists of actual nodes that need to be traversed.
	  *         This node is always the first element in the returned path.
	  *         The node containing the specified nav element is located at the end of this path.
	  */
	def pathTo(nav: A) = if (this.nav ~== nav) Some(Empty) else findWithPath { _.nav ~== nav }
}
