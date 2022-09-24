package utopia.flow.collection.template

import utopia.flow.collection.immutable.{Graph, Tree}
import utopia.flow.util.CollectionExtensions._
import utopia.flow.view.template.Viewable

import scala.annotation.tailrec
import scala.collection.immutable.VectorBuilder
import scala.math.Ordered.orderingToOrdered

object GraphNode
{
	type AnyNode = GraphNode[_, _, _, _]
	type AnyEdge = GraphEdge[_, _, _]
	
	private class PathsFinder[N, E, GNode <: GraphNode[N, E, GNode, Edge], Edge <: GraphEdge[N, E, GNode], C]
	(start: GNode, destinations: Set[GNode => Boolean], startCost: C)(costOf: Edge => C)(sumOf: (C, C) => C)
	(implicit ord: Ordering[C])
	{
		def apply(): Map[GNode, (Set[Vector[Edge]], C)] = {
			// Filters out destinations that are immediately completed
			val incompleteDestinations = destinations.filterNot { _(start) }
			// Case: All destinations are already reached
			if (incompleteDestinations.isEmpty) {
				if (destinations.nonEmpty)
					Map(start -> (Set(Vector[Edge]()) -> startCost))
				else
					Map()
			}
			// Case: Actual search is required
			else {
				val result = apply(Map(start -> new PathFinder(start, startCost, Set(Vector()))), Set(),
					incompleteDestinations, Vector(), Map())
				// Adds some auto-completed destinations, if there were some
				if (incompleteDestinations.size < destinations.size)
					result + (start -> (Set(Vector[Edge]()) -> startCost))
				else
					result
			}
		}
		
		@tailrec
		private def apply(origins: Map[GNode, PathFinder], pastNodes: Set[AnyNode],
		                  remainingDestinations: Iterable[GNode => Boolean],
		                  improvableDestinations: Iterable[(GNode => Boolean, C)],
		                  pastResults: Map[GNode, (Set[Vector[Edge]], C)]): Map[GNode, (Set[Vector[Edge]], C)] =
		{
			val currentMinCost = origins.valuesIterator.map { _.currentCost }.min
			// Selects the next iteration origins - Nodes with the lowest current cost
			val (delayedOrigins, iterationOrigins) = origins
				.divideBy { case (_, finder) => ord.equiv(finder.currentCost, currentMinCost) }
			val blockedNodes = pastNodes ++ iterationOrigins.keys
			// Takes the next step and merges routes that arrived to the same node
			val newFinders = iterationOrigins.values.flatMap { _.next(blockedNodes) }.groupBy { _.currentNode }
				.flatMap { case (location, finders) =>
					val bestNewCost = finders.map { _.currentCost }.min
					// Only recognizes the new results if they were better than some previously encountered origins
					// In which case either merges or discards the previous origins
					val earlierResults = origins.get(location)
					if (earlierResults.forall { _.currentCost >= bestNewCost }) {
						// Includes the previously found "origins" in the merging process
						val bestFinders = (finders ++ earlierResults)
							.filter { f => ord.equiv(f.currentCost, bestNewCost) }
						// Case: Only one finder arrived to this location => picks it as it is
						if (bestFinders.size == 1)
							Some(bestFinders.head)
						// Case: Merging => Takes the finders of smallest cost and combines them
						else
							Some(bestFinders.reduce { (a, b) =>
								new PathFinder(location, a.currentCost, a.pathHistory ++ b.pathHistory)
							})
					}
					else
						None
				}
			
			val (destinations, improvable, results) = {
				// Case: Iteration yielded no routes => keeps previous destinations etc.
				if (newFinders.isEmpty)
					(remainingDestinations, improvableDestinations, pastResults)
				// Case: Iteration yielded new routes => updates results and destinations, etc.
				else {
					// Checks whether already arrived to some or all destinations
					// Found destinations contains 4 parts:
					// 1) The original destination function
					// 2) Destination node
					// 3) Routes
					// 4) Cost
					val (nextDestinations, foundDestinations) = remainingDestinations.divideWith { destination =>
						val arrived = newFinders.filter { o => destination(o.currentNode) }
						if (arrived.isEmpty)
							Left(destination)
						else {
							val bestResults = arrived.minGroupBy { _.currentCost }
							val bestResult = bestResults.head
							// The latest result may still be improved upon
							Right((destination, bestResult.currentNode,
								bestResults.flatMap { _.pathHistory }.toSet, bestResult.currentCost))
						}
					}
					// Checks whether it's possible to get a better result than one already found
					val remainingImprovableDestinations = improvableDestinations.filter { currentMinCost <= _._2 }
					val betterResults = remainingImprovableDestinations.flatMap { case (destination, minCost) =>
						val arrived = newFinders.filter { o => destination(o.currentNode) }.filter { _.currentCost <= minCost }
						if (arrived.nonEmpty) {
							val bestResults = arrived.minGroupBy { _.currentCost }
							val bestResult = bestResults.head
							val newRoutes = pastResults.get(bestResult.currentNode)
								.filter { case (_, cost) => ord.equiv(cost, bestResult.currentCost) } match
							{
								case Some((pastRoutes, _)) => pastRoutes ++ bestResults.flatMap { _.pathHistory }
								case None => bestResults.flatMap { _.pathHistory }.toSet
							}
							Some(bestResult.currentNode -> (newRoutes, bestResult.currentCost))
						}
						else
							None
					}
					
					/*
					println()
					println(s"Current minimum cost: $currentMinCost")
					println(s"${origins.size} origins")
					println(s"${iterationOrigins.size} Current targets: [${ iterationOrigins.valuesIterator.map { _.currentNode.content }.mkString(", ") }]")
					println(s"${delayedOrigins.size} delayed targets: [${ delayedOrigins.valuesIterator.map { _.currentNode.content }.mkString(", ") }]")
					println(s"${blockedNodes.size} Nodes which may not be entered: [${ blockedNodes.map { _.content }.mkString(", ") }]")
					println(s"Targets after iteration: [${ newFinders.map { _.currentNode.content }.mkString(", ") }]")
					println(s"Found ${ foundDestinations.size } initial destination matches: [${ foundDestinations.map { _._4 }.mkString(", ") }]. ${ nextDestinations.size } destinations remain.")
					println(s"${ remainingImprovableDestinations.size } destinations / routes may still be improved upon")
					println(s"Found ${ betterResults.size } results that are better than the previous: [${ betterResults.map { _._2._2 }.mkString(", ") }]")
					 */
					val newResults = {
						if (foundDestinations.isEmpty && betterResults.isEmpty)
							pastResults
						else
							pastResults ++ betterResults ++
								foundDestinations.map { case (_, node, routes, cost) => node -> (routes -> cost) }
					}
					(nextDestinations, remainingImprovableDestinations ++
						foundDestinations.map { case (dest, _, _, cost) => dest -> cost } ,
						newResults)
				}
			}
			
			val nextFinders = delayedOrigins ++ newFinders.map { f => f.currentNode -> f }
			// Case: Impossible to continue => finishes
			if (nextFinders.isEmpty)
				results
			// Case: No need to continue further, as optimum routes have already been found
			else if (destinations.isEmpty && improvable.map { _._2 }.maxOption
				.forall { c => nextFinders.valuesIterator.forall { _.currentCost >= c } }) {
				/*
				println("No more search needed")
				println(s"Potential improvements: [${ improvable.map { _._2 }.mkString(", ") }]")
				println(s"Next min cost would have been: ${ nextFinders.valuesIterator.map { _.currentCost }.minOption }")
				 */
				results
			} // Case: Possible to and reasonable to continue => moves on to the next iteration
			else
				apply(nextFinders, blockedNodes, destinations, improvable, results)
		}
		
		private class PathFinder(val currentNode: GNode, val currentCost: C, val pathHistory: Set[Vector[Edge]])
		{
			def next(blockedNodes: Set[AnyNode]) = {
				currentNode.leavingEdges.filterNot { e => blockedNodes.contains(e.end) }
					.map { edge =>
						new PathFinder(edge.end, sumOf(currentCost, costOf(edge)), pathHistory.map { _ :+ edge })
					}
			}
		}
	}
}

/**
 * Graph nodes contain content and are connected to other graph nodes via edges
 * @author Mikko Hilpinen
 * @since 10.4.2019
 */
trait GraphNode[N, E, GNode <: GraphNode[N, E, GNode, Edge], Edge <: GraphEdge[N, E, GNode]] extends Viewable[N]
{
    // TYPES    --------------------
	
    type Route = Vector[Edge]
    
    
    // ABSTRACT --------------------
	
	/**
	  * @return The edges leaving this node
	  */
    def leavingEdges: Set[Edge]
	
	protected def repr: GNode
    
    
    // COMPUTED PROPERTIES    -------
    
    /**
     * The nodes accessible from this node
     */
    def endNodes = leavingEdges.map { _.end }
	
	/**
	  * @return All nodes accessible from this node, including this node
	  */
	def allNodes =
	{
		val buffer = new VectorBuilder[GNode]()
		foreach(buffer.+=)
		buffer.result().toSet
	}
	
	/**
	 * @return Content of all nodes linked with this node, including the contents of this node
	 */
	def allNodeContent =
	{
		val buffer = new VectorBuilder[N]
		foreach { buffer += _.content }
		buffer.result().toSet
	}
	
	/**
	 * Converts this node to a graph
	 * @return A graph based on this node's connections
	 */
	def toGraph =
	{
		val connectionsBuffer = new VectorBuilder[(N, E, N)]
		foreach { node => node.leavingEdges.foreach { edge =>
			val newConnection = (node.content, edge.content, edge.end.content)
			connectionsBuffer += newConnection
		} }
		Graph(connectionsBuffer.result().toSet)
	}
	
	/**
	 * Converts this graph to a tree
	 * @return A tree from this node
	 */
	def toTreeWithoutEdges = _toTreeWithoutEdges(Set())
	
	private def _toTreeWithoutEdges(traversedNodes: Set[Any]): Tree[N] =
	{
		val newTraversedNodes = traversedNodes + this
		val children = endNodes.filterNot { traversedNodes.contains(_) }.map { _._toTreeWithoutEdges(newTraversedNodes) }
		Tree(content, children.toVector)
	}
	
	/**
	 * @return Finds all circular routes from this node to itself without traversing trough any other node more than once
	 */
	def routesToSelf = routesTo(this)
	
	
	// OPERATORS	-----------------
	
	/**
	  * Traverses edges from this node once
	  * @param edgeType The content of the traversed edge(s)
	  * @return The node(s) at the end of the edge(s)
	  */
	def /(edgeType: E) = leavingEdges.filter { _.content == edgeType }.map { _.end }
	
	/**
	  * Traverses a deep path that consists of edges between nodes
	  * @param path The content of the edges to travel in sequence, starting from edges of this node
	  * @return The node(s) at the end of the path
	  */
	def /(path: Seq[E]): Set[GNode] =
	{
		if (path.isEmpty)
			Set()
		else
		{
			var current = /(path.head)
			path.drop(1).foreach { p => current = current.flatMap { _ / p } }
			
			current
		}
	}
	
	/**
	  * Traverses a deep path that consists of edges between nodes
	  * @param first The first edge to traverse
	  * @param second The second edge to traverse
	  * @param more More edges
	  * @return The node(s) at the end of the path
	  */
	def /(first: E, second: E, more: E*): Set[GNode] = this / (Vector(first, second) ++ more)
    
    
    // OTHER METHODS    ------------
    
	def isDirectlyConnectedTo(other: AnyNode) = edgeTo(other).isDefined
	
    /**
     * Finds an edge pointing to another node, if there is one
     * @param other The node this node may be connected to
     * @return an edge connecting the two nodes, if there is one. If there are multiple edges
     * pointing toward the specified node, returns one of them chosen randomly.
     * @see edgesTo(GraphNode[_, _])
     */
    def edgeTo(other: AnyNode) = leavingEdges.find { _.end == other }
    
    /**
     * Finds all edges pointing from this node to the provided node.
     * @param other another node
     * @return The edges pointing towards the provided node from this node
     */
    def edgesTo(other: AnyNode) = leavingEdges.filter { _.end == other }
	
	/**
	  * Finds the cheapest routes to multiple destination searches at once
	  * @param destinations Destinations to find, where each is a node search function
	  * @param startCost The initial (zero) route cost
	  * @param costOf A function for calculating the cost of a single route step (edge)
	  * @param sumOf A function for combining two costs together
	  * @param ord Implicit ordering for comparing costs
	  * @tparam C Type of calculated cost
	  * @return A map containing an entry for each <b>found</b> destination. Values of the map contain 2 parts:
	  *         1) All cheapest routes o that node and 2) the cost of those routes
	  */
	def findCheapestRoutesTo[C](destinations: Set[GNode => Boolean], startCost: C)
	                           (costOf: Edge => C)
	                           (sumOf: (C, C) => C)
	                           (implicit ord: Ordering[C]): Map[GNode, (Set[Vector[Edge]], C)] =
		new PathsFinder[N, E, GNode, Edge, C](repr, destinations, startCost)(costOf)(sumOf).apply()
	/**
	  * Finds the cheapest routes to multiple destination searches at once
	  * @param destinations Destinations to find, where each is a node search function
	  * @param costOf A function for calculating the cost of a single route step (edge)
	  * @param n Implicit numeric functions for the cost type
	  * @tparam C Type of calculated cost
	  * @return A map containing an entry for each <b>found</b> destination. Values of the map contain 2 parts:
	  *         1) All cheapest routes o that node and 2) the cost of those routes
	  */
	def findCheapestRoutesTo[C](destinations: Set[GNode => Boolean])
	                           (costOf: Edge => C)
	                           (implicit n: Numeric[C]): Map[GNode, (Set[Vector[Edge]], C)] =
		findCheapestRoutesTo[C](destinations, n.zero)(costOf)(n.plus)
	/**
	  * Finds the cheapest routes to a node found with a find function
	  * @param startCost The initial (zero) route cost
	  * @param find A find function that determines the destination node
	  * @param costOf A function for calculating the cost of a single route step (edge)
	  * @param sumOf A function for combining two costs together
	  * @param ord Implicit ordering for comparing costs
	  * @tparam C Type of calculated cost
	  * @return None if no route / destination was found.
	  *         Otherwise 1) the destination node, 2) cheapest routes to that node and 3) cost of those routes
	  */
	def findCheapestRoutes[C](startCost: C)(find: GNode => Boolean)(costOf: Edge => C)(sumOf: (C, C) => C)
	                           (implicit ord: Ordering[C]): Option[(GNode, Set[Vector[Edge]], C)] =
		findCheapestRoutesTo[C](Set(find), startCost)(costOf)(sumOf).headOption
			.map { case (node, (routes, cost)) => (node, routes, cost) }
	/**
	  * Finds the cheapest routes to a node found with a find function
	  * @param find A find function that determines the destination node
	  * @param costOf A function for calculating the cost of a single route step (edge)
	  * @param n Implicit numeric functions for the cost type
	  * @tparam C Type of calculated cost
	  * @return None if no route / destination was found.
	  *         Otherwise 1) the destination node, 2) cheapest routes to that node and 3) cost of those routes
	  */
	def findCheapestRoutes[C](find: GNode => Boolean)(costOf: Edge => C)
	                         (implicit n: Numeric[C]): Option[(GNode, Set[Vector[Edge]], C)] =
		findCheapestRoutes[C](n.zero)(find)(costOf)(n.plus)
	
	/**
	  * Finds the cheapest routes to multiple nodes at once
	  * @param destinations Nodes to find
	  * @param startCost The initial (zero) route cost
	  * @param costOf A function for calculating the cost of a single route step (edge)
	  * @param sumOf A function for combining two costs together
	  * @param ord Implicit ordering for comparing costs
	  * @tparam C Type of calculated cost
	  * @return A map containing an entry for each <b>found</b> destination. Values of the map contain 2 parts:
	  *         1) All cheapest routes o that node and 2) the cost of those routes
	  */
	def cheapestRoutesToMany[C](destinations: Set[AnyNode], startCost: C)(costOf: Edge => C)(sumOf: (C, C) => C)
	                       (implicit ord: Ordering[C]): Map[GNode, (Set[Vector[Edge]], C)] =
		findCheapestRoutesTo[C](destinations.map { d => _ == d }, startCost)(costOf)(sumOf)
	/**
	  * Finds the cheapest routes to multiple nodes at once
	  * @param destinations Nodes to find
	  * @param costOf A function for calculating the cost of a single route step (edge)
	  * @param n Implicit numeric functions for the cost type
	  * @tparam C Type of calculated cost
	  * @return A map containing an entry for each <b>found</b> destination. Values of the map contain 2 parts:
	  *         1) All cheapest routes o that node and 2) the cost of those routes
	  */
	def cheapestRoutesToMany[C](destinations: Set[AnyNode])(costOf: Edge => C)
	                       (implicit n: Numeric[C]): Map[GNode, (Set[Vector[Edge]], C)] =
		cheapestRoutesToMany[C](destinations, n.zero)(costOf)(n.plus)
	/**
	  * Finds the cheapest routes to another node
	  * @param destination The node to reach
	  * @param startCost The initial (zero) route cost
	  * @param costOf A function for calculating the cost of a single route step (edge)
	  * @param sumOf A function for combining two costs together
	  * @param ord Implicit ordering for comparing costs
	  * @tparam C Type of calculated cost
	  * @return 1) Routes found to to the targeted node (may be empty),
	  *         2) Cost of those routes (zero if no routes were found)
	  */
	def cheapestRoutesTo[C](destination: AnyNode, startCost: C)(costOf: Edge => C)(sumOf: (C, C) => C)
	                       (implicit ord: Ordering[C]): (Set[Vector[Edge]], C) =
		cheapestRoutesToMany[C](Set(destination), startCost)(costOf)(sumOf).headOption match {
			case Some((_, result)) => result
			case None => Set[Vector[Edge]]() -> startCost
		}
	/**
	  * Finds the cheapest routes to another node
	  * @param destination The node to reach
	  * @param costOf A function for calculating the cost of a single route step (edge)
	  * @param n Implicit numeric functions for the cost type
	  * @tparam C Type of calculated cost
	  * @return 1) Routes found to to the targeted node (may be empty),
	  *         2) Cost of those routes (zero if no routes were found)
	  */
	def cheapestRoutesTo[C](destination: AnyNode)(costOf: Edge => C)(implicit n: Numeric[C]): (Set[Vector[Edge]], C) =
		cheapestRoutesTo[C](destination, n.zero)(costOf)(n.plus)
	
	/**
	  * Finds one cheapest route to a node found with a search function
	  * @param find A search function for the destination node
	  * @param costOf A function for calculating the cost of a single route step (edge)
	  * @param n Implicit numeric functions for the cost type
	  * @tparam C Type of cost used
	  * @return None if no destination or route was found. Otherwise 1) The destination node and 2) cheapest route to that node
	  */
	def findCheapestRoute[C](find: GNode => Boolean)(costOf: Edge => C)(implicit n: Numeric[C]) =
		findCheapestRoutesTo[C](Set(find))(costOf).headOption.map { case (node, (routes, _)) => node -> routes.head }
	
	/**
	  * Finds one cheapest route to a node
	  * @param destination The destination node
	  * @param costOf A function for calculating the cost of a single route step (edge)
	  * @param n Implicit numeric functions for the cost type
	  * @tparam C Type of cost used
	  * @return None if no route was found. Otherwise the cheapest route found.
	  */
	def cheapestRouteTo[C](destination: AnyNode)(costOf: Edge => C)(implicit n: Numeric[C]) =
		cheapestRoutesToMany(Set(destination))(costOf).headOption.map { _._2._1.head }
	
	/**
	  * Finds the shortest routes to multiple destination searches at once
	  * @param destinations Destinations to find, where each is a node search function
	  * @return A map containing an entry for each <b>found</b> destination. Values of the map contain all shortest
	  *         routes to that node
	  */
	def findShortestRoutesTo(destinations: Set[GNode => Boolean]) =
		findCheapestRoutesTo[Int](destinations) { _ => 1 }.view.mapValues { _._1 }.toMap
	/**
	  * Finds the shortest routes to a destination found with a search function
	  * @param find A search function for the destination node
	  * @return None if no destination / route was found.
	  *         Otherwise the node that was found and the shortest routes to that node.
	  */
	def findShortestRoutes(find: GNode => Boolean) =
		findCheapestRoutes[Int](find) { _ => 1 }.map { case (node, routes, _) => node -> routes }
	/**
	  * Finds one shortest route to a destination found with a search function
	  * @param find A search function for the destination node
	  * @return None if no destination / route was found.
	  *         Otherwise the node that was found and the shortest route to that node.
	  */
	def findShortestRoute(find: GNode => Boolean) = findCheapestRoute[Int](find) { _ => 1 }
	
	/**
	  * Finds the shortest routes to multiple destinations at once
	  * @param destinations Destinations nodes
	  * @return A map containing an entry for each <b>found</b> destination. Values of the map contain all shortest
	  *         routes to that node
	  */
	def shortestRoutesToMany(destinations: Set[AnyNode]) =
		findShortestRoutesTo(destinations.map { d => _ == d })
	/**
	  * Finds the shortest routes to another node
	  * @param destination the destination node
	  * @return Shortest routes to that node. Empty if no routes were found.
	  */
	def shortestRoutesTo(destination: AnyNode) = cheapestRoutesTo[Int](destination) { _ => 1 }._1
	/**
	  * Finds one shortest route to another node
	  * @param destination the destination node
	  * @return Shortest route to that node. None if no routes were found.
	  */
	def shortestRouteTo(destination: AnyNode) = cheapestRouteTo[Int](destination) { _ => 1 }
    
    /**
     * Finds all routes (edge combinations) that connect this node to the provided node. Routes
     * can't contain the same node multiple times so no looping routes are included. An exception to this is the case
	 * where this node is targeted. In that case, the resulting routes start and end at this node.
     * @param node The node this node may be connected to
     * @return All possible routes to the provided node. In case this node is the searched node,
     * however, a single empty route will be returned. The end node will always be at the end of
     * each route and nowhere else. If there are no connecting routes, an empty array is returned.
     */
    def routesTo(node: AnyNode): Set[Route] =
	{
		// If trying to find routes to self, will have to handle limitations a bit differently
		if (node == this)
		{
			leavingEdges.find { _.end == this } match
			{
				case Some(zeroRoute) => Set(Vector(zeroRoute))
				case None => leavingEdges.flatMap { e => e.end.routesTo(this, Set()).map { route => e +: route } }
			}
		}
		else
			routesTo(node, Set())
	}
    
    // Uses recursion
    private def routesTo(node: AnyNode, visitedNodes: Set[AnyNode]): Set[Route] =
    {
		// Tries to find the destination from each connected edge that leads to a new node
		val newVisitedNodes = visitedNodes + this
	
		// Checks whether there exist edges to the final node
		val availableEdges = leavingEdges.filterNot { e => newVisitedNodes.contains(e.end) }
		availableEdges.find { _.end == node } match
		{
			case Some(directRoute) => Set(Vector(directRoute))
			case None =>
				// If there didn't exist a direct path, tries to find an indirect one
				// Attaches this element at the beginning of each returned route (if there were any returned)
				availableEdges.flatMap { e => e.end.routesTo(node, newVisitedNodes).map { route => e +: route } }
		}
    }
	
	/**
	  * Performs a recursive check and looks whether this node is at all connected to the specified node
	  * @param other Another node
	  * @return Whether this node is at all connected to the specified node
	  */
	def isConnectedTo(other: AnyNode) = traverseWhile { _.isDirectlyConnectedTo(other) }
	
	/**
	  * Traverses this graph until the provided operation returns true
	  * @param operation An operation performed on each node until it returns true
	  * @return Whether the operation ever returned true
	  */
	def traverseWhile(operation: GNode => Boolean) = traverseUntil
	{
		n =>
			val result = operation(n)
			if (result) Some(result) else None
	} getOrElse false
	
	/**
	  * Traverses this graph until a node is found that produces a result
	  * @param operation An operation performed on each node until a suitable one is found
	  * @tparam B Operation result type
	  * @return Final operation result. None if operation returned None on all nodes
	  */
	def traverseUntil[B](operation: GNode => Option[B]): Option[B] = traverseUntil(operation, Set())
	
	private def traverseUntil[B](operation: GNode => Option[B], traversedNodes: Set[GNode]): Option[B] =
	{
		val nodes = traversedNodes + repr
		
		// Performs the operation on self first
		// If that didn't yield a result, tries children instead
		operation(repr).orElse(endNodes.diff(nodes).findMap { _.traverseUntil(operation, nodes) })
	}
	
	/**
	  * Performs an operation on all of the nodes in this graph
	  * @param operation An operation
	  * @tparam U Arbitary result type
	  */
	def foreach[U](operation: GNode => U): Unit = foreach(operation, Set())
	
	private def foreach[U](operation: GNode => U, traversedNodes: Set[AnyNode]): Set[AnyNode] =
	{
		val newTraversedNodes = traversedNodes + this
		operation(repr)
		endNodes.foldLeft(newTraversedNodes) { (traversed, node) =>
			if (traversed.contains(node))
				traversed
			else
				node.foreach(operation, traversed)
		}
	}
}