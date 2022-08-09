package utopia.flow.datastructure.template

import utopia.flow.datastructure.immutable.{Graph, Tree}
import utopia.flow.util.CollectionExtensions._

import scala.collection.immutable.VectorBuilder

/**
 * Graph nodes contain content and are connected to other graph nodes via edges
 * @author Mikko Hilpinen
 * @since 10.4.2019
 */
trait GraphNode[N, E, GNode <: GraphNode[N, E, GNode, Edge], Edge <: GraphEdge[N, E, GNode]] extends Node[N]
{
    // TYPES    --------------------
    
    type AnyNode = GraphNode[_, _, _, _]
    type AnyEdge = GraphEdge[_, _, _]
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
     * Finds the shortest (least amount of edges) route from this node to another node
     * @param node the node traversed to
     * @return The shortest route from this node to the provided node, if any exist
     */
    def shortestRouteTo(node: AnyNode) = cheapestRouteTo(node) { _ => 1 }
	
	/**
	 * Finds the shortest (least amount of edges) routes from this node to another node
	 * @param node the node traversed to
	 * @return The shortest routes from this node to the provided node, if any exist
	 */
	def shortestRoutesTo(node: AnyNode) = cheapestRoutesTo(node) { _ => 1 }
	
    /**
     * Finds the 'cheapest' route from this node to the provided node, if there is one. A special
     * function is used for calculating the route cost.
     * @param node The node traversed to
     * @param costOf The function used for calculating the cost of a single edge (based on the edge
     * contents, for example)
     * @return The cheapest route found, if any exist
     */
    // TODO: optimize
    def cheapestRouteTo(node: AnyNode)(costOf: Edge => Double): Option[Route] =
	    cheapestRoutesTo(node)(costOf).headOption
	
	/**
	 * Finds the 'cheapest' routes from this node to the provided node, provided there are any. A special
	 * function is used for calculating the route cost.
	 * @param node The node traversed to
	 * @param costOf The function used for calculating the cost of a single edge (based on the edge
	 * contents, for example)
	 * @return The cheapest routes found, if any exist
	 */
	def cheapestRoutesTo(node: AnyNode)(costOf: Edge => Double): Set[Route] =
		cheapestRoutesTo(node, 0.0, None, Set(this))(costOf) match {
			case Some((routes, _)) => routes
			case None => Set()
		}
	
	// visitedNodes is expected to contain "this" node already
	private def cheapestRoutesTo(node: AnyNode, currentCost: Double, currentMinCost: Option[Double],
	                              visitedNodes: Set[AnyNode])
	                             (costOf: Edge => Double): Option[(Set[Route], Double)] =
	{
		// Checks whether already arrived
		if (this == node)
			Some(Set[Route](Vector()) -> currentCost)
		else {
			// Checks available route options
			val availableEdges = leavingEdges.filterNot { e => visitedNodes.contains(e.end) }
			// Doesn't even consider moving to edges which are directly accessible from this node
			// (because why make the round-trip?)
			val newVisitedNodes = visitedNodes ++ availableEdges.map { _.end }
			
			// Orders the available edges in order to maximize the probability of finding a good route
			// Traverses each route fully before moving to the next one
			availableEdges.toVector.map { e => e -> costOf(e) }
				.sortedWith(Ordering.by { _._2 }, Ordering.by { _._1.end.leavingEdges.size })
				.foldLeft[Option[(Set[Route], Double)]](currentMinCost.map { Set[Route]() -> _ }) { case (bestRouteData, (edge, edgeCost)) =>
				val minCost = bestRouteData.map { _._2 }
				// Calculates next step cost
				val stepCost = currentCost + edgeCost
				// Will not continue to search the route if the minimum cost is exceeded
				if (minCost.exists { _ < stepCost })
					bestRouteData
				else {
					// Finds the routes from the linked node
					val routeResult = edge.end.cheapestRoutesTo(node, stepCost, minCost, newVisitedNodes)(costOf)
					// Remembers the route check results
					// cachedPaths.getOrElseUpdate(edge.end, mutable.HashMap()) += ((node: AnyRef) -> routeResult)
					// Checks whether the new route(s) are better than the previously found best routes
					routeResult match {
						// Case: Some routes were found that are better or as good as the ones found before
						case Some((newRoutes, newCost)) =>
							val fullNewRoutes = newRoutes.map { edge +: _ }
							bestRouteData.filter { _._2 <= newCost } match {
								// Case: The previously found routes are as good as the new ones => Combines
								case Some((oldBestRoutes, bestCost)) =>
									Some((oldBestRoutes ++ fullNewRoutes) -> bestCost)
								// Case: The new routes are better => Overwrites
								case None => Some(fullNewRoutes -> newCost)
							}
						// Case: No routes were found
						case None => bestRouteData
					}
				}
			}.filter { _._1.nonEmpty }
		}
	}
    
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