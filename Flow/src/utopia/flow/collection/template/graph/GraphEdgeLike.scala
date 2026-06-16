package utopia.flow.collection.template.graph

import utopia.flow.view.immutable.View
import utopia.flow.view.template.Extender

/**
 * Graph edges are used for connecting two graph nodes and storing data
 * @author Mikko Hilpinen
 * @since 05.06.2026, v2.9
 */
trait GraphEdgeLike[+A, +Node] extends View[A] with Extender[A]
{
	// ABSTRACT	------------------
	
	/**
	 * @return The node this edge points to
	 */
	def end: Node
	
	
	// IMPLEMENTED  --------------
	
	override def wrapped: A = value
}
