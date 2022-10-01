package utopia.flow.collection.immutable

import utopia.flow.collection.template.GraphNode
import utopia.flow.view.immutable.View

object ViewGraphNode
{
	def apply[N, E](value: View[N], leavingEdges: Iterable[ViewGraphEdge[N, E]]) =
		new ViewGraphNode[N, E](value, leavingEdges)
}

/**
  * @author Mikko Hilpinen
  * @since 30.9.2022, v2.0
  */
class ViewGraphNode[N, E](valueView: View[N], override val leavingEdges: Iterable[ViewGraphEdge[N, E]])
	extends GraphNode[N, E, ViewGraphNode[N, E], ViewGraphEdge[N, E]]
{
	override protected def repr = this
	
	override def value = valueView.value
}
