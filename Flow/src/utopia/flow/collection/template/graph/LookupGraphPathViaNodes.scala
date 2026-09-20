package utopia.flow.collection.template.graph

import utopia.flow.collection.template.LookupPath
import utopia.flow.operator.equality.EqualsFunction

import scala.annotation.unchecked.uncheckedVariance

object LookupGraphPathViaNodes
{
	// OTHER    ---------------------------
	
	/**
	 * @param origin The root node to start the searches from
	 * @param valueEquals Implicit value equality function used for comparing nav input and node values
	 * @tparam Nav Type of accepted nav input
	 * @tparam Node Type of searched nodes
	 * @return A new path-lookup interface for the specified node
	 */
	def apply[Nav, Node <: GraphNodeLike[_ <: Nav, _, Node, _]](origin: Node)
	                                                           (implicit valueEquals: EqualsFunction[Nav] = EqualsFunction.default): LookupGraphPathViaNodes[Nav, Node] =
		new _LookupGraphPathViaNodes[Nav, Node](origin)
	
	
	// NESTED   ---------------------------
	
	private class _LookupGraphPathViaNodes[Nav, +Node <: GraphNodeLike[_ <: Nav, _, Node, _]](override val current: Node)
	                                                                                          (implicit override val valueEquals: EqualsFunction[Nav])
		extends LookupGraphPathViaNodes[Nav, Node]
}

/**
 * Common trait for path lookup implementations for graphs, which are based on graph node values.
 * @tparam Nav Type of navigational input accepted
 * @tparam Node Type of nodes yielded
 * @author Mikko Hilpinen
 * @since 20.09.2026, v2.9
 */
trait LookupGraphPathViaNodes[Nav, +Node <: GraphNodeLike[_ <: Nav, _, Node, _]] extends LookupPath[Nav, Node]
{
	// ABSTRACT --------------------------
	
	/**
	 * @return An equals function used for comparing node values
	 */
	protected def valueEquals: EqualsFunction[Nav]
	
	
	// IMPLEMENTED  ---------------------
	
	override protected def findUnder(parent: Node @uncheckedVariance, nav: Nav): Option[Node] =
		parent.endNodesIterator.find { node => valueEquals(nav, node.value) }
}
