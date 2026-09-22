package utopia.flow.collection.immutable.graph

import utopia.flow.collection.immutable.Empty
import utopia.flow.collection.template.PathNavigator
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.operator.equality.EqualsFunction

import scala.annotation.unchecked.uncheckedVariance

object ImmutableGraphViaNodesNavigator
{
	// OTHER    ---------------------------
	
	/**
	 * @param current The starting node
	 * @param valueEquals Implicit value equality -function used for comparing nav input with node values
	 * @tparam Nav Type of the accepted nav input
	 * @tparam E Type of graph-edge values
	 * @tparam NC Type of graph node constructors used
	 * @return A navigator interface to the specified graph
	 */
	def apply[Nav, E, NC[N2, +E2] <: CopyableGraphNodeLike[N2, E2, NC, GraphEdge, NC[N2, E2], _]](current: NC[Nav, E])
	                                                                                             (implicit valueEquals: EqualsFunction[Nav] = EqualsFunction.default): ImmutableGraphViaNodesNavigator[Nav, E, NC] =
		new _ImmutableGraphViaNodesNavigator[Nav, E, NC](current)
	
	
	// NESTED   ---------------------------
	
	private class _ImmutableGraphViaNodesNavigator[Nav, +E, NC[N2, +E2] <: CopyableGraphNodeLike[N2, E2, NC, GraphEdge, NC[N2, E2], _]]
	(override protected val current: NC[Nav, E])(implicit override protected val valueEquals: EqualsFunction[Nav])
		extends ImmutableGraphViaNodesNavigator[Nav, E, NC]
}

/**
 * An interface for navigating graphs.
 * Generates new nodes as placeholders for situations where no nodes exist in the original graph.
 * @author Mikko Hilpinen
 * @since 21.09.2026, v2.9
 */
trait ImmutableGraphViaNodesNavigator[Nav, +E, NC[N2, +E2] <: CopyableGraphNodeLike[N2, E2, NC, GraphEdge, NC[N2, E2], _]]
	extends PathNavigator[Nav, NC[Nav, E]]
{
	// ABSTRACT -----------------------------
	
	/**
	 * @return Equals-function used for comparing nav input with node values
	 */
	protected def valueEquals: EqualsFunction[Nav]
	
	
	// IMPLEMENTED  -------------------------
	
	override protected def nodeFor(nav: Nav): NC[Nav, E] = current.factory.node(nav, Empty)
	override protected def nodeForPath(parents: Seq[NC[Nav, E @uncheckedVariance]], path: Iterator[Nav]): NC[Nav, E] =
		nodeFor(path.last)
	
	override protected def findUnder(parent: NC[Nav, E @uncheckedVariance], nav: Nav): Option[NC[Nav, E]] =
		parent.endNodesIterator.find { node => valueEquals(node.value, nav) }
}
