package utopia.flow.collection.template.graph

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.iterable.CachingSeq
import utopia.flow.collection.immutable.graph.{GraphTravelResults, NodeTravelStage}
import utopia.flow.collection.immutable.{Empty, Graph, Pair, Single}
import utopia.flow.collection.mutable.graph.GraphSearchProcess
import utopia.flow.collection.mutable.iterator.OrderedDepthIterator
import utopia.flow.collection.{immutable, template}
import utopia.flow.collection.template.graph.GraphNodeLike.PathsFinder
import utopia.flow.collection.template.graph.NodeTarget.AnyNode
import utopia.flow.operator.Identity
import utopia.flow.view.immutable.View
import utopia.flow.view.template.Extender

import scala.collection.mutable
import scala.math.Ordered.orderingToOrdered

object GraphNodeLike
{
	// NESTED   --------------------
	
	private class PathsFinder[N, E, Node <: GraphNodeLike[N, E, Node, Edge], Edge <: GraphEdgeLike[E, Node], C]
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
		
		private class PathFinder(val currentNode: Node, val currentCost: C, val pathHistory: Seq[Seq[Edge]])
		{
			def next(blockedNodes: scala.collection.Set[Node]) = {
				currentNode.leavingEdges.view.filterNot { e => blockedNodes.contains(e.end) }.map { edge =>
					new PathFinder(edge.end, sumOf(currentCost, costOf(edge)), pathHistory.map { _ :+ edge })
				}
			}
		}
		
		private class FinderIterator(destinations: Iterable[NodeTarget[N, E]], isExclusive: Boolean)
			extends Iterator[GraphTravelResults[Node, Edge, C]]
		{
			// ATTRIBUTES   ----------------
			
			// Contains nodes from which a pathfinder has LEFT
			private val blockedNodesBuffer = mutable.Set[Node]()
			// Contains an entry for each encountered search result. The contained values may not be the final results.
			private val resultsBuffer = mutable.Map[Node, (Seq[Seq[Edge]], C)]()
			
			// Prepared pathfinders for the next iteration
			private var nextOrigins: Map[Node, PathFinder] =
				Map(start -> new PathFinder(start, startCost, Single(Empty)))
			// Smallest achievable cost for the next iteration,
			// assuming that cost function always returns a positive (> 0) value
			private var nextMinCost = startCost
			// Destination search functions for which no nodes have been found
			// In 'inclusive' mode, this will remain as 'destinations' throughout the whole process
			private var remainingDestinations = destinations
			// Destination search functions, discovered routes, achieved cost values, plus found node,
			// Listed for cases where a node has been found, but where a better result may still be achieved
			// Only filled in 'exclusive' mode
			private var unprovenDestinations: Iterable[(NodeTarget[N, E], Seq[Seq[Edge]], C, Node)] = Empty
			private val provenDestinationsBuffer = mutable.Set[Node]()
			
			// Contains true once the search process has completed
			private var completed = false
			
			
			// IMPLEMENTED  ----------------
			
			override def hasNext: Boolean = !completed
			
			override def next() = {
				// Selects the next iteration origins - Nodes with the lowest current cost
				val (delayedOrigins, iterationOrigins) = nextOrigins
					.divideBy { case (_, finder) => ord.equiv(finder.currentCost, nextMinCost) }.toTuple
				
				// Will not allow arriving to a node from which we left already (now or in the past),
				// because that can only increase the route cost
				blockedNodesBuffer ++= iterationOrigins.keys
				
				// Takes the next step and merges routes that arrived to the same node
				// Will reject new arrivals to nodes visited earlier with a better cost
				val newFinders = iterationOrigins.values.flatMap { _.next(blockedNodesBuffer) }
					.groupBy { _.currentNode }
					.flatMap { case (location, finders) =>
						val bestNewCost = finders.map { _.currentCost }.min
						// Only recognizes the new results if they were better than some previously encountered origins
						// In which case either merges or discards the previous origins
						val earlierResults = nextOrigins.get(location)
						// Case: Found better or at least as good routes to the already discovered nodes
						if (earlierResults.forall { _.currentCost >= bestNewCost }) {
							// Includes the previously found "origins" in the merging process
							val bestFinders = (finders ++ earlierResults)
								.filter { f => ord.equiv(f.currentCost, bestNewCost) }
							// Case: Only one finder arrived to this location => picks it as it is
							if (bestFinders hasSize 1)
								Some(bestFinders.head)
							// Case: Merging => Takes the finders of smallest cost and combines them
							else
								Some(bestFinders.reduce { (a, b) =>
									new PathFinder(location, a.currentCost, a.pathHistory ++ b.pathHistory)
								})
						}
						// Case: The new routes were more expensive than the ones found before => Discards them
						else
							None
					}
				
				
				// Updates the next origins and the minimum cost value
				nextOrigins = delayedOrigins ++ newFinders.map { f => f.currentNode -> f }
				if (nextOrigins.nonEmpty)
					nextMinCost = nextOrigins.valuesIterator.map { _.currentCost }.min
				
				// Case: Iteration yielded new routes => updates results and destinations, etc.
				if (newFinders.nonEmpty) {
					// Checks whether already arrived to some or all destinations
					// Found destinations contains 4 parts:
					//      1) The original destination function
					//      2) Destination node
					//      3) Routes
					//      4) Cost
					val (nextDestinations, foundDestinations) = remainingDestinations.flatDivideWith { destination =>
						val arrived = newFinders.filter { o => destination(o.currentNode, o.currentNode.leavingEdges) }
						// Case: No finder arrived to this destination yet => Keeps it as a remaining destination
						if (arrived.isEmpty)
							Single(Left(destination))
						// Case: One or more finders arrived to this destination
						//       => Creates a summary of the (currently) best results
						else if (isExclusive) {
							val bestResults = arrived.filterMinBy { _.currentCost }.toOptimizedSeq
							val bestResult = bestResults.head
							val routes = (bestResults.tail.filter { _.currentNode == bestResult.currentNode } :+
								bestResult)
								.flatMap { _.pathHistory }.distinct
							// The latest result may still be improved upon
							Single(Right((destination, routes, bestResult.currentCost, bestResult.currentNode)))
						}
						// Case: One or more finders arrived to a node identified by this destination
						//       => Determines the best result for each encountered node
						else
							arrived.groupBy { _.currentNode }.map { case (node, arrived) =>
								// WET WET
								val bestResults = arrived.filterMinBy { _.currentCost }.toSeq
								val bestResult = bestResults.head
								val routes = bestResults.flatMap { _.pathHistory }.distinct
								Right((destination, routes, bestResult.currentCost, node))
							}
					}
					// Checks whether it's possible to get a better result than one already found
					// Results are considered "unproven"
					// until the acquired minimum cost exceeds that found for the destination
					// Note: Only used in "exclusive" mode where each destination corresponds with a single node
					if (exclusive) {
						val (newlyProvenDestinations, remainsUnprovenDestinations) = unprovenDestinations
							.divideBy { nextMinCost <= _._3 }.toTuple
						// Remembers the proven destinations in order to prevent their removal
						provenDestinationsBuffer ++= newlyProvenDestinations.view.map { _._4 }
						// Each entry contains 4 values:
						//      1) New result node
						//      2) New result routes
						//      3) New best cost
						//      4) Node to possibly remove from the results
						val updatedUnprovenResults = remainsUnprovenDestinations
							.map { case (destination, previousRoutes, previousMinCost, previousNode) =>
								// Finds new search results which are better or as good as the results found before
								val arrived = newFinders
									.filter { o => destination(o.currentNode, o.currentNode.leavingEdges) }
									.filter { _.currentCost <= previousMinCost }
								// Case: New competing results found
								//       => Merges them to the previous results or overrides previous results with them
								if (arrived.nonEmpty) {
									val bestResults = arrived.filterMinBy { _.currentCost }
									val bestResult = bestResults.find { _.currentNode == previousNode }
										.getOrElse(bestResults.head)
									val newNode = bestResult.currentNode
									val discoveredRoutes = bestResults
										.filter { _.currentNode == newNode }.flatMap { _.pathHistory }
									
									// Case: The new results are better than those found before
									//       => Replaces the old results
									if (bestResult.currentCost < previousMinCost) {
										// If targeted different nodes, may remove the other node from the results
										(destination, newNode, discoveredRoutes.toOptimizedSeq, bestResult.currentCost,
											Some(previousNode).filterNot { _ == newNode })
									}
									// Case: The new results are as good as the previous
									//       => Merges them if they're for the same node, otherwise ignores them
									else if (newNode == previousNode)
										(destination, previousNode, previousRoutes ++ discoveredRoutes,
											previousMinCost, None)
									else
										(destination, previousNode, previousRoutes, previousMinCost, None)
								}
								// Case: No competing results found => Keeps the previous entry
								else
									(destination, previousNode, previousRoutes, previousMinCost, None)
							}
						
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
						resultsBuffer ++= foundDestinations
							.map { case (_, routes, cost, node) => node -> (routes -> cost) }
				}
				
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
						unprovenDestinations.view.map { _._3 }.maxOption.forall { _ <= nextMinCost })
				
				// Converts the search results into graph travel results
				// Result-based stages are processed immediately because the results collection is mutable
				val resultStages = resultsBuffer.view
					.map { case (node, (routes, cost)) =>
						NodeTravelStage(node, routes, cost, isDestination = true,
							isConfirmedAsOptimal = completed || cost <= nextMinCost)
					}
					.toVector
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
trait GraphNodeLike[+N, +E, +Repr <: GraphNodeLike[N, E, Repr, Edge], +Edge <: GraphEdgeLike[E, Repr]]
	extends View[N] with Extender[N]
{
    // TYPES    --------------------
	
    type Route = Seq[Edge]
    
    
    // ABSTRACT --------------------
	
	/**
	  * @return The edges leaving this node.
	  */
    def leavingEdges: Iterable[Edge]
	
	/**
	  * @return This node
	  */
	def self: Repr
    
    
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
	private def _allNodesIterator(visitedNodes: mutable.Set[Any]): Iterator[Repr] = {
		Iterator.single(self) ++ leavingEdges.iterator.flatMap { edge =>
			val node = edge.end
			if (visitedNodes.contains(node))
				None
			else {
				visitedNodes += node
				node._allNodesIterator(visitedNodes)
			}
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
	  *         1) Route to the node in question as a sequence of edges
	  *         2) The node at the end of that route
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
	 * Converts this node to a graph
	 * @return A graph based on this node's connections
	 */
	def toGraph =
		Graph(allNodesIterator
			.flatMap { node => node.leavingEdges.map { edge => (node.value, edge.value, edge.end.value) } }.toSet)
	
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
	def toValueTree: template.tree.ValueTree[N] = _toTree(Set(self)) { _.value }
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
	def toTree: template.tree.ValueTree[Repr] = _toTree(Set(self))(Identity)
	private def _toTree[A](traversedNodes: Set[Any])(wrapNode: Repr => A): template.tree.ValueTree[A] = {
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
	 * @return Finds all circular routes from this node to itself without traversing through any other node more than once
	 */
	def routesToSelf = routesTo(self)
	
	/**
	  * @return An interactive search process which targets all nodes in this graph
	  */
	def searchShortestRoutesToAll = searchAllNodes[Int] { _: Edge => 1 }
	
	
	// IMPLEMENTED  ----------------------
	
	override def wrapped: N = value
	
	override def toString = s"Node($value)"


	// OTHER	-----------------
	
	/**
	  * Traverses edges from this node once
	  * @param edgeType The content of the traversed edge(s)
	  * @return An iterator that yields the node(s) at the end of the edge(s)
	  */
	def /[E2 >: E](edgeType: E2) = leavingEdges.iterator.filter { _.value == edgeType }.map { _.end }
	/**
	  * Traverses a deep path that consists of edges between nodes
	  * @param path The content of the edges to travel in sequence, starting from edges of this node.
	  *             An empty path is considered to point to this node.
	  * @return The node(s) at the end of the path
	  */
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
	  * Finds all routes (edge combinations) that connect this node to the provided node. Routes
	  * can't contain the same node multiple times so no looping routes are included. An exception to this is the case
	  * where this node is targeted. In that case, the resulting routes start and end at this node.
	  * @param node The node this node may be connected to
	  * @return All possible routes to the provided node. In case this node is the searched node,
	  * however, a single empty route will be returned. The end node will always be at the end of
	  * each route and nowhere else. If there are no connecting routes, an empty array is returned.
	  */
	def routesTo(node: NodeTarget[N, E]): Iterable[Route] = {
		// If trying to find routes to self, will have to handle limitations a bit differently
		if (node(self, leavingEdges))
			leavingEdges.find { _.end == self } match {
				case Some(zeroRoute) => Single(Single(zeroRoute))
				case None => leavingEdges.flatMap { e => e.end.routesTo(node, Set()).map { route => e +: route } }
			}
		else
			routesTo(node, Set())
	}
	// Uses recursion
	private def routesTo(node: NodeTarget[N, E], visitedNodes: Set[Any]): Iterable[Route] = {
		// Tries to find the destination from each connected edge that leads to a new node
		val newVisitedNodes = visitedNodes + self
		
		// Checks whether there exist edges to the final node
		val availableEdges = leavingEdges.filterNot { e => newVisitedNodes.contains(e.end) }
		availableEdges.find { edge => node(edge.end, edge.end.leavingEdges) } match {
			case Some(directRoute) => Single(Single(directRoute))
			case None =>
				// If there didn't exist a direct path, tries to find an indirect one
				// Attaches this element at the beginning of each returned route (if there were any returned)
				availableEdges.flatMap { e => e.end.routesTo(node, newVisitedNodes).map { route => e +: route } }
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
	def shortestRoutesToOne(node: NodeTarget[N, E]) =
		cheapestRoutesToOne(node) { _ => 1 }
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
	  * Finds the shortest routes to a certain sub-group of nodes within this graph
	  * @param filter A function used for identifying, which nodes are targeted and which are not.
	  * @return Search results
	  */
	def shortestRoutesTo(filter: NodeTarget[N, E]) =
		cheapestRoutesTo(filter) { _ => 1 }
	/**
	  * Finds the cheapest routes to a certain sub-group of nodes within this graph
	  * @param filter A function used for identifying, which nodes are targeted and which are not.
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param n Numeric implementation for the cost values
	  * @tparam C Type of cost values used.
	  * @return Search results
	  */
	def cheapestRoutesTo[C](filter: NodeTarget[N, E])(costOf: Edge => C)(implicit n: Numeric[C]) =
		search(filter)(costOf).finish()
	/**
	  * Starts a graph search which looks for shortest routes (in terms of the number of edges).
	  * @param target A function for identifying the targeted nodes
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def searchShortestRoutesTo(target: NodeTarget[N, E]) =
		search(target) { _ => 1 }
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
	  * @param nodes Searched nodes
	  * @return Search results
	  */
	def shortestRoutesToEach(nodes: Iterable[NodeTarget[N, E]]) =
		cheapestRoutesToEach(nodes) { _ => 1 }
	/**
	  * Finds the cheapest routes to a certain group of nodes
	  * @param nodes Searched nodes
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param n Numeric implementation for the cost values
	  * @tparam C Type of cost values used.
	  * @return Search results
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
		
	private def _searchFor[C](destinations: Iterable[NodeTarget[N, E]], startCost: C, exclusive: Boolean)
	                            (costOf: Edge => C)(sumOf: (C, C) => C)
	                            (implicit ord: Ordering[C]) =
		new PathsFinder[N, E, Repr, Edge, C](self, destinations, startCost, exclusive)(costOf)(sumOf).iterator
}