package utopia.flow.collection.immutable.graph

import utopia.flow.collection.immutable.Empty
import utopia.flow.collection.immutable.caching.iterable.CachingSeq
import utopia.flow.collection.immutable.graph.GraphNode.GraphNodeValuesNavigator
import utopia.flow.collection.mutable.iterator.LazyInitIterator
import utopia.flow.collection.template
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.mutable
import scala.language.implicitConversions

object GraphNode extends GraphFactory[GraphNode, GraphEdge]
{
	// IMPLEMENTED  -------------------------
	
	override def node[N, E](value: N, edges: IterableOnce[GraphEdge[N, E]]): GraphNode[N, E] = apply(value, edges)
	override def edge[N, E](value: E, end: View[GraphNode[N, E]]): GraphEdge[N, E] = GraphEdge(value, end)
	
	
	// IMPLICIT -----------------------------
	
	// Implicitly provides a navigation interface
	implicit def autoNavigate[Nav, E](node: GraphNode[Nav, E])
	                                 (implicit valueEquals: EqualsFunction[Nav] = EqualsFunction.default): GraphNodeValuesNavigator[Nav, E] =
		node.navigateUsing[Nav](valueEquals)
	
	
	// OTHER    -----------------------------
	
	/**
	 * Creates a new graph node where the edges are generated / evaluated lazily
	 * @param value Value to wrap by this node
	 * @param edges A function that generates the edges from this node
	 * @tparam N Type of node values
	 * @tparam E Type of edge values
	 * @return A new graph node
	 */
	def lazily[N, E](value: N)(edges: => IterableOnce[GraphEdge[N, E]]) =
		apply(value, CachingSeq(LazyInitIterator(edges)))
	
	/**
	 * @param node A graph node to wrap
	 * @tparam N Type of node values
	 * @tparam E Type of edge values
	 * @return An immutable graph node, based on that node
	 */
	def from[N, E](node: template.graph.GraphNode[N, E]): GraphNode[N, E] = _from[N, E](node, mutable.Map())
	// Uses previously cached results, where possible
	private def _from[N, E](node: template.graph.GraphNode[N, E],
	                        conversions: mutable.Map[template.graph.GraphNode[N, E], GraphNode[N, E]]): GraphNode[N, E] =
		conversions.getOrElse(node, {
			node match {
				// Case: Already of the desired type => No conversion is needed
				case n: GraphNode[N, E] => n
				// Case: Another type of node => Creates an immutable copy of it
				case n =>
					// The edge end nodes are resolved lazily, in order for the conversions to be ready by that time
					val edges = n.leavingEdges.map { edge =>
						GraphEdge(edge.value, Lazy { _from(edge.end, conversions) })
					}
					val node = apply(n.value, edges)
					// Caches the conversion result
					conversions += (n -> node)
					node
			}
		})
	
	/**
	 * @param value Value to wrap by this node
	 * @param edges Edges that leave from this node
	 * @tparam N Type of the node values
	 * @tparam E Type of the edge values
	 * @return A new graph node
	 */
	def apply[N, E](value: N, edges: IterableOnce[GraphEdge[N, E]] = Empty): GraphNode[N, E] =
		new GraphNode(value, edges match {
			case v: scala.collection.View[GraphEdge[N, E]] => CachingSeq.from(v)
			case i: Iterable[GraphEdge[N, E]] => i
			case i => CachingSeq.from(i)
		})
	
	/**
	 * Produces a function that lazily generates a full graph / graphs.
	 * Assumes that node values are unique.
	 * @param edges A function for generating node edges.
	 *              Receives a node value and yields a collection where each value consists of two parts:
	 *              1. Value of the connecting edge
	 *              1. View to the value of the end node (usually Lazy)
	 * @tparam N Type of the node values in this graph
	 * @tparam E Type of the edge values in this graph
	 * @return A function that generates new nodes based on their values
	 */
	def generator[N, E](edges: N => IterableOnce[(E, View[N])]): N => GraphNode[N, E] = {
		val cache = mutable.Map[N, GraphNode[N, E]]()
		def fromValue(value: N) = _iterate(cache, value)(edges)
		fromValue
	}
	/**
	 * Lazily generates a full graph, assuming that node values are unique.
	 * @param start Value of the returned node
	 * @param edges A function for generating node edges.
	 *              Receives a node value and yields a collection where each value consists of two parts:
	 *              1. Value of the connecting edge
	 *              1. View to the value of the end node (usually Lazy)
	 * @tparam N Type of the node values in this graph
	 * @tparam E Type of the edge values in this graph
	 * @return A new graph node with value 'start'
	 */
	def iterate[N, E](start: N)(edges: N => IterableOnce[(E, View[N])]) =
		_iterate[N, E](mutable.Map(), start)(edges)
	private def _iterate[N, E](cache: mutable.Map[N, GraphNode[N, E]], value: N)
	                         (edges: N => IterableOnce[(E, View[N])]): GraphNode[N, E] =
		cache.getOrElseUpdate(value,
			apply(value, CachingSeq(LazyInitIterator {
				edges(value).iterator.map { case (edgeValue, endView) =>
					GraphEdge(edgeValue, endView.mapValue { endValue => _iterate(cache, endValue)(edges) })
				}
			})))
			
	
	// NESTED   ---------------------------
	
	class GraphNodeValuesNavigator[N, +E](override protected val current: GraphNode[N, E])
	                                     (implicit override protected val valueEquals: EqualsFunction[N])
		extends ImmutableGraphViaNodesNavigator[N, E, GraphNode]
}

/**
 * An immutable and copyable implementation of a graph node.
 * Each node and each edge wraps a value.
 * @tparam N Type of values wrapped by graph nodes
 * @tparam E Type of values wrapped by graph edges
 * @author Mikko Hilpinen
 * @since 11.09.2026, v2.9
 */
class GraphNode[+N, +E](override val value: N, override val leavingEdges: Iterable[GraphEdge[N, E]])
	extends template.graph.GraphNode[N, E]
		with CopyableGraphNodeLike[N, E, GraphNode, GraphEdge, GraphNode[N, E], GraphEdge[N, E]]
{
	// COMPUTED -----------------------------
	
	/**
	 * @param valueEquals An implicit equals-function used for matching node values with nav input.
	 *                    Default = use ==.
	 * @tparam Nav Type of the accepted nav input
	 * @return A navigator interface into this graph, which utilizes the specified equals-function
	 */
	def navigate[Nav >: N](implicit valueEquals: EqualsFunction[Nav] = EqualsFunction.default) =
		navigateUsing[Nav](valueEquals)
	
	
	// IMPLEMENTED  -------------------------
	
	override def self: GraphNode[N, E] = this
	override def factory: GraphFactory[GraphNode, GraphEdge] = GraphNode
	
	override def toGraph: Graph[N, E] = Graph(allNodesIterator)
	
	override def filterDirect(f: GraphEdge[N, E] => Boolean): GraphNode[N, E] = {
		if (leavingEdges.knownSize == 0)
			this
		else
			new GraphNode(value, leavingEdges.filter(f))
	}
	override def filter(f: (N, GraphEdge[N, E]) => Boolean): GraphNode[N, E] = _filter(mutable.Map())(f)
	
	
	// OTHER    ----------------------------
	
	/**
	 * @param valueEquals Equals-function used for matching node values with nav input
	 * @tparam Nav Type of the accepted nav input
	 * @return A navigator interface into this graph, which utilizes the specified equals-function
	 */
	def navigateUsing[Nav >: N](valueEquals: EqualsFunction[Nav]) =
		new GraphNodeValuesNavigator[Nav, E](self)(valueEquals)
	
	private def _filter(mappedNodes: mutable.Map[Any, GraphNode[N, E] @uncheckedVariance])
	                   (f: (N, GraphEdge[N, E]) => Boolean): GraphNode[N, E] =
		mappedNodes.getOrElseUpdate(self, {
			if (leavingEdges.knownSize == 0)
				this
			else
				GraphNode(value, leavingEdges.view.filter { f(value, _) }.map { _.mapEnd { _._filter(mappedNodes)(f) } })
		})
}
