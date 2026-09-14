package utopia.flow.collection.mutable.graph

import utopia.flow.collection.template.graph.GraphEdge
import utopia.flow.view.mutable.Pointer

/**
 * A mutable implementation of the graph edge -trait
 * @author Mikko Hilpinen
 * @since 13.09.2026, v2.9
 */
class MutableGraphEdge[A, Node](override var value: A, override val end: Node)
	extends GraphEdge[A, Node] with Pointer[A]
