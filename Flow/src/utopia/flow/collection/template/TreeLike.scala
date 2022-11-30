package utopia.flow.collection.template

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.mutable.iterator.{OrderedDepthIterator, PollableOnce}
import utopia.flow.operator.EqualsExtensions.ImplicitApproxEquals
import utopia.flow.operator.{EqualsFunction, MaybeEmpty}

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
	@deprecated("Please use .nav ~== nav instead")
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
	def allNodesIterator: Iterator[Repr] = repr +: nodesBelowIterator
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
	@deprecated("Replaced with .allNavsIterator", "v2.0")
	def allContentIterator = allNavsIterator
	/**
	  * @return An iterator that goes over all navigation elements within this tree, excluding this node's nav.
	  *         Each branch is fully traversed before moving to the sibling branch.
	  */
	def navsBelowIterator: Iterator[A] = nodesBelowIterator.map { _.nav }
	@deprecated("Replaced with .navsBelowIterator", "v2.0")
	def contentBelowIterator = navsBelowIterator
	/**
	  * @return All navigational elements within this tree, including this node's nav.
	  */
	def allNavs = allNavsIterator.toVector
	@deprecated("Replaced with .allNavs", "v2.0")
	def allContent = allNavs
	
	/**
	  * @return An iterator that returns this node, then all children of this node, then all grandchildren
	  *         of the children of this node, then the children of the grandchildren, and so on.
	  *         The nodes returned by this iterator are returned in an order
	  *         from top to bottom (primary) and left to right (secondary).
	  *         If the ordering of the children is not as important, one should rather call .allNodesIterator,
	  *         as it is more memory-efficient.
	  */
	def topDownNodesIterator = repr +: topDownNodesBelowIterator
	/**
	  * @return An iterator that returns all children of this node, then all grandchildren
	  *         of those children, then the children of the grandchildren, and so on.
	  *         The nodes returned by this iterator are returned in an order
	  *         from top to bottom (primary) and left to right (secondary).
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
	  * @return All nodes below this node, ordered based on depth (primary) and horizontal index (secondary)
	  */
	@deprecated("Please use .topDownNodesBelow instead", "v2.0")
	def nodesBelowOrdered: Vector[Repr] =
	{
		val resultBuilder = new VectorBuilder[Repr]()
		var nextNodes = children
		while (nextNodes.nonEmpty) {
			resultBuilder ++= nextNodes
			nextNodes = nextNodes.flatMap { _.children }
		}
		resultBuilder.result()
	}
	
	/**
	  * @return An iterator that returns all leaf nodes that appear within this tree.
	  *         Leaves are nodes that don't have any children, i.e. the "end" nodes.
	  */
	def leavesIterator = if (isEmpty) PollableOnce(repr) else leavesBelowIterator
	/**
	  * @return An iterator that returns all leaf nodes that appear under this node.
	  *         This node is never included, even when it is a leaf node.
	  *         Leaves are nodes that don't have any children, i.e. the "end" nodes.
	  */
	def leavesBelowIterator = nodesBelowIterator.filter { _.isEmpty }
	/**
	  * @return The leaf nodes that appear within this tree.
	  *         Leaves are nodes that don't have any children, i.e. the "end" nodes.
	  *         If this node is empty, returns a vector containing this node only.
	  */
	def leaves: Vector[Repr] = if (isEmpty) Vector(repr) else leavesBelow
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
	def branchesIterator = {
		if (isEmpty)
			PollableOnce(Vector(repr))
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
			builder += repr
			PollableOnce(builder)
		}
		// Case: This is not a leaf node => Continues building the branches that were started below
		else
			children.iterator.flatMap { _._branchesIterator.map { b => b += repr; b } }
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
	/**
	  * @return List of all "branches" <b>under</b> this node. Eg. If this node contains two children each of which
	  *         have two children themselves, returns <b>4</b> vectors that each have a size of 2. The vectors only contain
	  *         node content, not the nodes themselves. <b>This node is not included in any of the returned vectors</b>.
	  *         In other words, the resulting number of vectors is the same as the number of leaves in this tree and
	  *         the depth of each vector matches the length of each branch, including that leaf.
	  */
	@deprecated("Please use .branchesBelow instead", "v2.0")
	def allBranches: Iterable[Vector[A]] =
	{
		// Lists branches starting from each of this tree's children (includes children in the branches they found)
		children.flatMap { child =>
			// Leaves form the ends of the branches
			if (child.hasChildren)
				child.allBranches.map { branch => child.nav +: branch }
			else
				Vector(Vector(child.nav))
		}
	}
	
	
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
	def /(path: Seq[A]): Repr = path.foldLeft(repr) { _ / _ }
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
	def apply(first: A, second: A, more: A*) = this / (Vector(first, second) ++ more)
	
	/**
	  * Finds a child directly under this node that matches the specified navigational step
	  * @param nav The navigational step to take next, if a matching node is found
	  * @return The first child that matches the specified step. None if no such (direct) child was found.
	  */
	def get(nav: A) = children.find { _.nav ~== nav }
	
	/**
	  * Performs a search over the whole tree structure
	  * @param filter A predicate for finding a node
	  * @return The first child that satisfies the predicate. None if no such child was found.
	  */
	@deprecated("Please use .nodesBelowIterator.find(...) or other such combination", "v2.0")
	def find(filter: Repr => Boolean): Option[Repr] =
		children.find(filter) orElse children.findMap { _.find(filter) }
	
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
	@deprecated("Replaced with .containsNav(A)", "v2.0")
	def contains(content: A): Boolean = containsNav(content)
	
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
	def findWithPath(filter: Repr => Boolean): Option[Vector[Repr]] = {
		if (filter(repr))
			Some(Vector(repr))
		else
			children.findMap { _.findWithPath(filter) }.map { repr +: _ }
	}
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
	def filterWithPaths(filter: Repr => Boolean) = _filterWithPaths(filter).map { _.result().reverse }.caching
	private def _filterWithPaths(filter: Repr => Boolean): Iterator[VectorBuilder[Repr @uncheckedVariance]] = {
		// Case: This node represents a search result => Starts a new branch to it
		if (filter(repr)) {
			val builder = new VectorBuilder[Repr]()
			builder += repr
			Iterator.single(builder)
		}
		// Case: This node is not a search result => looks from below and builds the paths if found
		else
			children.iterator.flatMap { _._filterWithPaths(filter) }.map { _ += repr }
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
	def pathTo(nav: A) = if (this.nav ~== nav) Some(Vector()) else findWithPath { _.nav ~== nav }
	
	/**
	  * Finds the highest level branches from this tree which satisfy the specified condition. Will not test the
	  * child nodes of the accepted branches. Therefore there is no overlap in the results
	  * (no node will be listed twice). Won't test or include this root node; Only targets children, grandchildren etc.
	  * @param filter A function that tests a branch to see whether it should be accepted in the result
	  * @return All the branches which were accepted by the specified filter.
	  */
	@deprecated("Deprecated for removal. This function didn't work correctly, yields wrong type of result and is generally ambiguous", "v2.0")
	def findBranches(filter: Repr => Boolean): Iterable[Repr] = {
		children.flatMap { child =>
			if (filter(child))
				Vector(child)
			else
				child.findBranches(filter)
		}
	}
}
