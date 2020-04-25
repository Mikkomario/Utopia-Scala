package utopia.flow.datastructure.template

import utopia.flow.datastructure.immutable.Graph
import utopia.flow.util.CollectionExtensions._

import scala.math.Ordering.Double.TotalOrdering
import scala.collection.immutable.VectorBuilder

/**
 * Graph nodes contain content and are connected to other graph nodes via edges
 * @author Mikko Hilpinen
 * @since 10.4.2019
 */
trait GraphNode[+N, E, GNode <: GraphNode[N, E, GNode, Edge], Edge <: GraphEdge[N, E, GNode]] extends Node[N]
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
	 * Converts this node to a graph
	 * @tparam N2 Type of node content in the graph
	 * @return A graph based on this node's connections
	 */
	def toGraph[N2 >: N] =
	{
		val connectionsBuffer = new VectorBuilder[(N2, E, N2)]
		foreach { node => node.leavingEdges.foreach { edge =>
			val newConnection = (node.content, edge.content, edge.end.content)
			connectionsBuffer += newConnection
		} }
		Graph(connectionsBuffer.result().toSet)
	}
	
	
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
    def shortestRouteTo(node: AnyNode) = cheapestRouteTo(node, { _ => 1 })
    
    /**
     * Finds the 'cheapest' route from this node to the provided node, if there is one. A special
     * function is used for calculating the route cost.
     * @param node The node traversed to
     * @param costOf The function used for calculating the cost of a single edge (based on the edge
     * contents, for example)
     * @return The cheapest route found, if any exist
     */
    def cheapestRouteTo(node: AnyNode, costOf: Edge => Double) =
    {
        val routes = routesTo(node)
        if (routes.isEmpty)
            None
        else
            Some(routes.minBy { _.foldLeft(0.0) { (currentCost, edge) => currentCost + costOf(edge) } })
    }
    
    /**
     * Finds all routes (edge combinations) that connect this node to the provided node. Routes
     * can't contain the same edge multiple times so no looping routes are included.
     * @param node The node this node may be connected to
     * @return All possible routes to the provided node. In case this node is the searched node,
     * however, a single empty route will be returned. The end node will always be at the end of
     * each route and nowhere else. If there are no connecting routes, an empty array is returned.
     */
    def routesTo(node: AnyNode): Set[Route] = routesTo(node, Set())
    
    // Uses recursion
    private def routesTo(node: AnyNode, visitedNodes: Set[AnyNode]): Set[Route] =
    {
        if (node == this)
            Set(Vector())
        else
        {
    		// Tries to find the destination from each connected edge that leads to a new node
			val newVisitedNodes = visitedNodes + this
	
			// Records each found route. The traversed edge is included in the returned route(s)
			val availableEdges = leavingEdges.filterNot { e => newVisitedNodes.contains(e.end) }
			val routes = availableEdges.flatMap { e => e.end.routesTo(node, newVisitedNodes).map { route => e +: route } }
			
			routes
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
	
	private def foreach[U](operation: GNode => U, traversedNodes: Set[GNode]): Unit =
	{
		val nodes = traversedNodes + repr
		operation(repr)
		endNodes.diff(nodes).foreach { _.foreach(operation, nodes) }
	}
}