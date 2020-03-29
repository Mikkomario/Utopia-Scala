package utopia.flow.datastructure.immutable

import utopia.flow.datastructure.template

/**
 * Graph edges are used for connecting two nodes and storing data. Edges are immutable.
 * @author Mikko Hilpinen
 * @since 28.10.2016
 */
case class GraphEdge[N, E, GNode <: template.GraphNode[N, E, GNode, _]](override val content: E, override val end: GNode)
    extends template.GraphEdge[N, E, GNode]
{
    // OTHER METHODS    ----------
    
    /**
     * Creates a new edge that has different content
     * @param content The contents of the new edge
     */
    def withContent(content: E) = GraphEdge[N, E, GNode](content, end)
    
    /**
     * Creates a new edge that has a different end node
     * @param node The node the new edge points towards
     */
    def pointingTo[B, G <: template.GraphNode[B, E, G, _]](node: G): GraphEdge[B, E, G] = GraphEdge(content, node)
}