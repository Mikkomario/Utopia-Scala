package utopia.flow.collection.mutable.graph

import utopia.flow.collection.template.graph.GraphEdge
import utopia.flow.view.mutable.Pointer

object MutableGraphEdge
{
	/**
	 * Converts a generic edge into a mutable edge
	 * @param edge Edge to convert into a mutable edge
	 * @tparam A Type of the edge value
	 * @tparam Node Type of the edge end node
	 * @return A mutable graph edge based on the specified edge.
	 *         Note: If the specified edge was already a mutable graph edge, yields it directly.
	 */
	def from[A, Node](edge: GraphEdge[A, Node]) = edge match {
		case e: MutableGraphEdge[A, Node] => e
		case e => apply(e.value, e.end)
	}
	
	/**
	 * Creates a new graph edge
	 * @param value Value to assign to this edge
	 * @param end The node this edge points to
	 * @tparam A Type of the value in this edge
	 * @tparam Node Type of the node this edge points to
	 * @return A new mutable graph edge
	 */
	def apply[A, Node](value: A, end: Node) = new MutableGraphEdge[A, Node](value, end)
}

/**
 * A mutable implementation of the graph edge -trait
 * @author Mikko Hilpinen
 * @since 13.09.2026, v2.9
 */
class MutableGraphEdge[A, Node](override var value: A, override val end: Node)
	extends GraphEdge[A, Node] with Pointer[A]
