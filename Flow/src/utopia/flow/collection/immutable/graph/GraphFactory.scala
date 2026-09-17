package utopia.flow.collection.immutable.graph

import utopia.flow.collection.immutable.Empty
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy

/**
 * Common trait for interfaces used for building graphs
 * @tparam Node Generic node type built using this factory
 * @tparam Edge Generic edge type built using this factory
 * @author Mikko Hilpinen
 * @since 11.09.2026, v2.9
 */
trait GraphFactory[Node[_, _], Edge[_, _]]
{
	// ABSTRACT -------------------------
	
	/**
	 * Constructs a new graph node
	 * @param value Value to wrap by this node
	 * @param edges Edges that should leave from this node. Default = empty.
	 * @tparam N Type of node values
	 * @tparam E Type of edge values
	 * @return A new graph node
	 */
	def node[N, E](value: N, edges: IterableOnce[Edge[N, E]] = Empty): Node[N, E]
	/**
	 * Constructs a new graph edge
	 * @param value Value to wrap by this edge
	 * @param end A view to the node this edge points to
	 * @tparam N Type of node values
	 * @tparam E Type of edge values
	 * @return A new graph edge
	 */
	def edge[N, E](value: E, end: View[Node[N, E]]): Edge[N, E]
	
	
	// OTHER    -------------------------
	
	/**
	 * Creates a new graph edge. The node reference is resolved lazily.
	 * @param value Value to wrap by this edge
	 * @param end Node this edge points to. Call-by-name.
	 * @tparam N Type of node values
	 * @tparam E Type of edge values
	 * @return A new graph edge
	 */
	def lazyEdge[N, E](value: E, end: => Node[N, E]): Edge[N, E] = edge(value, Lazy(end))
}
