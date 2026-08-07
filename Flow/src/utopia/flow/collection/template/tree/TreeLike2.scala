package utopia.flow.collection.template.tree

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Single}
import utopia.flow.collection.mutable.iterator.{BottomToTopIterator, OrderedDepthIterator, PollableOnce}
import utopia.flow.collection.template.tree.TreeLike2.AllPathsIterator
import utopia.flow.operator.MaybeEmpty

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.immutable.VectorBuilder
import scala.collection.mutable

object TreeLike2
{
	// NESTED   -------------------------
	
	private class AllPathsIterator[+N <: TreeLike2[N]](parents: Seq[N], node: N) extends Iterator[Seq[N]]
	{
		// ATTRIBUTES   -----------------
		
		private val path = parents :+ node
		private lazy val delegate = node.children.iterator.flatMap { new AllPathsIterator[N](path, _) }
		private var rootConsumed = false
		
		
		// IMPLEMENTED  -----------------
		
		override def hasNext: Boolean = !rootConsumed || delegate.hasNext
		
		override def next(): Seq[N] = {
			if (rootConsumed)
				delegate.next()
			else {
				rootConsumed = true
				path
			}
		}
	}
}

/**
  * Common trait for structures that are shaped like tress, with descending children.
  * @author Mikko Hilpinen
  * @since 1.11.2016
  * @tparam Repr Types of nodes in this tree
  */
trait TreeLike2[+Repr <: TreeLike2[Repr]] extends MaybeEmpty[Repr]
{
	// ABSTRACT   --------------------
	
	/**
	  * The child nodes directly under this node
	  */
	def children: Seq[Repr]
	
	
	// COMPUTED    ----------------------
	
	/**
	  * @return Whether this tree has child nodes registered under it
	  */
	def hasChildren = nonEmpty
	
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
	 * @return An iterator that returns this node, every child node, their children and so on.
	 *         With each node, includes the level of depth, starting from 0 (for this node).
	 *         Each branch is fully traversed before moving to its sibling.
	 *         Starts with this node.
	 */
	def allNodesWithDepthIterator = (self -> 0) +: nodesBelowWithDepthIterator
	/**
	  * @return An iterator that goes over all the nodes below this node.
	  *         Will not include this node.
	  *         Each branch is fully traversed before moving to its sibling.
	  *         I.e. every child of the first child is returned before the second child of this node is returned.
	  */
	def nodesBelowIterator: Iterator[Repr] = children.iterator.flatMap { c => c +: c.nodesBelowIterator }
	/**
	 * @return An iterator that goes over all the nodes below this node and includes their depth, starting from 1.
	 *         Will not include this node.
	 *         Each branch is fully traversed before moving to its sibling.
	 *         I.e. every child of the first child is returned before the second child of this node is returned.
	 */
	def nodesBelowWithDepthIterator: Iterator[(Repr, Int)] = _nodesBelowWithDepthIterator(1)
	private def _nodesBelowWithDepthIterator(currentDepth: Int): Iterator[(Repr, Int)] =
		children.iterator.flatMap { c => (c -> currentDepth) +: c._nodesBelowWithDepthIterator(currentDepth + 1) }
	/**
	  * @return All nodes that belong to this tree structure, including this node, as an iterable collection.
	  */
	def allNodes = allNodesIterator.toOptimizedSeq
	/**
	  * All nodes below this node as an iterable collection
	  */
	def nodesBelow = nodesBelowIterator.toOptimizedSeq
	
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
	  *         as that implementation is more efficient in terms of memory and speed.
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
	def branches = branchesIterator.toOptimizedSeq
	/**
	  * @return All branches that leave from this node.
	  *         Branches are "routes" of nodes from this node to each leaf node.
	  *         This node will not appear in any resulting branch.
	  *
	  *         Please note that this collection will contain a large number of items for large trees.
	  *         If this node had 2 children, that each had 2 children, 4 branches of length 2 would be returned.
	  *         As the size of the tree increases, the number of resulting branches increases exponentially.
	  */
	def branchesBelow = branchesBelowIterator.toOptimizedSeq
	
	/**
	 * @return An iterator that yields all unique paths within this tree.
	 *         This means that a path to every tree node is returned once.
	 *
	 *         A path, in this context, is a sequence of nodes that starts with this node and ends with the
	 *         targeted node.
	 */
	def allPathsIterator: Iterator[Seq[Repr]] = new AllPathsIterator[Repr](Empty, self)
	
	
	// IMPLEMENTED  ----------------
	
	/**
	  * Whether this tree is empty and doesn't contain a single node below it
	  */
	override def isEmpty = children.isEmpty
	
	
	// OTHER    -------------------
	
	/**
	 * Follows a path to a possible node
	 * @param path Path to follow, consisting of n items
	 * @param matches A function which tests whether a nave element (1) matches a path element (2)
	 * @tparam P Type of path elements used
	 * @return Node at the end of the specified path. None if the specified path didn't point to an existing node.
	 */
	def follow[P](path: IterableOnce[P])(matches: (Repr, P) => Boolean) =
		path
			.foldLeftIterator[Option[Repr]](Some(self)) { (node, next) =>
				node.flatMap { _.children.find { c => matches(c, next) } }
			}
			.takeTo { _.isEmpty }.last
	
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
	def contains(node: Any): Boolean = nodesBelowIterator.contains(node)
	/**
	 * Attempts to find a specific node from this tree
	 * @param node Searched node
	 * @return If this tree contained the specified node, yields Some(Seq),
	 *         where the first element is this node and the last element is 'node'.
	 *         If this tree didn't contain 'node', yields None.
	 */
	def pathTo(node: Any) = findWithPath { _ == node }
	
	/**
	 * Finds the shared parent of nodes matching the specified elements.
	 * @param elements Searched elements that match individual nodes
	 * @param contains A function that checks whether a tree node (directly) contains/matches the specified element
	 * @return Node within this tree structure that's the shared parent
	 *         of all nodes represented with the specified elements.
	 *         None if one or more of the specified elements could not be found from this tree
	 */
	def findCommonParentOf[B](elements: Iterable[B])(contains: (Repr, B) => Boolean) =
		elements.emptyOneOrMany.flatMap {
			// Case: Targeting only a single element => Finds that element from this tree
			case Left(only) => allNodesIterator.find { contains(_, only) }
			// Case: Targeting multiple elements
			case Right(elements) =>
				// Searches for each element, including their full paths
				elements.findForAll { elem => findWithPath { contains(_, elem) } }.flatMap { paths =>
					// Checks the common part within the element paths
					val pathsIter = paths.iterator
					val firstPath = pathsIter.next()
					pathsIter
						.foldLeftIterator(firstPath) { (commonPath, nextPath) =>
							val commonElementsCount = commonPath.iterator.zip(nextPath)
								.takeWhile { case (p1, p2) => p1 == p2 }.size
							commonPath.take(commonElementsCount)
						}
						// Finds the last common element (assumes that this appears as the first element in all paths)
						.takeTo { _.hasSize <= 1 }.last.lastOption
				}
		}
	
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
	def rootsBelowWhereIterator(filter: Repr => Boolean): Iterator[Repr] =
		children.iterator.flatMap { n =>
			// Case: The child matches the filter function => Accepts it and won't go deeper
			if (filter(n))
				Single(n)
			// Case: Child not accepted => Checks whether any node below is
			else
				n.rootsBelowWhereIterator(filter)
		}
	
	/**
	  * Finds the first child node from this entire tree that matches the specified condition.
	  * Returns the path to that node.
	  * @param filter A search condition
	  * @return Path to the first node matching the specified condition.
	  *         None if no such node was found.
	  *         The path starts with this node
	  *         and contains all nodes that need to be traversed in order to reach the target node.
	  *         The node that first fulfilled the specified search condition always lies at the end of the path.
	  */
	def findWithPath(filter: Repr => Boolean): Option[IndexedSeq[Repr]] = {
		val builder = OptimizedIndexedSeq.newBuilder[Repr]
		if (_findWithPath(builder)(filter))
			Some(builder.result().reverse)
		else
			None
	}
	private def _findWithPath(builder: mutable.Growable[Repr @uncheckedVariance])(filter: Repr => Boolean): Boolean = {
		val result = filter(self) || children.exists { _._findWithPath(builder)(filter) }
		if (result)
			builder += self
		result
	}
	
	/**
	 * Finds the top nodes within this tree (whether this node, direct children or grandchildren etc.) that satisfy the
	 * specified filter. Includes the "path" to all the selected nodes as well.
	 * If a node is selected, it's children are not tested anymore.
	 * @param filter A filter function
	 * @return An iterator that returns paths to all the nodes that satisfy the specified filter function.
	 *         Every path will start with this node and end with the node that fulfilled the specified function.
	 *         If this node fulfills the specified function, returns a single path consisting only of this node.
	 */
	def pathsToRootsWhere(filter: Repr => Boolean) =
		pathsToRootsWhereIterator(filter).caching
	/**
	 * Finds the top nodes within this tree (whether this node, direct children or grandchildren etc.) that satisfy the
	 * specified filter. Includes the "path" to all the selected nodes as well.
	 * If a node is selected, it's children are not tested anymore.
	 * @param filter A filter function
	 * @return Returns paths to all the nodes that satisfy the specified filter function.
	 *         Every path will start with this node and end with the node that fulfilled the specified function.
	 *         If this node fulfills the specified function, returns a single path consisting only of this node.
	 *         The result is lazily computed and cached.
	 */
	def pathsToRootsWhereIterator(filter: Repr => Boolean) =
		_filterWithPaths(filter).iterator.map { _.result().reverse }
	private def _filterWithPaths(filter: Repr => Boolean): IterableOnce[mutable.Builder[Repr @uncheckedVariance, IndexedSeq[Repr]]] = {
		// Case: This node represents a search result => Starts a new branch to it
		if (filter(self)) {
			val builder = OptimizedIndexedSeq.newBuilder[Repr]
			builder += self
			Single(builder)
		}
		// Case: This node is not a search result => looks from below and builds the paths if found
		else
			children.iterator.flatMap { _._filterWithPaths(filter) }.map { _ += self }
	}
}
