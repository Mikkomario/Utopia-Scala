package utopia.flow.collection.template

import utopia.flow.view.immutable.View
import utopia.flow.view.template.Extender

/**
  * Graph edges are used for connecting two graph nodes and storing data
  * @author Mikko Hilpinen
  * @since 10.4.2019
  */
trait GraphEdge[+V, +Node] extends View[V] with Extender[V]
{
	// ABSTRACT	------------------
	
	/**
	  * @return The node this edge points to
	  */
	def end: Node
	
	
	// IMPLEMENTED  --------------
	
	override def wrapped: V = value
}
