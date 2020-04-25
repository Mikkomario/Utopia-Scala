package utopia.flow.datastructure.immutable

import utopia.flow.datastructure.immutable.Graph.{GraphViewEdge, GraphViewNode}
import utopia.flow.datastructure.template.GraphNode
import utopia.flow.datastructure.template
import utopia.flow.util.CollectionExtensions._

object Graph
{
	trait GraphViewNode[N, E] extends GraphNode[N, E, GraphViewNode[N, E], GraphViewEdge[N, E]]
	
	trait GraphViewEdge[N, E] extends template.GraphEdge[N, E, GraphViewNode[N, E]]
}

/**
 * A graph that consists of set of unique nodes and edges between those nodes
 * @author Mikko Hilpinen
 * @since 25.4.2020, v1.8
 */
case class Graph[N, E](connections: Set[(N, E, N)])
{
	// ATTRIBUTES	------------------------
	
	private lazy val edgesByStartNode = connections.toMultiMap { case (start, content, end) => start -> (content -> end) }
	
	
	// COMPUTED	----------------------------
	
	/**
	 * @return All nodes within this graph
	 */
	def nodes = connections.flatMap { case (start, _, end) =>
		Vector[GraphViewNode[N, E]](GNode(start), GNode(end)) }
	
	/**
	 * @return All edges within this graph
	 */
	def edges = connections.map { case (_, edge, end) => GEdge(edge, end): GraphViewEdge[N, E] }
	
	
	// OTHER	----------------------------
	
	/**
	 * @param nodeContent Content of the node
	 * @return A node in this graph with specified content
	 */
	def node(nodeContent: N): GraphViewNode[N, E] = GNode(nodeContent)
	
	/**
	 * @param startNode Starting node content
	 * @return A graph that contains only the specified node and the nodes connected to that node directly or indirectly
	 */
	def subGraphFrom(startNode: N) = Graph(node(startNode).allNodes.flatMap { n =>
		n.leavingEdges.map { e => (n.content, e.content, e.end.content) } })
	
	/**
	 * Maps the contents of this graph
	 * @param nodeMapper Mapping function for node content
	 * @param edgeMapper Mapping function for edge content
	 * @tparam N2 New node content type
	 * @tparam E2 New edge content type
	 * @return A mapped copy of this graph
	 */
	def map[N2, E2](nodeMapper: N => N2)(edgeMapper: E => E2) = Graph(connections.map {
		case (start, edge, end) => (nodeMapper(start), edgeMapper(edge), nodeMapper(end)) })
	
	/**
	 * Maps all nodes in this graph
	 * @param f Mapping function for node content
	 * @tparam N2 New type of node content
	 * @return A mapped copy of this graph
	 */
	def mapNodes[N2](f: N => N2) = Graph(connections.map { case (start, edge, end) =>
		(f(start), edge, f(end)) })
	
	/**
	 * Maps all edge contents in this graph
	 * @param f A mapping function for edge content
	 * @tparam E2 New edge content
	 * @return A mapped copy of this graph
	 */
	def mapEdges[E2](f: E => E2) = Graph(connections.map { case (start, edge, end) => (start, f(edge), end) })
	
	/**
	 * Filters the connections in this graph, only considering connection contents
	 * @param f A filtering function for connections based on connection contents
	 * @return A filtered copy of this graph
	 */
	def filterByContent(f: (N, E, N) => Boolean) = Graph(connections.filter { case (start, edge, end) => f(start, edge, end) })
	
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
		
		Graph(connections.filter { case (start, _, end) => test(start) && test(end) })
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
	def filterByNode(f: GraphViewNode[N, E] => Boolean) = filterByNodeContent { nodeContent => f(node(nodeContent)) }
	
	/**
	 * Filters this graph by testing individual edges.
	 * @param f A filter function for edges
	 * @return A filtered copy of this graph
	 */
	def filterByEdge(f: GraphViewEdge[N, E] => Boolean) = filterByContent { (_, edge, end) => f(GEdge(edge, end)) }
	
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
		Graph(connections + newConnection)
	}
	
	/**
	 * @param connection A new connection (start -> edge content -> end)
	 * @return A copy of this graph with specified connection added
	 */
	def +(connection: (N, E, N)) = Graph(connections + connection)
	
	/**
	 * @param newConnections New connections (start -> edge content -> end)
	 * @return A copy of this graph with specified connections added
	 */
	def ++(newConnections: IterableOnce[(N, E, N)]) = Graph(connections ++ newConnections)
	
	/**
	 * @param node Node to exclude from this graph
	 * @return A copy of this graph with specified node excluded
	 */
	def withoutNode(node: N) = Graph(connections.filter { case (start, _, end) => start != node && end != node })
	
	/**
	 * @param node Node to exclude from this graph
	 * @return A copy of this graph with specified node excluded
	 */
	def -(node: N) = withoutNode(node)
	
	/**
	 * @param nodes Nodes to exclude from this graph
	 * @return A copy of this graph with specified nodes excluded
	 */
	def withoutNodes(nodes: Iterable[N]) = Graph(connections.filterNot { case (start, _, end) =>
		nodes.exists { n => start == n || end == n } })
	
	/**
	 * @param nodes Nodes to exclude from this graph
	 * @return A copy of this graph with specified nodes excluded
	 */
	def --(nodes: Iterable[N]) = withoutNodes(nodes)
	
	
	// NESTED	----------------------------
	
	private case class GNode(content: N) extends GraphViewNode[N, E]
	{
		override lazy val leavingEdges = edgesByStartNode(content).map { case (content, end) => GEdge(content, end) }
		
		override protected def repr = this
	}
	
	private case class GEdge(content: E, endContent: N) extends GraphViewEdge[N, E]
	{
		override lazy val end = GNode(endContent)
	}
}