package utopia.flow.collection.immutable.graph

import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy

/**
 * Common trait for interfaces used for building graphs
 * @author Mikko Hilpinen
 * @since 11.09.2026, v2.9
 */
trait GraphFactory[Node[_, _], Edge[_, _]]
{
	// ABSTRACT -------------------------
	
	def node[N, E](value: N, edges: IterableOnce[Edge[N, E]]): Node[N, E]
	def edge[N, E](value: E, end: View[Node[N, E]]): Edge[N, E]
	
	
	// OTHER    -------------------------
	
	def lazyEdge[N, E](value: E, end: => Node[N, E]): Edge[N, E] = edge(value, Lazy(end))
}
