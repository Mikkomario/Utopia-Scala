package utopia.flow.collection.template.graph

import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy

object GraphEdge
{
	// OTHER    -------------------------
	
	/**
	 * @param value Value to wrap by this edge
	 * @param end Node this edge points to
	 * @tparam N Type of graph node values
	 * @tparam E Type of graph edge values
	 * @return A new graph edge
	 */
	def apply[N, E](value: E, end: GraphNode[N, E]): GraphEdge[N, E] = new _GraphEdge(value, end)
	/**
	 * @param value Value to wrap by this edge
	 * @param end Node this edge points to. Call-by-name, cached.
	 * @tparam N Type of graph node values
	 * @tparam E Type of graph edge values
	 * @return A new graph edge
	 */
	def lazily[N, E](value: E, end: => GraphNode[N, E]): GraphEdge[N, E] = new LazyGraphEdge(value, Lazy(end))
	
	
	// NESTED   -------------------------
	
	private class LazyGraphEdge[+N, +E](override val value: E, lazyEnd: View[GraphNode[N, E]]) extends GraphEdge[N, E]
	{
		override def end: GraphNode[N, E] = lazyEnd.value
	}
	
	private class _GraphEdge[+N, +E](override val value: E, override val end: GraphNode[N, E]) extends GraphEdge[N, E]
}

/**
 * Common trait for graph edge implementations. Removes the generic type parameter from [[GraphEdgeLike]].
 * @author Mikko Hilpinen
 * @since 11.09.2026, v2.9
 */
trait GraphEdge[+N, +E] extends GraphEdgeLike[E, GraphNode[N, E]]
