package utopia.flow.collection.immutable.graph

import utopia.flow.collection.template
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy

object GraphEdge
{
	/**
	 * @param edge Edge to wrap
	 * @tparam N Type of node values
	 * @tparam E Type of edge values
	 * @return An immutable graph edge, based on the specified edge
	 */
	def from[N, E](edge: template.graph.GraphEdge[E, template.graph.GraphNode[N, E]]): GraphEdge[N, E] = edge match {
		case e: GraphEdge[N, E] => e
		case e => lazily(e.value, GraphNode.from(e.end))
	}
	
	/**
	 * @param value A value to wrap by this edge
	 * @param end Graph node to point to (call-by-name)
	 * @tparam N Type of graph-node values
	 * @tparam E Type of graph-edge values
	 * @return A new lazily resolved graph edge
	 */
	def lazily[N, E](value: E, end: => GraphNode[N, E]) = apply[N, E](value, Lazy(end))
	/**
	 * @param value A value to wrap by this edge
	 * @param endView A view to the node targeted by this edge
	 * @tparam N Type of graph-node values
	 * @tparam E Type of graph-edge values
	 * @return A new graph edge
	 */
	def apply[N, E](value: E, endView: View[GraphNode[N, E]]) = new GraphEdge(value, endView)
}

/**
 * A graph edge, possibly lazily connected to a graph node
 * @author Mikko Hilpinen
 * @since 11.09.2026, v2.9
 */
class GraphEdge[+N, +E](override val value: E, endView: View[GraphNode[N, E]])
	extends template.graph.GraphEdge[E, GraphNode[N, E]]
{
	// IMPLEMENTED	---------------------
	
	override def end = endView.value
	
	
	// OTHER    -------------------------
	
	/**
	 * @param newValue New edge value
	 * @tparam E2 Type of the new edge value
	 * @return A copy of this edge with the specified value
	 */
	def withValue[E2 >: E](newValue: E2) = GraphEdge(newValue, endView)
	/**
	 * @param f A mapping function to apply to this edge's value
	 * @tparam E2 Type of the new edge value
	 * @return A copy of this edge with a mapped value
	 */
	def mapLocalValue[E2 >: E](f: E => E2) = withValue(f(value))
	/**
	 * @param f A mapping function applied to all graph-edge values
	 * @tparam E2 Type of mapped edge values
	 * @return A copy of this edges with a mapped value, pointing to a graph with mapped values
	 */
	def mapValues[E2](f: E => E2) = GraphEdge[N, E2](f(value), endView.mapValue { _.mapEdges(f) })
	
	/**
	 * @param newEnd New end node
	 * @tparam N2 Type of values in the end node
	 * @tparam E2 Type of values in the edges in the new end node
	 * @return A copy of this edge pointing to the targeted node
	 */
	def withEnd[N2 >: N, E2 >: E](newEnd: GraphNode[N2, E2]) = withEndView(View.fixed(newEnd))
	/**
	 * @param newEndView A view to the new end node
	 * @tparam N2 Type of values in the end node
	 * @tparam E2 Type of values in the edges in the new end node
	 * @return A copy of this edge pointing to the targeted node
	 */
	def withEndView[N2 >: N, E2 >: E](newEndView: View[GraphNode[N2, E2]]) = GraphEdge(value, newEndView)
	/**
	 * @param f A mapping function applied to this edge's end node
	 * @tparam N2 Type of the new node values
	 * @tparam E2 Type of the new edge values
	 * @return A copy of this edge with a mapped target node
	 */
	def mapEnd[N2 >: N, E2 >: E](f: GraphNode[N, E] => GraphNode[N2, E2]) =
		withEndView(endView.mapValue(f))
}
