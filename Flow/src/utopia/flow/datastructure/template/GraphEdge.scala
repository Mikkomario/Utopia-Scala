package utopia.flow.datastructure.template

/**
 * Graph edges are used for connecting two nodes and storing data
 * @author Mikko Hilpinen
 * @since 10.4.2019
 */
trait GraphEdge[+N, E, GNode <: GraphNode[N, E, GNode, _]] extends Node[E]
{
	// ABSTRACT	------------------
	
	/**
	  * @return The node this edge points to
	  */
	def end: GNode
}