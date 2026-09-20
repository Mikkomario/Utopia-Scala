package utopia.flow.collection.template.graph

import utopia.flow.collection.template.LookupPath
import utopia.flow.operator.equality.EqualsFunction

import scala.annotation.unchecked.uncheckedVariance

object LookupGraphPathViaEdges
{
	// OTHER    ------------------------------
	
	/**
	 * @param origin Root node to start the searches from
	 * @param edgeValueEquals An equality function used for matching nav input with graph-edge values
	 * @tparam Nav Type of nav input accepted
	 * @tparam Node Type of searched nodes
	 * @return A new path-lookup interface based on graph-edge values
	 */
	def apply[Nav, Node <: GraphNodeLike[_, _, _, GraphEdge[_ <: Nav, Node]]](origin: Node)
		(implicit edgeValueEquals: EqualsFunction[Nav] = EqualsFunction.default): LookupGraphPathViaEdges[Nav, Node] =
		new _LookupGraphPathViaEdges[Nav, Node](origin)(edgeValueEquals)
	
	
	// NESTED   ------------------------------
	
	private class _LookupGraphPathViaEdges[Nav, +Node <: GraphNodeLike[_, _, _, GraphEdge[_ <: Nav, Node]]]
	(override val current: Node)(implicit protected val edgeValueEquals: EqualsFunction[Nav])
		extends LookupGraphPathViaEdges[Nav, Node]
}

/**
 * Common trait for path-lookup interfaces used with graphs, based on graph-edge values
 * @tparam Nav Type of navigational input accepted
 * @tparam Node Type of the searched nodes
 * @author Mikko Hilpinen
 * @since 20.09.2026, v2.9
 */
trait LookupGraphPathViaEdges[Nav, +Node <: GraphNodeLike[_, _, _, GraphEdge[_ <: Nav, Node]]]
	extends LookupPath[Nav, Node]
{
	// ABSTRACT -----------------------------
	
	/**
	 * @return An equals function used for testing graph edge values against navigational input
	 */
	protected def edgeValueEquals: EqualsFunction[Nav]
	
	
	// IMPLEMENTED  -------------------------
	
	override protected def findUnder(parent: Node @uncheckedVariance, nav: Nav): Option[Node] =
		parent.leavingEdges.find { edge => edgeValueEquals(edge.value, nav) }.map { _.end }
}
