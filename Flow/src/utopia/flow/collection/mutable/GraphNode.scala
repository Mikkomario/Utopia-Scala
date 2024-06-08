package utopia.flow.collection.mutable

import utopia.flow.collection.immutable.{Empty, GraphEdge}
import utopia.flow.collection.mutable.iterator.OrderedDepthIterator
import utopia.flow.collection.template
import utopia.flow.collection.template.GraphNode.AnyNode

import scala.collection.mutable

/**
 * Graph nodes contain content and are connected to other graph nodes via edges
 * @author Mikko Hilpinen
 * @since 28.10.2016
 */
class GraphNode[N, E](var value: N) extends template.GraphNode[N, E, GraphNode[N, E], GraphEdge[E, GraphNode[N, E]]]
{
    // TYPES    --------------------
    
    type Node = GraphNode[N, E]
    type Edge = GraphEdge[E, Node]
    
    
    // IMPLEMENTED    --------------
    
    var leavingEdges: IndexedSeq[Edge] = Empty
    
    override def self = this
    
    
    // OTHER METHODS    ------------
    
    /**
      * @return A copy of this mutable node
      */
    def copy() = new GraphNode(value, leavingEdges)
    
    /**
     * Connects this node to another node, creating a new edge. This will always create a new edge, 
     * even when there exists a connection already.
     * @param node The node this node will be connected to
     * @param edgeContent The contents for the edge that is generated
     */
    def connect(node: Node, edgeContent: E) = leavingEdges :+= GraphEdge(edgeContent, node)
    
    /**
     * Replaces any existing connections to a certain node with a new edge
     * @param node The node this node will be connected to
     * @param edgeContent The content of the edge connecting the nodes
     */
    def setConnection(node: Node, edgeContent: E) =
    {
        if (isDirectlyConnectedTo(node))
            disconnectDirect(node)
        
        connect(node, edgeContent)
    }
    
    /**
     * Removes any connection(s) to the provided node from this node. The provided node may still 
     * contain edges towards this node afterwards.
     * @param node The node that is disconnected from this node
     */
    def disconnectDirect(node: AnyNode) = leavingEdges = leavingEdges.filterNot(edge => edge.end == node)
    
    /**
      * Disconnects every node in this graph from the specified node, except those nodes that are accessible
      * only through the specified node.
      * @param node A node
      */
    def disconnectTotally(node: AnyNode) = {
        val visitedNodes = mutable.Set[Any](this)
        OrderedDepthIterator(Iterator.single(self)) { start =>
            start.leavingEdges = start.leavingEdges.filterNot { _.end == node }
            start.leavingEdges.flatMap { edge =>
                val node = edge.end
                if (visitedNodes.contains(node))
                    None
                else
                    Some(node)
            }
        }.foreach { _ => () }
    }
}