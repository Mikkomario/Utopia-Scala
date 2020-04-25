package utopia.flow.datastructure.mutable

import utopia.flow.datastructure.template

import scala.collection.immutable.HashSet
import utopia.flow.datastructure.immutable.GraphEdge

/**
 * Graph nodes contain content and are connected to other graph nodes via edges
 * @author Mikko Hilpinen
 * @since 28.10.2016
 */
class GraphNode[N, E](var content: N) extends template.GraphNode[N, E, GraphNode[N, E], GraphEdge[N, E, GraphNode[N, E]]]
{
    // TYPES    --------------------
    
    type Node = GraphNode[N, E]
    type Edge = GraphEdge[N, E, Node]
    
    
    // IMPLEMENTED    --------------
    
    var leavingEdges: Set[Edge] = HashSet()
    
    override protected def repr = this
    
    
    // OTHER METHODS    ------------
    
    /**
      * @return A copy of this mutable node
      */
    def copy() = new GraphNode(content, leavingEdges)
    
    /**
     * Connects this node to another node, creating a new edge. This will always create a new edge, 
     * even when there exists a connection already.
     * @param node The node this node will be connected to
     * @param edgeContent The contents for the edge that is generated
     */
    def connect(node: Node, edgeContent: E) = leavingEdges += GraphEdge(edgeContent, node)
    
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
      * Disconnects every node in this graph from the specified node
      * @param node A node
      */
    def disconnectAll(node: AnyNode) = foreach { _.disconnectDirect(node) }
}