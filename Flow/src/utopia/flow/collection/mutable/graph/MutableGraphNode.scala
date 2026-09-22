package utopia.flow.collection.mutable.graph

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.collection.mutable.graph.MutableGraphNode.{MutableGraphNodeNavigator, MutableGraphViaEdgesNavigator}
import utopia.flow.collection.mutable.iterator.OrderedDepthIterator
import utopia.flow.collection.template
import utopia.flow.collection.template.PathNavigator
import utopia.flow.collection.template.graph.{GraphEdge, GraphNode, GraphNodeLike}
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.view.mutable.Pointer

import scala.collection.mutable

object MutableGraphNode
{
	// OTHER    ----------------------------
	
	/**
	 * Converts any type of graph node into a mutable graph node.
	 * @param node Node to convert
	 * @tparam N Type of node values
	 * @tparam E Type of edge values
	 * @return A mutable graph node from the specified node.
	 *
	 *         Note: If 'node' is already a mutable graph node, it is not converted but kept as is.
	 *               The same is true for all connected nodes.
	 */
	def from[N, E](node: GraphNode[N, E]) =
		_from(node, mutable.Map.empty[GraphNode[N, E], MutableGraphNode[N, E]])
	// Applies previously performed conversions, where possible
	private def _from[N, E](node: GraphNode[N, E],
	                        conversionsBuffer: mutable.Map[GraphNode[N, E], MutableGraphNode[N, E]]): MutableGraphNode[N, E] =
		conversionsBuffer.getOrElse(node, {
			// Case: A new conversion => Checks whether already of the desired type
			node match {
				// Case: Already a mutable node => No conversion is needed
				case m: MutableGraphNode[N, E] => m
				// Case: Another type of node => Creates a mutable copy of it
				case node =>
					val m = apply[N, E](node.value)
					conversionsBuffer += node -> m
					// Attaches all edges from the original node
					node.leavingEdges.foreach { edge => m.connect(edge.value, _from(edge.end, conversionsBuffer)) }
					m
			}
		})
	
	/**
	 * @param value Value to wrap by this node
	 * @tparam N Type of node values
	 * @tparam E Type of edge values
	 * @return A new mutable graph node
	 */
	def apply[N, E](value: N): MutableGraphNode[N, E] = new MutableGraphNode[N, E](value)
	/**
	 * @param value Value to wrap by this node
	 * @param edges Edges to assign to this node. Default = empty.
	 * @tparam N Type of node values
	 * @tparam E Type of edge values
	 * @return A new mutable graph node
	 */
	def apply[N, E](value: N, edges: IterableOnce[GraphEdge[E, MutableGraphNode[N, E]]]): MutableGraphNode[N, E] =
		new MutableGraphNode[N, E](value, edges.iterator.map(MutableGraphEdge.from).toOptimizedSeq)
		
	
	// NESTED   --------------------------------
	
	class MutableGraphNodeNavigator[N, E](override protected val current: MutableGraphNode[N, E], edgeValue: N => E)
	                                     (implicit valueEquals: EqualsFunction[N])
		extends PathNavigator[N, MutableGraphNode[N, E]]
	{
		// IMPLEMENTED  ---------------------
		
		override protected def findUnder(parent: MutableGraphNode[N, E], nav: N): Option[MutableGraphNode[N, E]] =
			parent.endNodesIterator.find { node => valueEquals(node.value, nav) }
		
		override protected def nodeFor(nav: N): MutableGraphNode[N, E] = {
			val node = MutableGraphNode[N, E](nav)
			current.connect(edgeValue(nav), node)
			node
		}
		override protected def nodeForPath(parents: Seq[MutableGraphNode[N, E]],
		                                   path: Iterator[N]): MutableGraphNode[N, E] =
		{
			var lastParent = parents.last
			while (path.hasNext) {
				val value = path.next()
				val node = MutableGraphNode[N, E](value)
				lastParent.connect(edgeValue(value), node)
				lastParent = node
			}
			lastParent
		}
	}
	class MutableGraphViaEdgesNavigator[N, E](override protected val current: MutableGraphNode[N, E], nodeValue: E => N)
	                                         (implicit valueEquals: EqualsFunction[E])
		extends PathNavigator[E, MutableGraphNode[N, E]]
	{
		override protected def findUnder(parent: MutableGraphNode[N, E], nav: E): Option[MutableGraphNode[N, E]] =
			parent.leavingEdges.find { edge => valueEquals.equals(edge.value, nav) }.map { _.end }
		
		override protected def nodeFor(nav: E): MutableGraphNode[N, E] = {
			val node = MutableGraphNode[N, E](nodeValue(nav))
			current.connect(nav, node)
			node
		}
		override protected def nodeForPath(parents: Seq[MutableGraphNode[N, E]], path: Iterator[E]): MutableGraphNode[N, E] = {
			var lastParent = parents.last
			while (path.hasNext) {
				val value = path.next()
				val node = MutableGraphNode[N, E](nodeValue(value))
				lastParent.connect(value, node)
				lastParent = node
			}
			lastParent
		}
	}
}

/**
 * A mutable graph node implementation
 * @tparam N Type of values stored within graph nodes
 * @tparam E Type of values stored within graph edges
 * @author Mikko Hilpinen
 * @since 28.10.2016
 */
class MutableGraphNode[N, E](var value: N, initialEdges: Seq[MutableGraphEdge[E, MutableGraphNode[N, E]]] = Empty)
	extends GraphNode[N, E]
		with GraphNodeLike[N, E, MutableGraphNode[N, E], MutableGraphEdge[E, MutableGraphNode[N, E]]] with Pointer[N]
{
	// TYPES    --------------------
	
	/**
	 * Type of nodes in this graph
	 */
	type Node = MutableGraphNode[N, E]
	/**
	 * Type of edges in this graph
	 */
	type Edge = MutableGraphEdge[E, Node]
	
	
	// ATTRIBUTES   ----------------
	
	private var _leavingEdges = initialEdges
	
	
	// IMPLEMENTED    --------------
	
	override def self = this
	
	override def leavingEdges = _leavingEdges
	def leavingEdges_=(newEdges: Seq[MutableGraphEdge[E, MutableGraphNode[N, E]]]) = _leavingEdges = newEdges
	
	
	// OTHER    --------------------
	
	/**
	 * @param edgeValue A function that generates edge values based on node values
	 * @param valueEquals Implicit equals-function used for comparing node values with nav input
	 * @return A navigation interface to this graph
	 */
	def navigateUsing(edgeValue: N => E)(implicit valueEquals: EqualsFunction[N] = EqualsFunction.default) =
		new MutableGraphNodeNavigator[N, E](this, edgeValue)(valueEquals)
	/**
	 * @param nodeValue A function that generates node values based on edge values
	 * @param valueEquals Implicit equals-function used for comparing edge values with nav input
	 * @return A navigation interface to this graph, uses edge values as nav input.
	 */
	def navigateEdgesUsing(nodeValue: E => N)(implicit valueEquals: EqualsFunction[E] = EqualsFunction.default) =
		new MutableGraphViaEdgesNavigator[N, E](this, nodeValue)(valueEquals)
	
	/**
	 * @return A copy of this mutable node
	 */
	def copy() = new MutableGraphNode(value, leavingEdges)
	
	/**
	 * Connects this node to another node, creating a new edge.
	 * This will always create a new edge, even when there exists a connection already.
	 * @param edgeValue The contents for the edge that is generated
	 * @param node The node this node will be connected to
	 * @return The newly created graph edge
	 */
	def connect(edgeValue: E, node: Node) = {
		val newEdge = new MutableGraphEdge(edgeValue, node)
		leavingEdges :+= newEdge
		newEdge
	}
	/**
	 * Either updates an existing connection or creates a new one
	 * @param node The node this node is or will be connected to
	 * @param edgeValue Value to assign for the edge connecting these nodes
	 * @return Edge (now) connecting these nodes
	 */
	def setConnection(node: Node, edgeValue: E) =
		leavingEdges.find { _.end == node } match {
			case Some(edge) =>
				edge.value = edgeValue
				edge
			case None => connect(edgeValue, node)
		}
	
	/**
	 * Adds a new (leaving) edge to this node
	 * @param edge Edge to add to this node
	 */
	def +=(edge: GraphEdge[E, MutableGraphNode[N, E]]) = _leavingEdges = _leavingEdges :+ MutableGraphEdge.from(edge)
	/**
	 * Adds n new (leaving) edges to this node
	 * @param edges Edges to attach to this node
	 */
	def ++=(edges: IterableOnce[GraphEdge[E, MutableGraphNode[N, E]]]) =
		_leavingEdges = _leavingEdges ++ edges.iterator.map(MutableGraphEdge.from)
	
	/**
	 * Removes any direct connection(s) to the provided node from this node.
	 * The specified node may still contain edges towards this node, afterwards.
	 * @param node Node to disconnect from this node
	 */
	def disconnectFromDirect(node: template.graph.GraphNode[_, _]) = disconnectDirectWhere { _.end == node }
	@deprecated("Renamed to disconnectFromDirect", "v2.9")
	def disconnectDirect(node: template.graph.GraphNode[_, _]) = disconnectFromDirect(node)
	
	/**
	 * Disconnects every node in this graph from the specified node,
	 * except for nodes that are accessible only through the specified node.
	 * @param node A node to disconnect from this graph
	 */
	def disconnectFrom(node: template.graph.GraphNode[_, _]) = disconnectWhere { _.end == node }
	@deprecated("Renamed to disconnectFrom", "v2.9")
	def disconnectTotally(node: template.graph.GraphNode[_, _]) = disconnectFrom(node)
	
	/**
	 * Disconnects all direct edges that satisfy the specified condition
	 * @param f A condition for removing an edge from this graph
	 */
	def disconnectDirectWhere(f: Edge => Boolean) = leavingEdges = leavingEdges.filterNot(f)
	/**
	 * Disconnects all edges within this graph that satisfy the specified condition
	 * @param f A condition for removing edges from this graph
	 */
	def disconnectWhere(f: Edge => Boolean) = {
		val visitedNodes = mutable.Set[Node](this)
		OrderedDepthIterator
			.apply(Iterator.single(self)) { node =>
				node.disconnectDirectWhere(f)
				val nextLayer = node.leavingEdges.view.map { _.end }.toSet
				val nextNodes = nextLayer.diff(visitedNodes)
				visitedNodes ++= nextNodes
				nextNodes
			}
			.foreach { _ => () }
	}
}