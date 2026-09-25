package utopia.flow.collection.immutable.graph

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.collection.immutable.caching.iterable.CachingSeq
import utopia.flow.collection.immutable.graph.Graph.GraphFactory
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Pair, Single}
import utopia.flow.collection.mutable.iterator.LazyInitIterator
import utopia.flow.operator.{Identity, MaybeEmpty}
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.caching.ResettableLazy

import scala.collection.mutable
import scala.language.implicitConversions

object Graph
{
	// ATTRIBUTES   -------------------------
	
	/**
	 * A factory used for constructing graphs
	 */
	val factory = GraphFactory()
	
	
	// IMPLICIT -----------------------------
	
	// Implicitly treats this object as a factory
	implicit def objectAsFactory(o: Graph.type): GraphFactory = o.factory
	
	
	// NESTED   -----------------------------
	
	case class GraphFactory(isLazy: Boolean = false)
	{
		/**
		 * @return A copy of this factory that builds the graphs lazily,
		 *         and where graph-modifications are made lazily, also.
		 */
		def lazily = if (isLazy) this else copy(isLazy = true)
		
		/**
		 * @tparam N Type of node values
		 * @tparam E Type of edge values
		 * @return An empty graph
		 */
		def empty[N, E]: Graph[N, E] = new EmptyGraph(isLazy)
		
		/**
		 * Creates a new graph using a set of connections
		 * @param connections Connections that form this graph.
		 *                    Each connection consists of 3 parts:
		 *                    1. Value of the node from which an edge originates
		 *                    1. Value assigned for the connecting edge
		 *                    1. Value of the node to which the edge points
		 * @param twoWayBound Whether two-way binding should be applied.
		 *                    Two-way binding means that all connections will be counted twice:
		 *                    Once as they are and once reversed.
		 *                    Default = false.
		 * @tparam N Type of node values in this graph
		 * @tparam E Type of edge values in this graph
		 * @return A new graph based on the specified connections
		 */
		def withConnections[N, E](connections: IterableOnce[(N, E, N)], twoWayBound: Boolean = false) = {
			if (!twoWayBound && connections.knownSize == 0)
				empty
			else {
				val appliedConnections = connections match {
					case v: scala.collection.View[(N, E, N)] => if (isLazy) v.caching else v.toOptimizedSeq
					case i: Iterable[(N, E, N)] => i
					case i => if (isLazy) i.caching else OptimizedIndexedSeq.from(i)
				}
				_apply(appliedConnections, twoWayBound)
			}
		}
		
		/**
		 * @param nodes Nodes that form this graph
		 * @tparam N Type of node values
		 * @tparam E Type of edge values
		 * @return A graph consisting of the specified nodes
		 */
		def apply[N, E](nodes: IterableOnce[GraphNode[N, E]]) = nodes match {
			case v: scala.collection.View[GraphNode[N, E]] =>
				if (isLazy) _fromNodes(v.caching) else _fromNodes(v.toOptimizedSeq)
			case i: Seq[GraphNode[N, E]] => _fromNodes(i)
			case i => if (isLazy) _fromNodes(i.caching) else _fromNodes(i.toOptimizedSeq)
		}
		
		private def _apply[N, E](connections: Iterable[(N, E, N)], twoWayBound: Boolean): Graph[N, E] =
			new GraphFromConnections[N, E](connections, twoWayBound, isLazy)
			
		private def _fromNodes[N, E](nodes: Seq[GraphNode[N, E]]): Graph[N, E] =
			new GraphFromNodes[N, E](nodes, isLazy)
	}
	
	private class EmptyGraph(override val isLazy: Boolean) extends Graph[Nothing, Nothing]
	{
		// ATTRIBUTES   --------------------------
		
		override val isEmpty: Boolean = true
		override val isTwoWayBound: Boolean = false
		
		override val connectionsIterator: Iterator[(Nothing, Nothing, Nothing)] = Iterator.empty
		
		override val nodeValues = Empty
		override val nodes = Empty
		override val headOption: Option[GraphNode[Nothing, Nothing]] = None
		
		override val edgesByTarget: Map[Any, (GraphNode[Nothing, Nothing], Seq[(GraphNode[Nothing, Nothing], GraphEdge[Nothing, Nothing])])] =
			Map()
			
		
		// IMPLEMENTED  --------------------------
		
		override def lazily: Graph[Nothing, Nothing] = if (isLazy) this else new EmptyGraph(isLazy = true)
		override def reversed: Graph[Nothing, Nothing] = this
		
		override def subgraphs = Single(this)
		
		override def node[N2 >: Nothing](nodeValue: N2): GraphNode[N2, Nothing] = GraphNode(nodeValue)
		
		override def filterNodeValues(f: Nothing => Boolean): Graph[Nothing, Nothing] = this
		override def filterNodes(f: GraphNode[Nothing, Nothing] => Boolean): Graph[Nothing, Nothing] = this
		override def filterEdgeValues(f: Nothing => Boolean): Graph[Nothing, Nothing] = this
		override def filterEdges(f: (GraphNode[Nothing, Nothing], GraphEdge[Nothing, Nothing]) => Boolean): Graph[Nothing, Nothing] = this
		override def filterConnections(f: (Nothing, Nothing, Nothing) => Boolean): Graph[Nothing, Nothing] = this
		
		override def map[N2, E2](mapNode: Nothing => N2)(mapEdge: Nothing => E2): Graph[N2, E2] = this
		
		override def subgraphFrom[N2 >: Nothing](startNodeValue: N2): Graph[N2, Nothing] = this
		
		override def +[N2 >: Nothing, E2 >: Nothing](connection: (N2, E2, N2)): Graph[N2, E2] =
			_withConnections(Single(connection))
		override def ++[N2 >: Nothing, E2 >: Nothing](other: Graph[N2, E2]): Graph[N2, E2] = other
		override def ++[N2 >: Nothing, E2 >: Nothing](newConnections: IterableOnce[(N2, E2, N2)]): Graph[N2, E2] =
			_withConnections(newConnections)
	}
	
	private class GraphFromConnections[N, E](connections: Iterable[(N, E, N)], override val isTwoWayBound: Boolean,
	                                         override val isLazy: Boolean)
		extends Graph[N, E]
	{
		// ATTRIBUTES	------------------------
		
		private val generator = GraphNode.generator[N, E] { origin =>
			// Case: Two-way bound graph => Counts all connections both ways
			if (isTwoWayBound)
				connections.iterator.flatMap { case (end1, edgeValue, end2) =>
					if (end1 == origin) {
						// Case: Self-connection => Ignored
						if (end2 == origin)
							None
						else
							Some(edgeValue -> View.fixed(end2))
					}
					else if (end2 == origin)
						Some(edgeValue -> View.fixed(end1))
					else
						None
				}
			else
				connections.iterator
					// Ignores self-connections
					.filter { case (from, _, to) => from == origin && to != origin }
					.map { case (_, edgeValue, endValue) => edgeValue -> View.fixed(endValue) }
		}
		
		override val nodeValues = {
			val valuesIter = connections.iterator.flatMap { case (v1, _, v2) => Pair(v1, v2) }.distinct
			if (isLazy) valuesIter.caching else valuesIter.toOptimizedSeq
		}
		override val nodes = nodeValues.map(generator)
		
		override lazy val edgesByTarget = _edgesByTarget
		
		override val subgraphs = LazyInitIterator { subGraphsIteratorFrom(nodeValues.iterator) }.caching
		
		/**
		 * A lazily initialized version of this graph that's based on realized nodes instead of connection data.
		 * Used for optimizing certain method implementations.
		 */
		private val lazyNodeGraph = Lazy { this.factory(nodes) }
		/**
		 * A view that contains Some if a fully realized node graph is available without additional computation.
		 * Used for optimizing certain method implementations.
		 */
		private val fullyRealizedView = {
			// Case: Lazily initialized graph
			//       => Node-based version is computed only once all nodes have resolved (or if called from elsewhere)
			if (isLazy)
				nodes match {
					case caching: CachingSeq[GraphNode[N, E]] =>
						Lazy.conditional {
							if (lazyNodeGraph.isInitialized || caching.isFullyCached)
								Some(lazyNodeGraph.value)
							else
								None
						} { _.isDefined }
					case _ => lazyNodeGraph.lightMap(Some.apply)
				}
			// Case: Fully realized graph => The node-based version may be used immediately
			else
				lazyNodeGraph.lightMap(Some.apply)
		}
		
		
		// IMPLEMENTED  ------------------------
		
		override def isEmpty = connections.isEmpty
		
		override def connectionsIterator: Iterator[(N, E, N)] = connections.iterator
		
		override def headOption: Option[GraphNode[N, E]] = nodeValues.headOption.map(generator)
		
		override def lazily = if (isLazy) this else this.factory.lazily(nodes)
		
		override def node[N2 >: N](nodeValue: N2): GraphNode[N2, E] = nodeValues.find { _ == nodeValue } match {
			case Some(v) => generator(v)
			case None => GraphNode(nodeValue)
		}
		
		// Uses the node-based version, if available
		override def filterNodeValues(f: N => Boolean): Graph[N, E] = fullyRealizedView.value match {
			case Some(nodes) => nodes.filterNodeValues(f)
			case None => _filterNodeValues(f)
		}
		// Uses the node-based version, if available
		override def filterNodes(f: GraphNode[N, E] => Boolean): Graph[N, E] = fullyRealizedView.value match {
			case Some(nodes) => nodes.filterNodes(f)
			case None => _filterNodeValues { v => f(generator(v)) }
		}
		
		override def filterEdgeValues(f: E => Boolean): Graph[N, E] =
			filterConnections { case (_, edge, _) => f(edge) }
		
		// Delegates the implementation to the node-based version
		override def filterEdges(f: (GraphNode[N, E], GraphEdge[N, E]) => Boolean): Graph[N, E] =
			lazyNodeGraph.value.filterEdges(f)
		
		// Uses the node-based version, if available
		override def map[N2, E2](mapNode: N => N2)(mapEdge: E => E2) = fullyRealizedView.value match {
			case Some(nodes) => nodes.map(mapNode)(mapEdge)
			case None =>
				// Caches node-mapping results to avoid repeated map function calls
				val nodeResultCache = Cache(mapNode)
				_withConnections(connections.iterator.map { case (from, edge, to) =>
					(nodeResultCache(from), mapEdge(edge), nodeResultCache(to))
				})
		}
		
		override def +[N2 >: N, E2 >: E](connection: (N2, E2, N2)): Graph[N2, E2] = {
			if (isLazy)
				connections match {
					case c: CachingSeq[(N, E, N)] =>
						if (c.current.contains(connection))
							this
						else
							_withConnections(c.appendIfDistinct(connection))
						
					case s: Seq[(N, E, N)] =>
						if (s.contains(connection))
							this
						else
							_withConnections(s.appendIfDistinct(connection))
				}
			else if (connections.exists { _ == connection })
				this
			else
				_withConnections(connections ++ Single(connection))
		}
		
		private def _filterNodeValues(f: N => Boolean) = {
			val resultCache = Cache(f)
			filterConnections { case (from, _, to) => resultCache(from) && resultCache(to) }
		}
	}
	
	private class GraphFromNodes[N, E](override val nodes: Seq[GraphNode[N, E]], override val isLazy: Boolean)
		extends Graph[N, E]
	{
		// ATTRIBUTES   -----------------------
		
		override val isTwoWayBound: Boolean = false
		
		override val nodeValues: Seq[N] = {
			if (isLazy)
				nodes match {
					case caching: CachingSeq[GraphNode[N, E]] => caching.map { _.value }
					case nodes => nodes.view.map { _.value }.caching
				}
			else
				nodes.map { _.value }
		}
		
		override lazy val edgesByTarget = _edgesByTarget
		
		override val subgraphs = LazyInitIterator { subGraphsIteratorFrom(nodeValues.iterator) }.caching
		
		
		// IMPLEMENTED  -----------------------
		
		override def isEmpty: Boolean = nodes.isEmpty
		
		override def headOption: Option[GraphNode[N, E]] = nodes.headOption
		
		override def connectionsIterator: Iterator[(N, E, N)] = nodes.iterator.flatMap { node =>
			node.leavingEdges.iterator.map { edge => (node.value, edge.value, edge.end.value) }
		}
		
		override def lazily: Graph[N, E] = if (isLazy) this else new GraphFromNodes(nodes, isLazy = true)
		
		override def node[N2 >: N](nodeValue: N2): GraphNode[N2, E] =
			nodes.find { _.value == nodeValue }.getOrElse { GraphNode(nodeValue) }
		
		override def filterNodeValues(f: N => Boolean): Graph[N, E] = filterNodes { node => f(node.value) }
		override def filterNodes(f: GraphNode[N, E] => Boolean): Graph[N, E] =
			this.factory(nodes.iterator.filter(f).map { _.filter { (_, edge) => f(edge.end) } })
		
		override def filterEdgeValues(f: E => Boolean): Graph[N, E] = filterEdges { (_, edge) => f(edge.value) }
		override def filterEdges(f: (GraphNode[N, E], GraphEdge[N, E]) => Boolean): Graph[N, E] =
			this.factory(nodes.map { node => node.filter { (_, edge) => f(node, edge) } })
		
		override def map[N2, E2](mapNode: N => N2)(mapEdge: E => E2): Graph[N2, E2] =
			this.factory(nodes.map { _.map(mapNode)(mapEdge) })
	}
}

/**
 * Common trait for pre-built graphs.
 * NB: Not all nodes specified within a graph are necessarily connected.
 * @tparam N Type of node values in this graph
 * @tparam E Type of edge values in this graph
 * @author Mikko Hilpinen
 * @since 25.4.2020, v1.8
 */
trait Graph[+N, +E] extends MaybeEmpty[Graph[N, E]]
{
	// ABSTRACT	------------------------
	
	/**
	 * @return An iterator that yields the connections within this graph.
	 *         Each connection contains 3 values:
	 *         1. Value of the connecting node (origin)
	 *         1. Value of the connecting edge
	 *         1. Value of the connected node (target)
	 * @see [[isTwoWayBound]]
	 */
	def connectionsIterator: Iterator[(N, E, N)]
	
	/**
	 * @return Whether connections in this graph are applied twice:
	 *         Once in the direction they are specified and once in reverse.
	 */
	def isTwoWayBound: Boolean
	/**
	 * @return Whether this graph is lazily resolved.
	 *         False if most parts of this graph are resolved immediately.
	 */
	def isLazy: Boolean
	
	/**
	 * All included node values
	 */
	def nodeValues: Seq[N]
	/**
	 * @return All nodes within this graph
	 */
	def nodes: Seq[GraphNode[N, E]]
	/**
	 * @return The first node in this graph. None if this graph is empty.
	 *         Functionally equivalent to 'nodes.headOption', but may be faster to compute.
	 */
	def headOption: Option[GraphNode[N, E]]
	
	/**
	 * @return A copy of this graph where functions are usually resolved lazily
	 */
	def lazily: Graph[N, E]
	
	/**
	 * @return All graphs within this graph that are not connected with each other.
	 *         If all the nodes in this graph are connected, returns only a single graph.
	 */
	def subgraphs: Seq[Graph[N, E]]
	
	/**
	 * @param nodeValue A node value
	 * @return A node in this graph with the specified value (may be a generated disconnected node).
	 */
	def node[N2 >: N](nodeValue: N2): GraphNode[N2, E]
	
	/**
	 * @return A map where keys are values of edge target nodes,
	 *         and where values consist of two parts:
	 *         1. The node matching the specified value
	 *         1. All edges that point to that node, including two values each:
	 *              1. The origin node
	 *              1. The connecting edge
	 */
	def edgesByTarget: Map[Any, (GraphNode[N, E], Seq[(GraphNode[N, E], GraphEdge[N, E])])]
	
	/**
	 * @param f A filtering function applied based on node values
	 * @return A copy of this graph containing only nodes accepted by the specified filtering function
	 */
	def filterNodeValues(f: N => Boolean): Graph[N, E]
	/**
	 * @param f A filtering function applied to nodes in this graph
	 * @return A copy of this graph containing only nodes accepted by the specified filtering function
	 */
	def filterNodes(f: GraphNode[N, E] => Boolean): Graph[N, E]
	/**
	 * @param f A filtering function applied based on edge values
	 * @return A copy of this graph containing only edges accepted by the specified filtering function
	 */
	def filterEdgeValues(f: E => Boolean): Graph[N, E]
	/**
	 * @param f A filtering function applied to edges.
	 *          Receives two values:
	 *              1. The origin node
	 *              1. The connecting edge
	 * @return A copy of this graph containing only edges accepted by the specified filtering function
	 */
	def filterEdges(f: (GraphNode[N, E], GraphEdge[N, E]) => Boolean): Graph[N, E]
	
	/**
	 * Maps all values within this graph (i.e. both node and edge values)
	 * @param mapNode Mapping function for node values
	 * @param mapEdge Mapping function for edge values
	 * @tparam N2 New node value-type
	 * @tparam E2 New edge value-type
	 * @return A mapped copy of this graph
	 */
	def map[N2, E2](mapNode: N => N2)(mapEdge: E => E2): Graph[N2, E2]
	
	
	// COMPUTED	----------------------------
	
	/**
	 * @return An iterator that yields all edges within this graph
	 */
	def edgesIterator = nodes.iterator.flatMap { _.leavingEdges }
	/**
	 * @return An iterator that yields all edges in this graph, grouped by the targeted end node.
	 *         Each entry contains two values:
	 *              1. The targeted end node
	 *              1. Edges that point to that node, including:
	 *                  1. The origin node
	 *                  1. The connecting edge
	 */
	def edgesByTargetIterator: Iterator[(GraphNode[N, E], Seq[(GraphNode[N, E], GraphEdge[N, E])])] =
		edgesByTarget.valuesIterator
	
	/**
	 * @return A copy of this graph where each edge points to the opposite direction
	 */
	def reversed: Graph[N, E] = {
		// Case: This graph is two-way bound => No change is needed
		if (isTwoWayBound)
			this
		else
			_withConnections(connectionsIterator.map { case (from, edge, to) => (to, edge, from) })
	}
	
	/**
	 * @return A calculated [[edgesByTarget]] value.
	 *         The subclasses are expected to store this value as a lazy property.
	 */
	protected def _edgesByTarget =
		nodes.iterator
			.flatMap { node => node.leavingEdges.iterator.map { e => (node, e, e.end) } }
			.groupToSeqsBy { _._3.value: Any }.view
			.mapValues { edges => edges.head._3 -> edges.map { case (origin, edge, _) => origin -> edge } }
			.toMap
	
	/**
	 * @return A factory interface for constructing more copies of this graph
	 */
	protected def factory = GraphFactory(isLazy)
	
	
	// IMPLEMENTED  ------------------------
	
	override def self = this
	
	
	// OTHER	----------------------------
	
	/**
	 * @param nodeValue Value of the targeted node
	 * @return A node in this graph with the specified value (may be a generated disconnected node)
	 */
	def apply[N2 >: N](nodeValue: N2) = node(nodeValue)
	
	/**
	 * @param nodeValue A node value
	 * @return Whether this graph contains an edge involving a node with the specified value
	 */
	def contains[N2 >: N](nodeValue: N2) = nodeValues.iterator.contains(nodeValue)
	
	/**
	 * @param nodeValue Value of the targeted node
	 * @return Edges pointing to that node (2), including the origin nodes (1)
	 */
	def edgesTo[N2 >: N](nodeValue: N2): Seq[(GraphNode[N, E], GraphEdge[N, E])] =
		nodeValues.find { _ == nodeValue }.flatMap { edgesByTarget.get(_) } match {
			case Some((_, edges)) => edges
			case None => Empty
		}
	
	/**
	 * Filters the connections / edges in this graph, based on their values
	 * @param f A filtering function to apply.
	 *          Receives 3 parameters:
	 *          1. Value of the origin node
	 *          1. Value of the connecting edge
	 *          1. Value of the target node
	 * @return A filtered copy of this graph
	 */
	def filterConnections(f: (N, E, N) => Boolean): Graph[N, E] =
		_withConnections(connectionsIterator.filter { case (from, edge, to) => f(from, edge, to) })
	
	/**
	 * Maps all node values in this graph
	 * @param f Mapping function for node values
	 * @tparam N2 New type of node values
	 * @return A mapped copy of this graph
	 */
	def mapNodes[N2](f: N => N2) = map(f)(Identity)
	/**
	 * Maps all edge values in this graph
	 * @param f A mapping function for edge values
	 * @tparam E2 Type of the new edge values
	 * @return A mapped copy of this graph
	 */
	def mapEdges[E2](f: E => E2) = map(Identity)(f)
	
	/**
	 * @param startNodeValue Value of the node from which the resulting graph will originate
	 * @return A graph that contains only nodes reachable from the specified node.
	 */
	def subgraphFrom[N2 >: N](startNodeValue: N2) =
		factory(LazyInitIterator { node(startNodeValue).allNodesIterator })
	@deprecated("Renamed to subgraphFrom", "v2.9")
	def subGraphFrom[N2 >: N](startNodeValue: N2) = subgraphFrom(startNodeValue)
	
	/**
	 * @param connection A new connection consisting of three parts:
	 *                   1. Value of the origin node
	 *                   1. Value of the connecting edge
	 *                   1. Value of the target node
	 * @return A copy of this graph with specified connection added/included
	 */
	def +[N2 >: N, E2 >: E](connection: (N2, E2, N2)) =
		_withConnections(connectionsIterator.appendIfDistinct(connection))
	@deprecated("Please use + instead", "v2.9")
	def withEdge[N2 >: N, E2 >: E](start: N2, edge: E2, end: N2) = this.+[N2, E2]((start, edge, end))
	
	/**
	 * @param other Another graph
	 * @return A graph that includes nodes and connections from both graphs
	 */
	def ++[N2 >: N, E2 >: E](other: Graph[N2, E2]): Graph[N2, E2] = this ++ other.connectionsIterator
	/**
	 * @param newConnections New connections to add. Each entry contains:
	 *                       1. Origin node value
	 *                       1. Value of the connecting edge
	 *                       1. Target node value
	 * @return A copy of this graph with specified connections added
	 */
	def ++[N2 >: N, E2 >: E](newConnections: IterableOnce[(N2, E2, N2)]) =
		_withConnections(connectionsIterator.appendAllIfDistinct(newConnections))
	
	/**
	 * @param nodeValue Node value to exclude from this graph
	 * @return A copy of this graph containing no node with the specified value
	 */
	def -[N2 >: N](nodeValue: N2) = withoutNode(nodeValue)
	/**
	 * @param nodeValue Node value to exclude from this graph
	 * @return A copy of this graph containing no node with the specified value
	 */
	def withoutNode[N2 >: N](nodeValue: N2) = filterNodeValues { _ == nodeValue }
	/**
	 * @param nodeValues Node values to exclude from this graph
	 * @return A copy of this graph not involving the specified node values
	 */
	def --[N2 >: N](nodeValues: Iterable[N2]) = withoutNodes(nodeValues)
	/**
	 * @param nodeValues Node values to exclude from this graph
	 * @return A copy of this graph not involving the specified node values
	 */
	def withoutNodes[N2 >: N](nodeValues: Iterable[N2]) =
		filterNodeValues { value => !nodeValues.exists { value == _ } }
	
	/**
	 * @param edgeValue An edge value to exclude from this graph
	 * @return A copy of this graph not including a single edge with the specified value
	 */
	def withoutEdge[E2 >: E](edgeValue: E2) = filterEdgeValues { _ == edgeValue }
	
	@deprecated("Renamed to filterConnections", "v2.9")
	def filterByContent(f: (N, E, N) => Boolean) = filterConnections(f)
	@deprecated("Renamed to filterNodeValues", "v2.9")
	def filterByNodeContent(f: N => Boolean) = filterNodeValues(f)
	@deprecated("Renamed to filterEdgeValues", "v2.9")
	def filterByEdgeContent(f: E => Boolean) = filterEdgeValues(f)
	@deprecated("Renamed to filterNodes", "v2.9")
	def filterByNode(f: GraphNode[N, E] => Boolean) = filterNodes(f)
	@deprecated("Replaced with filterEdges", "v2.9")
	def filterByEdge(f: GraphEdge[N, E] => Boolean) = filterEdges { (_, edge) => f(edge) }
	
	protected def _withConnections[N2, E2](connections: IterableOnce[(N2, E2, N2)]) =
		factory.withConnections(connections, isTwoWayBound)
	
	protected def subGraphsIteratorFrom[N2 >: N](distinctValuesIterator: Iterator[N2]): Iterator[Graph[N2, E]] =
		new DistinctGraphsIterator(distinctValuesIterator)
	
	
	// NESTED	----------------------------
	
	/**
	 * An iterator used for finding distinct graphs within a graph
	 * @param distinctValuesIterator An iterator that yields all distinct node values in this graph
	 * @tparam N2 Type of the accepted node values
	 */
	private class DistinctGraphsIterator[N2 >: N](distinctValuesIterator: Iterator[N2]) extends Iterator[Graph[N2, E]]
	{
		// ATTRIBUTES	--------------------
		
		/**
		 * A mutable set containing all node values that have already been included in a graph
		 */
		private val accessedNodes = mutable.Set[N2]()
		/**
		 * An iterator that yields all values in the last returned graph
		 */
		private var lastNodeValuesIter = Iterator.empty[N2]
		
		/**
		 * A lazy container called at every [[hasNext]] or [[next]].
		 * Initializes the next result.
		 */
		private val prepared = ResettableLazy {
			// Finds the next value that hasn't been involved in any graph yet
			accessedNodes ++= lastNodeValuesIter
			distinctValuesIterator.find { !accessedNodes.contains(_) }
		}
		
		
		// IMPLEMENTED  --------------------
		
		override def hasNext: Boolean = prepared.value.isDefined
		
		override def next() = {
			// Gets the next starting node
			val nextValue = prepared.pop().get
			val nextRoot = node(nextValue)
			
			// Resolves (possibly lazily) all nodes accessible via that node
			val nextNodesIter = nextRoot.allNodesIterator
			val nextNodes = if (isLazy) nextNodesIter.caching else nextNodesIter.toOptimizedSeq
			
			// Prepares the next iteration and returns the discovered nodes as a graph
			lastNodeValuesIter = nextNodes.iterator.map { _.value }
			factory(nextNodes)
		}
	}
}