package utopia.flow.datastructure.immutable

import utopia.flow.datastructure.immutable.Graph.{GraphViewEdge, GraphViewNode}
import utopia.flow.datastructure.template.GraphNode
import utopia.flow.datastructure.template
import utopia.flow.util.CollectionExtensions._

object Graph
{
	trait GraphViewNode[N, E] extends GraphNode[N, E, GraphViewNode[N, E], GraphViewEdge[N, E]]
	
	trait GraphViewEdge[N, E] extends template.GraphEdge[N, E, GraphViewNode[N, E]]
	
	/**
	 * @param isTwoWayBound Whether the resulting graph should be two-way bound
	 * @tparam N Type of nodes in graph
	 * @tparam E Type of edges in graph
	 * @return An empty graph
	 */
	def empty[N, E](isTwoWayBound: Boolean = false) = Graph(Set[(N, E, N)](), isTwoWayBound)
	
	/**
	 * Creates a graph where the connections are two-way-bound (can be traversed either ways)
	 * @param connections Connections that form the graph
	 * @tparam N Type of node content
	 * @tparam E Type of edge content
	 * @return A new graph
	 */
	def twoWayBound[N, E](connections: Set[(N, E, N)]) = Graph(connections, isTwoWayBound = true)
}

/**
 * A graph that consists of set of unique nodes and edges between those nodes. Notice that by default the edges are
 * single direction only.
 * @author Mikko Hilpinen
 * @since 25.4.2020, v1.8
 */
case class Graph[N, E](connections: Set[(N, E, N)], isTwoWayBound: Boolean = false)
{
	// ATTRIBUTES	------------------------
	
	private lazy val nodesByContent = connections.flatMap { case (start, _, end) => Set(start, end) }.map { n =>
		n -> (GNode(n): GraphViewNode[N, E]) }.toMap
	private lazy val edgesByStartNode = connections.toMultiMap { case (start, content, end) =>
		start -> (GEdge(content, end): GraphViewEdge[N, E]) }
	private lazy val edgesByEndNode = connections.toMultiMap { case (start, content, end) =>
		end -> (GEdge(content, start): GraphViewEdge[N, E]) }
	
	/**
	 * @return All nodes within this graph
	 */
	lazy val nodes = nodesByContent.values.toSet
	
	/**
	 * @return All graphs within this graph that are not connected together. If all of the nodes in this graph are
	 *         connected, returns only a single graph (this).
	 */
	lazy val subGraphs =
	{
		// Collects graphs from nodes until all nodes have been collected
		var pulledGraphs = Set[Graph[N, E]]()
		nodes.foreach { startNode =>
			if (!pulledGraphs.exists { _.contains(startNode.content) })
				pulledGraphs += subGraphFrom(startNode.content)
		}
		pulledGraphs
	}
	
	
	// COMPUTED	----------------------------
	
	/**
	 * @return Whether this graph is empty
	 */
	def isEmpty = connections.isEmpty
	
	/**
	 * @return Whether this graph contains connections
	 */
	def nonEmpty = !isEmpty
	
	/**
	 * @return All edges within this graph
	 */
	def edges = connections.map { case (_, edge, end) => GEdge(edge, end): GraphViewEdge[N, E] }
	
	/**
	 * @return A copy of this graph where each edge points to the opposite direction
	 */
	def reversed = copy(connections = connections.map { case (start, edge, end) => (end, edge, start) })
	
	/**
	 * @return A copy of this graph where each node is connected with at least two edges, each pointing to
	 *         opposite direction.
	 */
	def twoWayBound = if (isTwoWayBound) this else copy(isTwoWayBound = true)
	
	
	// OTHER	----------------------------
	
	/**
	 * @param nodeContent Content of the node
	 * @return A node in this graph with specified content
	 */
	def node(nodeContent: N): GraphViewNode[N, E] = nodesByContent.getOrElse(nodeContent, GNode(nodeContent))
	
	/**
	 * @param nodeContent Content of the node
	 * @return A node in this graph with specified content
	 */
	def apply(nodeContent: N) = node(nodeContent)
	
	/**
	 * @param nodeContent Tested node content
	 * @return Whether this graph contains a link for the specified node
	 */
	def contains(nodeContent: N) = nodesByContent.contains(nodeContent)
	
	/**
	 * @param startNode Starting node content
	 * @return A graph that contains only the specified node and the nodes connected to that node directly or indirectly
	 */
	def subGraphFrom(startNode: N) = copy(connections = node(startNode).allNodes.flatMap { n =>
		n.leavingEdges.map { e => (n.content, e.content, e.end.content) } })
	
	/**
	 * Maps the contents of this graph
	 * @param nodeMapper Mapping function for node content
	 * @param edgeMapper Mapping function for edge content
	 * @tparam N2 New node content type
	 * @tparam E2 New edge content type
	 * @return A mapped copy of this graph
	 */
	def map[N2, E2](nodeMapper: N => N2)(edgeMapper: E => E2) = copy(connections = connections.map {
		case (start, edge, end) => (nodeMapper(start), edgeMapper(edge), nodeMapper(end)) })
	
	/**
	 * Maps all nodes in this graph
	 * @param f Mapping function for node content
	 * @tparam N2 New type of node content
	 * @return A mapped copy of this graph
	 */
	def mapNodes[N2](f: N => N2) = copy(connections = connections.map { case (start, edge, end) =>
		(f(start), edge, f(end)) })
	
	/**
	 * Maps all edge contents in this graph
	 * @param f A mapping function for edge content
	 * @tparam E2 New edge content
	 * @return A mapped copy of this graph
	 */
	def mapEdges[E2](f: E => E2) = copy(connections = connections.map { case (start, edge, end) =>
		(start, f(edge), end) })
	
	/**
	 * Filters the connections in this graph, only considering connection contents
	 * @param f A filtering function for connections based on connection contents
	 * @return A filtered copy of this graph
	 */
	def filterByContent(f: (N, E, N) => Boolean) = copy(connections =
		connections.filter { case (start, edge, end) => f(start, edge, end) })
	
	/**
	 * Filters the nodes in this graph by testing their content. Function will be applied only once for each unique
	 * node content.
	 * @param f A filter function for node contents
	 * @return A filtered copy of this graph
	 */
	def filterByNodeContent(f: N => Boolean) =
	{
		// Calls the filter function as rarely as possible
		var acceptedNodes = Set[N]()
		var rejectedNodes = Set[N]()
		def test(node: N) =
		{
			if (acceptedNodes.contains(node))
				true
			else if (rejectedNodes.contains(node))
				false
			else if (f(node))
			{
				acceptedNodes += node
				true
			}
			else
			{
				rejectedNodes += node
				false
			}
		}
		
		copy(connections = connections.filter { case (start, _, end) => test(start) && test(end) })
	}
	
	/**
	 * Filters this graph by testing edge content
	 * @param f A function for filtering edges by content
	 * @return A filtered copy of this graph
	 */
	def filterByEdgeContent(f: E => Boolean) = filterByContent { (_, edge, _) => f(edge) }
	
	/**
	 * Filters this graph by testing individual nodes. The filter function is called only once for each unique node.
	 * @param f A filter function for nodes
	 * @return A filtered copy of this graph
	 */
	def filterByNode(f: GraphViewNode[N, E] => Boolean) = filterByNodeContent { nodeContent =>
		f(node(nodeContent)) }
	
	/**
	 * Filters this graph by testing individual edges.
	 * @param f A filter function for edges
	 * @return A filtered copy of this graph
	 */
	def filterByEdge(f: GraphViewEdge[N, E] => Boolean) = filterByContent { (_, edge, end) =>
		f(GEdge(edge, end)) }
	
	/**
	 * Creates a copy of this graph with an edge added
	 * @param start Start node
	 * @param edge New edge content
	 * @param end End node
	 * @return A copy of this graph with additional edge
	 */
	def withEdge(start: N, edge: E, end: N) =
	{
		val newConnection = (start, edge, end)
		copy(connections = connections + newConnection)
	}
	
	/**
	 * @param connection A new connection (start -> edge content -> end)
	 * @return A copy of this graph with specified connection added
	 */
	def +(connection: (N, E, N)) = copy(connections = connections + connection)
	
	/**
	 * @param newConnections New connections (start -> edge content -> end)
	 * @return A copy of this graph with specified connections added
	 */
	def ++(newConnections: IterableOnce[(N, E, N)]) = copy(connections = connections ++ newConnections)
	
	/**
	 * @param other Another graph
	 * @return A combination of these two graphs
	 */
	def ++(other: Graph[N, E]) = copy(connections = connections ++ other.connections)
	
	/**
	 * @param node Node to exclude from this graph
	 * @return A copy of this graph with specified node excluded
	 */
	def withoutNode(node: N) = copy(connections =
		connections.filter { case (start, _, end) => start != node && end != node })
	
	/**
	 * @param node Node to exclude from this graph
	 * @return A copy of this graph with specified node excluded
	 */
	def -(node: N) = withoutNode(node)
	
	/**
	 * @param nodes Nodes to exclude from this graph
	 * @return A copy of this graph with specified nodes excluded
	 */
	def withoutNodes(nodes: Iterable[N]) = copy(connections = connections.filterNot { case (start, _, end) =>
		nodes.exists { n => start == n || end == n } })
	
	/**
	 * @param nodes Nodes to exclude from this graph
	 * @return A copy of this graph with specified nodes excluded
	 */
	def --(nodes: Iterable[N]) = withoutNodes(nodes)
	
	/**
	 * @param other Another graph
	 * @return A copy of this graph with none of the connections in the other graph
	 */
	def --(other: Graph[N, E]) = copy(connections = connections -- other.connections)
	
	
	// NESTED	----------------------------
	
	private case class GNode(content: N) extends GraphViewNode[N, E]
	{
		// May include edges to other way as well
		override lazy val leavingEdges = {
			val singleWay = edgesByStartNode.getOrElse(content, Set())
			if (isTwoWayBound)
				singleWay ++ edgesByEndNode.getOrElse(content, Set())
			else
				singleWay
		}
		
		override protected def repr = this
	}
	
	private case class GEdge(content: E, endContent: N) extends GraphViewEdge[N, E]
	{
		override lazy val end = nodesByContent(endContent)
	}
}