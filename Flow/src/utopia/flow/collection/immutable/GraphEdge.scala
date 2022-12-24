package utopia.flow.collection.immutable

import utopia.flow.collection.template

/**
  * Graph edges are used for connecting two nodes and storing data. Edges are immutable.
  * @author Mikko Hilpinen
  * @since 28.10.2016
  */
case class GraphEdge[+A, +Node](override val value: A, override val end: Node)
	extends template.GraphEdge[A, Node]
{
	// OTHER METHODS    ----------
	
	/**
	  * Creates a new edge that has different value / nav element
	  * @param value The value of the new edge
	  */
	def withValue[B](value: B) = GraphEdge(value, end)
	/**
	  * Creates a new edge that has different content
	  * @param content The contents of the new edge
	  */
	@deprecated("Replaced with .withValue(B)", "v2.0")
	def withContent[B](content: B) = GraphEdge(content, end)
	
	/**
	  * Creates a new edge that has a different end node
	  * @param node The node the new edge points towards
	  */
	def pointingTo[N2](node: N2): GraphEdge[A, N2] = GraphEdge(value, node)
}
