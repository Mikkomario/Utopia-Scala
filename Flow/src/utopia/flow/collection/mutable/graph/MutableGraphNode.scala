package utopia.flow.collection.mutable.graph

import utopia.flow.collection.immutable.Empty
import utopia.flow.collection.mutable.iterator.OrderedDepthIterator
import utopia.flow.collection.template
import utopia.flow.collection.template.graph.{GraphNode, GraphNodeLike}
import utopia.flow.view.mutable.Pointer

import scala.collection.mutable

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