package utopia.flow.collection.template.graph

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable
import utopia.flow.collection.immutable.caching.iterable.CachingSeq
import utopia.flow.collection.immutable.graph.{Graph, GraphTravelResults, NodeTravelStage}
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Pair, Single}
import utopia.flow.collection.mutable.graph.GraphSearchProcess
import utopia.flow.collection.mutable.iterator.OrderedDepthIterator
import utopia.flow.collection.template.LookupPath
import utopia.flow.collection.template.graph.GraphNodeLike.PathsFinder
import utopia.flow.collection.template.graph.NodeTarget.AnyNode
import utopia.flow.operator.{Identity, MaybeEmpty}
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.view.immutable.View
import utopia.flow.view.template.Extender

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.mutable
import scala.math.Ordered.orderingToOrdered

object GraphNodeLike
{
	// NESTED   --------------------
	
	/**
	 * A mutable / stateful interface for finding the cheapest paths through a graph
	 * @param start The starting node
	 * @param destinations Searched destinations
	 * @param startCost Initial cost (typically 0)
	 * @param exclusive Whether looking for a single result, only (default = true)
	 * @param costOf A function that calculates the cost of traversing an edge
	 * @param sumOf A sum function for the cost
	 * @param ord Implicit ordering for the cost
	 * @tparam N Type of node values
	 * @tparam E Type of edge values
	 * @tparam Node Type of graph nodes
	 * @tparam Edge Type of graph edges
	 * @tparam C Type of counted cost
	 */
	private class PathsFinder[N, E, Node <: GraphNodeLike[N, E, Node, Edge], Edge <: GraphEdge[E, Node], C]
	(start: Node, destinations: Iterable[NodeTarget[N, E]], startCost: C, exclusive: Boolean = true)
	(costOf: Edge => C)(sumOf: (C, C) => C)
	(implicit ord: Ordering[C])
	{
		// COMPUTED --------------------
		
		/**
		 * @return A graph search process (iterator)
		 */
		def iterator: GraphSearchProcess[Node, Edge, C] = {
			// Case: No search destinations => Yields a completed search
			if (destinations.isEmpty)
				new GraphSearchProcess(Iterator.empty, start, startCost, includeStartAsResult = false,
					autocomplete = true)
			else {
				val (searchedDestinations, addStartResult) = {
					// Case: Performing an exclusive search (i.e. single result per search)
					//       => Excludes searches if they match the starting node
					if (exclusive) {
						val incompleteDestinations = destinations.filterNot { _(start, start.leavingEdges) }
						incompleteDestinations -> (incompleteDestinations.hasSize < destinations)
					}
					// Case: Performing an inclusive search => May still include the starting node in the results
					else
						destinations -> destinations.exists { _(start, start.leavingEdges) }
				}
				val iter = {
					// Case: All destinations are already reached => No need to perform any search
					if (searchedDestinations.isEmpty)
						Iterator.empty
					// Case: An actual search is required
					else
						new FinderIterator(searchedDestinations, exclusive)
				}
				new GraphSearchProcess(iter, start, startCost, includeStartAsResult = addStartResult,
					autocomplete = searchedDestinations.isEmpty)
			}
		}
		
		
		// NESTED   -------------------------
		
		/**
		 * A node wrapper used for continuing the search from that node
		 * @param currentNode The wrapped node
		 * @param currentCost The accumulated cost so far
		 * @param pathHistory Routes traversed so far. May contain multiple entries, if joining multiple pathfinders.
		 */
		private class PathFinder(val currentNode: Node, val currentCost: C, val pathHistory: Seq[Seq[Edge]])
		{
			/**
			 * Moves to the next reachable & unvisited nodes
			 * @param blockedNodes Nodes that should no longer be visited
			 * @return An iterator that yields the next reachable nodes
			 */
			def next(blockedNodes: scala.collection.Set[Node]) =
				currentNode.leavingEdges.iterator.filterNot { e => blockedNodes.contains(e.end) }.map { edge =>
					new PathFinder(edge.end, sumOf(currentCost, costOf(edge)), pathHistory.map { _ :+ edge })
				}
		}
		
		/**
		 * An iterator used for advancing the graph search
		 * @param destinations Searched destinations
		 * @param isExclusive Whether searching for only a single result
		 */
		private class FinderIterator(destinations: Iterable[NodeTarget[N, E]], isExclusive: Boolean)
			extends Iterator[GraphTravelResults[Node, Edge, C]]
		{
			// ATTRIBUTES   ----------------
			
			/**
			 * Contains nodes from which a pathfinder has LEFT
			 */
			private val blockedNodesBuffer = mutable.Set[Node]()
			/**
			 * Contains an entry for each encountered search result. The contained values may not be the final results.
			 */
			private val resultsBuffer = mutable.Map[Node, (Seq[Seq[Edge]], C)]()
			
			/**
			 * Contains the prepared pathfinders for the next iteration.
			 * Keys are the current pathfinder nodes.
			 */
			private var nextOrigins: Map[Node, PathFinder] =
				Map(start -> new PathFinder(start, startCost, Single(Empty)))
			/**
			 * Contains the smallest hypothetically achievable cost for the next iteration,
			 * assuming that the cost function always returns a positive (> 0) value.
			 */
			private var nextMinCost = startCost
			/**
			 * Contains the destinations for which no nodes have been found.
			 * In 'inclusive' mode, this remains as 'destinations' throughout the whole process.
			 */
			private var remainingDestinations = destinations
			/**
			 * Contains destinations, for which a potential result has been found.
			 * I.e. these may be later populated with better results.
			 *
			 * Each entry contains:
			 *      1. The search destination
			 *      1. The best routes so far
			 *      1. The lowest achieved cost
			 *      1. The discovered node
			 *
			 * Only filled in 'exclusive' mode.
			 */
			private var unprovenDestinations: Iterable[(NodeTarget[N, E], Seq[Seq[Edge]], C, Node)] = Empty
			/**
			 * Contains destinations for which the best possible result has been acquired.
			 */
			private val provenDestinationsBuffer = mutable.Set[Node]()
			
			/**
			 * Contains true once the search process has completed
			 */
			private var completed = false
			
			
			// IMPLEMENTED  ----------------
			
			override def hasNext: Boolean = !completed
			
			override def next() = {
				// Selects the next iteration origins - I.e. nodes with the lowest current cost
				val (delayedOrigins, iterationOrigins) = nextOrigins
					.divideBy { case (_, finder) => ord.equiv(finder.currentCost, nextMinCost) }.toTuple
				
				// Will not allow arriving to a node from which we left already (now or in the past),
				// because that can only increase the route cost
				blockedNodesBuffer ++= iterationOrigins.keys
				
				// Takes the next step and merges routes that arrived to the same node
				// Will reject new arrivals to nodes visited earlier with a better cost
				val newFinders = advance(iterationOrigins.valuesIterator)
				
				// Updates the next origins and the minimum cost value
				nextOrigins = delayedOrigins ++ newFinders.view.map { f => f.currentNode -> f }
				if (nextOrigins.nonEmpty)
					nextMinCost = nextOrigins.valuesIterator.map { _.currentCost }.min
				
				// Case: Iteration yielded new routes => updates results and destinations, etc.
				if (newFinders.nonEmpty)
					updateResults(newFinders)
				
				// Checks for completion
				// Completes if either:
				//      1) There are no more nodes to travel through, OR
				//      2) All destinations have been identified, AND
				//          3) It is impossible to achieve an equal or better cost for any destination
				// Tests condition #1
				completed = nextOrigins.isEmpty ||
					// Tests for #2
					(remainingDestinations.isEmpty &&
						// Tests for #3
						unprovenDestinations.iterator.map { _._3 }.maxOption.forall { _ <= nextMinCost })
				
				// Converts the search results into graph-travel results
				// Result-based stages are processed immediately because the results-collection is mutable
				val resultStages = resultsBuffer.iterator
					.map { case (node, (routes, cost)) =>
						NodeTravelStage(node, routes, cost, isDestination = true,
							isConfirmedAsOptimal = completed || cost <= nextMinCost)
					}
					.toOptimizedSeq
				// Other stages are added only when requested
				val pathFinderStagesIterator = nextOrigins.valuesIterator.map { pf =>
					NodeTravelStage(pf.currentNode, pf.pathHistory, pf.currentCost,
						isConfirmedAsOptimal = completed || pf.currentCost <= nextMinCost)
				}
				
				GraphTravelResults(
					stages = CachingSeq(pathFinderStagesIterator, resultStages),
					minFutureCost = nextMinCost,
					foundResults = resultsBuffer.nonEmpty,
					foundAllResults = if (isExclusive) remainingDestinations.isEmpty else completed,
					isConfirmedAsOptimal = completed)
			}
			
			
			// OTHER    -------------------------
			
			private def advance(finders: IterableOnce[PathFinder]) = {
				// Takes the next step and merges routes that arrived to the same node
				// Will reject new arrivals to nodes visited earlier with a better cost
				finders.iterator.flatMap { _.next(blockedNodesBuffer) }.groupToSeqsBy { _.currentNode }
					.flatMap { case (location, finders) =>
						val bestNewCost = finders.map { _.currentCost }.min
						val earlierResults = nextOrigins.get(location)
						// Case: Found better or at least as good routes to the already discovered nodes
						if (earlierResults.forall { _.currentCost >= bestNewCost }) {
							// Includes the previously found "origin" in the merging process
							scala.collection.View.concat(finders, earlierResults)
								.filter { f => ord.equiv(f.currentCost, bestNewCost) }
								.toOptimizedSeq.oneOrMany match
							{
								// Case: Only one finder arrived to this location => picks it as it is
								case Left(bestFinder) => Some(bestFinder)
									// Case: Multiple finders with the same cost => Combines them into one
								case Right(bestFinders) =>
									Some(bestFinders.reduce { (a, b) =>
										new PathFinder(location, a.currentCost, a.pathHistory ++ b.pathHistory)
									})
							}
						}
						// Case: The new routes were more expensive than the ones found before => Discards them
						else
							None
					}
			}
			
			/**
			 * Updates [[resultsBuffer]], [[proveDestinations]], [[unprovenDestinations]] and [[remainingDestinations]]
			 * @param updatedFinders Pathfinders that were recently updated. Not empty.
			 */
			private def updateResults(updatedFinders: Iterable[PathFinder]): Unit = {
				// Checks whether already arrived to some or all destinations.
				// Found destinations contain 4 parts:
				//      1) The original destination function
				//      2) Destination node
				//      3) Routes
				//      4) Cost
				val (nextDestinations, foundDestinations) = remainingDestinations.flatDivideWith { destination =>
					val arrived = updatedFinders.filter { o => destination(o.currentNode, o.currentNode.leavingEdges) }
					// Case: No finder arrived to this destination yet => Keeps it as a remaining destination
					if (arrived.isEmpty)
						Single(Left(destination))
					// Case: One or more finders arrived to this (exclusive) destination
					//       => Creates a summary of the (currently) best results
					else if (isExclusive) {
						val bestResults = arrived.filterMinBy { _.currentCost }.toOptimizedSeq
						val bestResult = bestResults.head
						val targetNode = bestResult.currentNode
						// Combines all discovered unique routes to the target node
						val routes = (
							bestResults.view.tail.iterator.filter { _.currentNode == targetNode } ++
							Single(bestResult))
							.flatMap { _.pathHistory }.distinct.toOptimizedSeq
						// The latest result may still be improved upon
						Single(Right((destination, routes, bestResult.currentCost, targetNode)))
					}
					// Case: One or more finders arrived to a node identified by this (non-exclusive) destination
					//       => Determines the best result for each encountered node
					else
						arrived.groupToSeqsBy { _.currentNode }.map { case (node, arrived) =>
							// WET WET
							val bestResults = arrived.filterMinBy { _.currentCost }
							val bestResult = bestResults.head
							val routes = bestResults.iterator.flatMap { _.pathHistory }.distinct.toOptimizedSeq
							Right((destination, routes, bestResult.currentCost, node))
						}
				}
				// In exclusive mode, checks whether it's possible to get a better result than one already found.
				// Results are considered "unproven" until the acquired minimum cost exceeds
				// that found for the destination.
				if (exclusive) {
					// Checks for destinations that are now proven optimal.
					// Calculates the new state of the unproven destinations.
					val updatedUnprovenResults = proveDestinations(updatedFinders)
					
					// Updates the results buffer
					if (updatedUnprovenResults.nonEmpty) {
						// Performs result-removal first, if appropriate
						resultsBuffer --= updatedUnprovenResults.view.flatMap { _._5 }.filterNot { node =>
							provenDestinationsBuffer.contains(node) || updatedUnprovenResults.exists { _._2 == node }
						}
						// Next adds the updated results to the results buffer
						resultsBuffer ++= updatedUnprovenResults
							.view.map { case (_, node, routes, cost, _) => node -> (routes -> cost) }
					}
					
					// Updates the remaining destinations
					remainingDestinations = nextDestinations
					unprovenDestinations = updatedUnprovenResults
						.map { case (destination, node, routes, cost, _) => (destination, routes, cost, node) } ++
						foundDestinations
				}
				
				// Adds new results to the buffer
				if (foundDestinations.nonEmpty)
					resultsBuffer ++= foundDestinations.map { case (_, routes, cost, node) => node -> (routes -> cost) }
			}
			
			/**
			 * Checks which destinations are now proven optimal and which are not.
			 * Calculates the updated state of the unproven destinations.
			 *
			 * This function is only needed in exclusive mode.
			 *
			 * @param updatedFinders Pathfinders that were updated. Not empty.
			 * @return New unproven destinations.
			 *         Each entry contains:
			 *              1. The search destination
			 *              1. The best routes so far
			 *              1. The lowest achieved cost
			 *              1. The discovered node
			 */
			private def proveDestinations(updatedFinders: Iterable[PathFinder]) = {
				// Checks which destinations can now be proven optimal
				val (newlyProvenDestinations, remainsUnprovenDestinations) = unprovenDestinations
					.divideBy { nextMinCost <= _._3 }.toTuple
				// Remembers the proven destinations in order to prevent their removal
				provenDestinationsBuffer ++= newlyProvenDestinations.view.map { _._4 }
				
				// Checks whether unproven results may be improved upon
				remainsUnprovenDestinations.map { case (destination, previousRoutes, previousMinCost, previousNode) =>
					// Finds updated search results that are better or as good as the results found before
					val arrived = updatedFinders.iterator
						.filter { o => destination(o.currentNode, o.currentNode.leavingEdges) }
						.filter { _.currentCost <= previousMinCost }
						.toOptimizedSeq
					// Case: New competing results found
					//       => Merges them to the previous results or overrides previous results with them
					if (arrived.nonEmpty) {
						val bestResults = arrived.filterMinBy { _.currentCost }
						val bestResult = bestResults.find { _.currentNode == previousNode }
							.getOrElse(bestResults.head)
						val newNode = bestResult.currentNode
						val discoveredRoutes = bestResults.iterator
							.filter { _.currentNode == newNode }.flatMap { _.pathHistory }.distinct.toOptimizedSeq
						
						// Case: The new results are better than those found before
						//       => Replaces the old results
						if (bestResult.currentCost < previousMinCost) {
							// If targeted different nodes, may remove the other node from the results
							(destination, newNode, discoveredRoutes, bestResult.currentCost,
								Some(previousNode).filterNot { _ == newNode })
						}
						// Case: The new results are as good as the previous
						//       => Merges them if they're for the same node, otherwise ignores them
						else if (newNode == previousNode)
							(destination, previousNode,
								scala.collection.View.concat(previousRoutes ++ discoveredRoutes)
									.iterator.distinct.toOptimizedSeq,
								previousMinCost, None)
						else
							(destination, previousNode, previousRoutes, previousMinCost, None)
					}
					// Case: No competing results found => Keeps the previous entry
					else
						(destination, previousNode, previousRoutes, previousMinCost, None)
				}
			}
		}
	}
}

/**
 * Graph nodes contain content and are connected to other graph nodes via edges
 * @tparam N Type of values wrapped by graph nodes
 * @tparam E Type of values wrapped by graph edges
 * @tparam Repr Type of the implementing nodes
 * @tparam Edge Type of the implementing edges
 * @author Mikko Hilpinen
 * @since 10.4.2019
 */
trait GraphNodeLike[+N, +E, +Repr <: GraphNodeLike[N, E, Repr, Edge], +Edge <: GraphEdge[E, Repr]]
	extends View[N] with Extender[N] with MaybeEmpty[Repr]
{
    // ABSTRACT --------------------
	
	/**
	 * @return This node
	 */
	def self: Repr
	
	/**
	  * @return The edges leaving this node.
	  */
    def leavingEdges: Iterable[Edge]
	
	/**
	 * Converts this node to a graph
	 * @return A graph based on this node's connections
	 */
	def toGraph: Graph[N, E]
    
    
    // COMPUTED    ----------------
	
	/**
	  * The nodes accessible from this node
	  */
	def endNodes = endNodesIterator.toOptimizedSeq
	/**
	  * @return An iterator that yields the nodes that are directly accessible from this node
	  */
	def endNodesIterator = leavingEdges.iterator.map { _.end }.distinct
	
	/**
	  * @return An iterator that returns all nodes within this graph, starting with this one.
	  *         The iterator is not specifically ordered, but iterates by traversing through this graph.
	  *         See: .orderedAllNodesIterator if you want an ordered iterator.
	  */
	def allNodesIterator = {
		val visitedNodes = mutable.Set[Any](this)
		_allNodesIterator(visitedNodes)
	}
	private def _allNodesIterator(visitedNodes: mutable.Set[Any]): Iterator[Repr] =
		Iterator.single(self) ++ leavingEdges.iterator.flatMap { edge =>
			val node = edge.end
			if (visitedNodes.contains(node))
				None
			else {
				visitedNodes += node
				node._allNodesIterator(visitedNodes)
			}
		}
	/**
	  * @return An iterator that returns all nodes within this graph, starting with this one.
	  *         The iterator is ordered in a way that the nodes are returned from closest to furthest.
	  */
	def orderedAllNodesIterator: Iterator[Repr] = {
		val visitedNodes = mutable.Set[Any](this)
		OrderedDepthIterator(Iterator.single(self)) { start =>
			start.leavingEdges.iterator.flatMap { edge =>
				val node = edge.end
				if (visitedNodes.contains(node))
					None
				else {
					visitedNodes += node
					Some(node)
				}
			}
		}
	}
	
	/**
	  * @return An iterator that returns all shortest routes that appear within this node.
	  *         In situations where there would be multiple equally short routes,
	  *         only the first encountered route is returned.
	  *
	  *         The resulting routes / nodes are listed in order of length;
	  *         Empty route to this node is returned first,
	  *         then routes of length 1 to this node's siblings, then routes of length 2 and so on.
	  *
	  *         The values returned by the returned iterator consist of two parts:
	  *         1. Route to the node in question as a sequence of edges
	  *         1. The node at the end of that route
	  */
	def shortestRoutesIterator: Iterator[(Seq[Edge], Repr)] = {
		val visitedNodes = mutable.Set[Any](this)
		OrderedDepthIterator(Iterator.single[(Seq[Edge], Repr)](Empty -> self)) { case (route, lastNode) =>
			lastNode.leavingEdges.iterator.flatMap { edge =>
				val node = edge.end
				if (visitedNodes.contains(node))
					None
				else {
					visitedNodes += node
					Some((route :+ edge) -> node)
				}
			}
		}
	}
	
	/**
	  * @return An iterator that returns all edges that appear within this graph.
	  *         The edges are not returned in any specific order, except that all edges belonging to a single node
	  *         are returned in sequence.
	  */
    def allEdgesIterator = allNodesIterator.flatMap { _.leavingEdges }
	/**
	  * @return An iterator that returns all values within the nodes in this graph.
	  *         The values are not returned in any specific order, except that the value of this node is returned first.
	  */
	def allValuesIterator = allNodesIterator.map { _.value }
	@deprecated("Deprecated for removal. Renamed to .allValuesIterator", "v2.9")
	def allNavsIterator = allValuesIterator
	/**
	  * @return An iterator that returns all values within the nodes in this graph.
	  *         The iterator is ordered so that it returns first the values of nodes closest to this node,
	  *         starting from this node itself. The iterator then moves further one layer at a time.
	  */
	def orderedAllValuesIterator = orderedAllNodesIterator.map { _.value }
	
	/**
	  * @return All nodes that appear within this graph, including this node
	  */
	def allNodes = allNodesIterator.toOptimizedSeq
	/**
	  * @return All edges that appear within this graph
	  */
	def allEdges = allEdgesIterator.toOptimizedSeq
	/**
	  * @return All distinct values that appear within this graph
	  */
	def allValues = allValuesIterator.toOptimizedSeq
	
	/**
	 * @return A lazily initialized tree based on this graph, where each node matches one in this graph
	 *         but only contains the node value.
	 *
	 *         This node's representation will appear as the root of the tree.
	 *         Other nodes may appear in multiple locations, but never twice in a single branch.
	 *
	 *         For example, if node A connects to nodes B and C, which both connect to node D,
	 *         which then connects to node E, the resulting branches would be:
	 *         A -> B -> D -> E,
	 *         A -> C -> D -> E.
	 *         Notice how D and E appear twice.
	 *
	 *         The resulting tree may be considered to consist of unique paths within this graph that all start
	 *         from this node and never traverse one node twice.
	 *
	 *         Please note that the resulting tree will be very large for graphs with a large number of edges.
	 */
	def toValueTree: immutable.tree.ValueTree[N] = _toTree(Set(self)) { _.value }
	/**
	  * @return A lazily initialized tree based on this graph.
	  *         This node will appear as the root of the tree.
	  *         Other nodes may appear in multiple locations, but never twice in a single branch.
	  *         Each tree node contains a reference to a graph node.
	  *
	  *         For example, if node A connects to nodes B and C, which both connect to node D,
	  *         which then connects to node E, the resulting branches would be:
	  *         A -> B -> D -> E,
	  *         A -> C -> D -> E.
	  *         Notice how D and E appear twice.
	  *
	  *         The resulting tree may be considered to consist of unique paths within this graph that all start
	  *         from this node and never traverse one node twice.
	  *
	  *         Please note that the resulting tree will be very large for graphs with a large number of edges.
	  */
	def toTree: immutable.tree.ValueTree[Repr] = _toTree(Set(self))(Identity)
	private def _toTree[A](traversedNodes: Set[Any])(wrapNode: Repr => A): immutable.tree.ValueTree[A] = {
		// Remembers which nodes have been visited (branch-specific)
		val newTraversed = traversedNodes + self
		// Creates the tree lazily
		immutable.tree.ValueTree(wrapNode(self), lazily = true).withChildren(leavingEdges.iterator.flatMap { edge =>
			val node = edge.end
			// Case: A node would be a parent of this node in the tree => ends
			if (newTraversed.contains(node))
				None
			// Case: Unique node within this branch => Converts it to a tree lazily, also
			else
				Some(node._toTree(newTraversed)(wrapNode))
		})
	}
	
	/**
	 * Finds all circular routes (edge combinations) that connect this node to itself.
	 * @return An iterator that yields all possible routes to the specified node.
	 */
	def routesToSelf = routesTo(self)
	
	/**
	  * @return An interactive search process that targets all nodes in this graph
	  */
	def searchShortestRoutesToAll = searchAllNodes[Int] { _: Edge => 1 }
	
	/**
	 * @param valueEquals Implicit equals-function for comparing node values with nav input
	 * @tparam Nav Type of nav input accepted
	 * @return An interface for traversing a path in this graph, using node values.
	 */
	def lookup[Nav >: N](implicit valueEquals: EqualsFunction[Nav] = EqualsFunction.default): LookupPath[Nav, Repr] =
		lookupUsing[Nav](valueEquals)
	/**
	 * @param edgeValueEquals The equality function used to compare edge values with navigation input.
	 * @return A path-traversal interface from this node, based on edge values and the specified equals-function.
	 */
	def traverseEdges[Nav >: E](implicit edgeValueEquals: EqualsFunction[Nav] = EqualsFunction.default): LookupPath[Nav, Repr] =
		traverseEdgesUsing[Nav](edgeValueEquals)
	
	
	// IMPLEMENTED  ----------------------
	
	override def wrapped: N = value
	
	override def isEmpty: Boolean = leavingEdges.isEmpty
	
	override def toString = s"Node($value)"


	// OTHER	-----------------
	
	/**
	  * Traverses edges from this node once
	  * @param edgeType The content of the traversed edge(s)
	  * @return An iterator that yields the node(s) at the end of the edge(s)
	  */
	@deprecated("Deprecated for removal. Please use traverseEdges instead", "v2.9")
	def /[E2 >: E](edgeType: E2) = leavingEdges.iterator.filter { _.value == edgeType }.map { _.end }
	/**
	  * Traverses a deep path that consists of edges between nodes
	  * @param path The content of the edges to travel in sequence, starting from edges of this node.
	  *             An empty path is considered to point to this node.
	  * @return The node(s) at the end of the path
	  */
	@deprecated("Deprecated for removal. Please use traverseEdges instead", "v2.9")
	def /[E2 >: E](path: IterableOnce[E2]): Seq[Repr] =
		path
			.foldLeftIterator[Seq[Repr]](Single(self)) { (nodes, nextElem) =>
				nodes.iterator.flatMap { _/nextElem }.distinct.toOptimizedSeq
			}
			.takeTo { _.isEmpty }.last
	/**
	  * Traverses a deep path that consists of edges between nodes
	  * @param first The first edge to traverse
	  * @param second The second edge to traverse
	  * @param more More edges
	  * @return The node(s) at the end of the path
	  */
	@deprecated("Deprecated for removal. Please use traverseEdges instead", "v2.9")
	def /[E2 >: E](first: E2, second: E2, more: E2*): Seq[Repr] = this / (Pair(first, second) ++ more)
	
	/**
	  * @param other Another node
	  * @return Whether this node contains a direct connection to the specified node
	  */
	def isDirectlyConnectedTo(other: NodeTarget[N, E]) =
		leavingEdges.exists { edge => other(edge.end, edge.end.leavingEdges) }
	/**
	  * Performs a recursive check and looks whether this node is at all connected to the specified node
	  * @param other Another node
	  * @return Whether this node is at all connected to the specified node
	  */
	def isConnectedTo(other: NodeTarget[N, E]) = allNodesIterator.exists { n => other(n, n.leavingEdges) }
	
	/**
	 * @param valueEquals Equality function to use in navigation
	 * @tparam Nav Type of navigation input accepted
	 * @return A path-traversal interface from this node, based on node values and the specified equals-function.
	 */
	def lookupUsing[Nav >: N](valueEquals: EqualsFunction[Nav]): LookupPath[Nav, Repr] =
		LookupGraphPathViaNodes[Nav, Repr](self)(valueEquals)
	/**
	 * @param edgeValueEquals The equality function used to compare edge values with navigation input.
	 * @return A path-traversal interface from this node, based on edge values and the specified equals-function.
	 */
	def traverseEdgesUsing[Nav >: E](edgeValueEquals: EqualsFunction[Nav]): LookupPath[Nav, Repr] =
		LookupGraphPathViaEdges[Nav, Repr](self)(edgeValueEquals)
	
    /**
     * Finds an edge pointing to another node, if there is one
     * @param other The node this node may be connected to
     * @return an edge connecting the two nodes, if there is one. If there are multiple edges
     * pointing toward the specified node, returns the first one encountered.
     * @see edgesTo(GraphNode[_, _])
     */
    def edgeTo(other: NodeTarget[N, E]) =
	    leavingEdges.find { edge => other(edge.end, edge.end.leavingEdges) }
    /**
     * Finds all edges pointing from this node to the specified node.
     * @param other another node
     * @return The edges pointing towards the provided node from this node
     */
    def edgesTo(other: NodeTarget[N, E]) = leavingEdges.filter { edge => other(edge.end, edge.end.leavingEdges) }
	
	/**
	  * Finds all routes (edge combinations) that connect this node to the specified node.
	 * No looping routes are included, except when finding routes to self.
	  *
	 * @param node The searched node
	  * @return An iterator that yields all possible routes to the specified node.
	  */
	def routesTo(node: NodeTarget[N, E]): Iterator[Seq[Edge]] = {
		// Case: Searching for routes to self => Starts from the next nodes instead
		if (node(self, leavingEdges))
			leavingEdges.iterator.flatMap { edge =>
				// Case: Direct edge to self => Won't apply recursion
				if (edge.end == self)
					Single(Single(edge))
				else
					edge.end._routesTo(node, Set()).map { b => (b += edge).result().reverse }
			}
		// Case: Searching for routes to another node => Uses the default implementation
		else
			_routesTo(node, Set()).map { _.result().reverse }
	}
	// Uses recursion
	private def _routesTo(node: NodeTarget[N, E],
	                      visitedNodes: Set[Any]): Iterator[mutable.Builder[Edge @uncheckedVariance, Seq[Edge]]] =
	{
		// Tries to find the destination from each connected edge that leads to a new node
		val newVisitedNodes = visitedNodes + self
		leavingEdges.iterator.filterNot { e => newVisitedNodes.contains(e.end) }.flatMap { edge =>
			// Case: Reached the targeted node => Applies a direct route to it
			if (node(edge.end, edge.end.leavingEdges)) {
				val routeBuilder = OptimizedIndexedSeq.newBuilder[Edge]
				routeBuilder += edge
				Single(routeBuilder)
			}
			// Case: No direct route => Searches for indirect routes
			else
				edge.end._routesTo(node, newVisitedNodes).map { _ += edge }
		}
	}
	
	/**
	  * Starts a graph search which targets all nodes in this graph.
	  * Throughout this interactive process, discovers the cheapest route or routes to these nodes.
	  * This process may be advanced manually, through the returned interface.
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param n Numeric implementation for the cost values
	  * @tparam C Type of cost values used.
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def searchAllNodes[C](costOf: Edge => C)(implicit n: Numeric[C]): GraphSearchProcess[Repr, Edge, C] =
		customSearchAllNodes(n.zero)(costOf)(n.plus)
	/**
	  * Starts a graph search which targets all nodes in this graph.
	  * Throughout this interactive process, discovers the cheapest route or routes to these nodes.
	  * This process may be advanced manually, through the returned interface.
	  * @param startCost Initial assigned to all routes.
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param sumOf A function for computing the sum of two cost values.
	  * @param ord Implicit ordering to use for cost values.
	  * @tparam C Type of cost values used.
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def customSearchAllNodes[C](startCost: C)(costOf: Edge => C)(sumOf: (C, C) => C)(implicit ord: Ordering[C]) =
		customSearch(startCost)(AnyNode)(costOf)(sumOf)
	
	/**
	  * Finds the shortest routes to a single node.
	  * @param node The targeted node
	  * @return Search results, if successful. None if unsuccessful.
	  */
	def shortestRoutesToOne(node: NodeTarget[N, E]) = cheapestRoutesToOne(node) { _ => 1 }
	/**
	  * Finds the cheapest routes to a single node.
	  * @param node The targeted node
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param n Numeric implementation for the cost values
	  * @tparam C Type of cost values used.
	  * @return Search results, if successful. None if unsuccessful.
	  */
	def cheapestRoutesToOne[C](node: NodeTarget[N, E])(costOf: Edge => C)(implicit n: Numeric[C]) =
		searchForOne(node)(costOf).finish().any
	/**
	  * Starts a graph search which looks for shortest route to the specified node (in terms of the number of edges).
	  * @param node Targeted route end node
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def searchShortestRoutesToOne(node: NodeTarget[N, E]) =
		searchForOne(node) { _ => 1 }
	/**
	  * Starts a graph search which targets a single node.
	  * Throughout this interactive process, discovers the cheapest route or routes to that node.
	  * This process may be advanced manually, through the returned interface.
	  * @param node Targeted route end node
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param n Numeric implementation for the cost values
	  * @tparam C Type of cost values used.
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def searchForOne[C](node: NodeTarget[N, E])(costOf: Edge => C)(implicit n: Numeric[C]) =
		searchForEach(Single(node))(costOf)
	/**
	  * Starts a graph search which targets a single node.
	  * Throughout this interactive process, discovers the cheapest route or routes to that node.
	  * This process may be advanced manually, through the returned interface.
	  * @param node Targeted route end node
	  * @param startCost Initial assigned to all routes.
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param sumOf A function for computing the sum of two cost values.
	  * @param ord Implicit ordering to use for cost values.
	  * @tparam C Type of cost values used.
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def customSearchForOne[C](startCost: C)(node: NodeTarget[N, E])(costOf: Edge => C)(sumOf: (C, C) => C)
	                         (implicit ord: Ordering[C]) =
		customSearchForEach(Single(node), startCost)(costOf)(sumOf)
	
	/**
	  * Finds the shortest routes to a certain subgroup of nodes within this graph
	  * @param filter A function used for identifying which nodes are targeted and which are not.
	  * @return Search results that include the shortest route or routes to each reachable node that fulfilled 'filter'.
	  */
	def shortestRoutesTo(filter: NodeTarget[N, E]) = cheapestRoutesTo(filter) { _ => 1 }
	/**
	  * Finds the cheapest routes to a certain subgroup of nodes within this graph
	  * @param filter A function used for identifying which nodes are targeted and which are not.
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param n Numeric implementation for the cost values
	  * @tparam C Type of cost values used.
	  * @return Search results that include the shortest route or routes to each reachable node that fulfilled 'filter'.
	  */
	def cheapestRoutesTo[C](filter: NodeTarget[N, E])(costOf: Edge => C)(implicit n: Numeric[C]) =
		search(filter)(costOf).finish()
	/**
	  * Starts a graph search which looks for shortest routes (in terms of the number of edges).
	  * @param target A function for identifying the targeted nodes
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def searchShortestRoutesTo(target: NodeTarget[N, E]) = search(target) { _ => 1 }
	/**
	  * Starts a graph search.
	  * Throughout this interactive process, discovers ALL nodes accepted by the specified filter function.
	  * Also discovers the cheapest route or routes to each of these nodes.
	  * This process may be advanced manually, through the returned interface.
	  * @param target A function for identifying the targeted nodes
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param n Numeric implementation for the cost values
	  * @tparam C Type of cost values used.
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def search[C](target: NodeTarget[N, E])(costOf: Edge => C)
	             (implicit n: Numeric[C]): GraphSearchProcess[Repr, Edge, C] =
		customSearch(n.zero)(target)(costOf)(n.plus)
	/**
	  * Starts a graph search.
	  * Throughout this interactive process, discovers ALL nodes accepted by the specified filter function.
	  * Also discovers the cheapest route or routes to each of these nodes.
	  * This process may be advanced manually, through the returned interface.
	  * @param startCost Initial assigned to all routes.
	  * @param target A function for identifying the targeted nodes
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param sumOf A function for computing the sum of two cost values.
	  * @param ord Implicit ordering to use for cost values.
	  * @tparam C Type of cost values used.
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def customSearch[C](startCost: C)(target: NodeTarget[N, E])(costOf: Edge => C)(sumOf: (C, C) => C)
	                   (implicit ord: Ordering[C]) =
		_searchFor(Single(target), startCost, exclusive = false)(costOf)(sumOf)
	
	// TODO: Add findAny version of filter
	
	/**
	  * Finds the shortest routes to a certain group of nodes
	  * @param nodes Searched nodes (exclusive)
	  * @return Search results that contain the shortest route or routes to each of the specified targets
	  */
	def shortestRoutesToEach(nodes: Iterable[NodeTarget[N, E]]) = cheapestRoutesToEach(nodes) { _ => 1 }
	/**
	  * Finds the cheapest routes to a certain group of nodes
	  * @param nodes Searched nodes
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param n Numeric implementation for the cost values
	  * @tparam C Type of cost values used.
	  * @return Search results that contain the shortest route or routes to each of the specified targets
	  */
	def cheapestRoutesToEach[C](nodes: Iterable[NodeTarget[N, E]])(costOf: Edge => C)(implicit n: Numeric[C]) =
		searchForEach(nodes)(costOf).finish()
	/**
	  * Starts a graph search which looks for shortest routes to the specified nodes (in terms of the number of edges).
	  * @param nodes Targeted route end nodes
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def searchShortestRoutesToEach(nodes: Iterable[NodeTarget[N, E]]) =
		searchForEach(nodes) { _ => 1 }
	/**
	  * Starts a graph search which targets 0-n nodes.
	  * Throughout this interactive process, discovers these nodes and the cheapest route or routes to them.
	  * This process may be advanced manually, through the returned interface.
	  * @param destinations A set of find functions which identify the targeted graph nodes.
	  *                     Will yield 0-1 results for each of these functions.
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param n Numeric implementation for the cost values
	  * @tparam C Type of cost values used.
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def searchForEach[C](destinations: Iterable[NodeTarget[N, E]])(costOf: Edge => C)
	                    (implicit n: Numeric[C]): GraphSearchProcess[Repr, Edge, C] =
		customSearchForEach(destinations, n.zero)(costOf)(n.plus)
	/**
	  * Starts a graph search which targets 0-n nodes.
	  * Throughout this interactive process, discovers these nodes and the cheapest route or routes to them.
	  * This process may be advanced manually, through the returned interface.
	  * @param destinations A set of find functions which identify the targeted graph nodes.
	  *                     Will yield 0-1 results for each of these functions.
	  * @param startCost Initial assigned to all routes.
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param sumOf A function for computing the sum of two cost values.
	  * @param ord Implicit ordering to use for cost values.
	  * @tparam C Type of cost values used.
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def customSearchForEach[C](destinations: Iterable[NodeTarget[N, E]], startCost: C)
	                      (costOf: Edge => C)(sumOf: (C, C) => C)
	                      (implicit ord: Ordering[C]) =
		_searchFor(destinations, startCost, exclusive = true)(costOf)(sumOf)
		
	@deprecated("Please use cheapestRoutesToOne instead", "v2.9")
	def cheapestRoutesToNode[C](node: View[Any])(costOf: Edge => C)(implicit n: Numeric[C]) =
		cheapestRoutesToOne(node)(costOf)
		
	private def _searchFor[C](destinations: Iterable[NodeTarget[N, E]], startCost: C, exclusive: Boolean)
	                            (costOf: Edge => C)(sumOf: (C, C) => C)
	                            (implicit ord: Ordering[C]) =
		new PathsFinder[N, E, Repr, Edge, C](self, destinations, startCost, exclusive)(costOf)(sumOf).iterator
}