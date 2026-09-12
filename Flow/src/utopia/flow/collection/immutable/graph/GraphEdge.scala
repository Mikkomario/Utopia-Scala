package utopia.flow.collection.immutable.graph

import utopia.flow.collection.template
import utopia.flow.collection.template.graph.GraphEdgeLike
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy

object GraphEdge
{
	def lazily[N, E](value: E, end: => GraphNode[N, E]) = apply[N, E](value, Lazy(end))
	
	def apply[N, E](value: E, endView: View[GraphNode[N, E]]) = new GraphEdge(value, endView)
}

/**
 * A graph edge, possibly lazily connected to a graph node
 * @author Mikko Hilpinen
 * @since 11.09.2026, v2.9
 */
class GraphEdge[+N, +E](override val value: E, endView: View[GraphNode[N, E]])
	extends template.graph.GraphEdge[N, E] with GraphEdgeLike[E, GraphNode[N, E]]
{
	// IMPLEMENTED	---------------------
	
	override def end = endView.value
	
	
	// OTHER    -------------------------
	
	def withValue[E2 >: E](newValue: E2) = GraphEdge(newValue, endView)
	def mapLocalValue[E2 >: E](f: E => E2) = withValue(f(value))
	
	def withEnd[N2 >: N, E2 >: E](newEnd: GraphNode[N2, E2]) = withEndView(View.fixed(newEnd))
	def withEndView[N2 >: N, E2 >: E](newEndView: View[GraphNode[N2, E2]]) = GraphEdge(value, newEndView)
	def mapEnd[N2 >: N, E2 >: E](f: GraphNode[N, E] => GraphNode[N2, E2]) =
		withEndView(endView.mapValue(f))
}
