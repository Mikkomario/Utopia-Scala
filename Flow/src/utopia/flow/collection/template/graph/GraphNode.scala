package utopia.flow.collection.template.graph

import utopia.flow.collection.immutable.Empty

object GraphNode
{
	// OTHER    -------------------------
	
	/**
	 * @param value Value to wrap by this node
	 * @param leavingEdges Edges leaving from this node
	 * @tparam N Type of node values
	 * @tparam E Type of edge values
	 * @return A new graph node
	 */
	def apply[N, E](value: N, leavingEdges: Iterable[GraphEdge[N, E]] = Empty): GraphNode[N, E] =
		new _GraphNode(value, leavingEdges)
	
	
	// NESTED   -------------------------
	
	private class _GraphNode[+N, +E](override val value: N, override val leavingEdges: Iterable[GraphEdge[N, E]])
		extends GraphNode[N, E]
	{
		override def self: GraphNode[N, E] = this
	}
}

/**
 * Common trait for graph node implementations. Removes the generic Repr types from [[GraphNodeLike]].
 * @author Mikko Hilpinen
 * @since 11.09.2026, v2.9
 */
trait GraphNode[+N, +E] extends GraphNodeLike[N, E, GraphNode[N, E], GraphEdge[N, E]]