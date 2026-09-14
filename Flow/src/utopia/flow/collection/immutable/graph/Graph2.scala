package utopia.flow.collection.immutable.graph

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Graph.{GraphViewEdge, GraphViewNode}
import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.MaybeEmpty
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.caching.ResettableLazy

import scala.collection.mutable

/**
 * Represents a set of connections that form potentially multiple graphs
 * @author Mikko Hilpinen
 * @since 25.4.2020, v1.8
 */
// TODO: Add an implementation that accepts a set of nodes instead
// NB: Connections must not contain self-references
// TODO: Make covariant
class Graph2[N, E](connections: Iterable[(N, E, N)], isTwoWayBound: Boolean = false) extends MaybeEmpty[Graph2[N, E]]
{
	// ATTRIBUTES	------------------------
	
	private val generator = GraphNode.generator[N, E] { origin =>
		if (isTwoWayBound)
			connections.iterator.flatMap { case (end1, edgeValue, end2) =>
				if (end1 == origin)
					Some(edgeValue -> View.fixed(end2))
				else if (end2 == origin)
					Some(edgeValue -> View.fixed(end1))
				else
					None
			}
		else
			connections.iterator.filter { _._1 == origin }
				.map { case (_, edgeValue, endValue) => edgeValue -> View.fixed(endValue) }
	}
	
	val nodeValues = connections.iterator.flatMap { case (v1, _, v2) => Pair(v1, v2) }.distinct.caching
	/**
	 * @return All nodes within this graph
	 */
	val nodes = nodeValues.map(generator)
	
	/**
	 * @return All sets of nodes within this graph that are not connected with each other.
	 *         If all the nodes in this graph are connected, returns only a single set of nodes.
	 */
	val subgraphs = new DistinctGraphsIterator().caching
	
	
	// COMPUTED	----------------------------
	
	/**
	 * @return All edges within this graph
	 */
	def edges = nodes.flatMap { _.leavingEdges }
	
	/**
	 * @return A copy of this graph where each edge points to the opposite direction
	 */
	def reversed = new Graph2(connections.map { case (start, edge, end) => (end, edge, start) }, isTwoWayBound)
	
	/**
	 * @return A copy of this graph where each connection is counted twice (once in each direction)
	 */
	def twoWayBound = if (isTwoWayBound) this else new Graph2(connections, isTwoWayBound = true)
	
	
	// IMPLEMENTED  ------------------------
	
	override def self = this
	
	override def isEmpty = connections.isEmpty
	
	
	// OTHER	----------------------------
	
	/**
	 * @param nodeContent Content of the node
	 * @return A node in this graph with specified content
	 */
	def node(nodeContent: N) = generator(nodeContent)
	/**
	 * @param nodeContent Content of the node
	 * @return A node in this graph with specified content
	 */
	def apply(nodeContent: N) = node(nodeContent)
	
	// TODO: Continue refactoring
	
	/**
	 * @param nodeContent Content of the targeted node
	 * @return Edges pointing to that node
	 */
	def edgesTo(nodeContent: N) = edgesByEndNode.getOrElse(nodeContent, Set())
	
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
		n.leavingEdges.map { e => (n.value, e.value, e.end.value) } })
	
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
	 * @param edge An edge content to exclude from this graph
	 * @return A copy of this graph with all edges with the specified content removed
	 */
	def withoutEdge(edge: E) = copy(connections = connections.filter { case (_, e, _) => e != edge })
	
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
	
	private class DistinctGraphsIterator extends Iterator[Seq[GraphNode[N, E]]]
	{
		// ATTRIBUTES	--------------------
		
		private val valuesIterator = (connections.iterator.map { _._1 } ++ connections.iterator.map { _._3 }).distinct
		private val accessedNodes = mutable.Set[N]()
		private var lastNodeValuesIter = Iterator.empty[N]
		
		private val prepared = ResettableLazy {
			accessedNodes ++= lastNodeValuesIter
			valuesIterator.find { !accessedNodes.contains(_) }
		}
		
		
		// IMPLEMENTED  --------------------
		
		override def hasNext: Boolean = prepared.value.isDefined
		
		override def next(): Seq[GraphNode[N, E]] = {
			val nextValue = prepared.value.get
			val nextRoot = generator(nextValue)
			val nextNodes = nextRoot.allNodesIterator.caching
			
			lastNodeValuesIter = nextNodes.iterator.map { _.value }
			nextNodes
		}
	}
}