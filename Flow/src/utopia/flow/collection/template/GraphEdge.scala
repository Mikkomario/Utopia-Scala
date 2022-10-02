package utopia.flow.collection.template

import utopia.flow.view.immutable.View

/**
  * Graph edges are used for connecting two graph nodes and storing data
  * @author Mikko Hilpinen
  * @since 10.4.2019
  */
trait GraphEdge[+V, +Node] extends View[V]
{
	// ABSTRACT	------------------
	
	/**
	  * @return The node this edge points to
	  */
	def end: Node
}
