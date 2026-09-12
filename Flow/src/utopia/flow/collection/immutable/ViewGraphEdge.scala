package utopia.flow.collection.immutable

import utopia.flow.collection.template.GraphEdge
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy

@deprecated("Please use the new GraphEdge in template.tree instead", "v2.9")
object ViewGraphEdge
{
	def apply[N, E](value: View[E], end: View[ViewGraphNode[N, E]]) = new ViewGraphEdge[N, E](value, end)
	
	def lazily[N, E](value: => E, end: => ViewGraphNode[N, E]) = apply(Lazy(value), Lazy(end))
}

/**
  * A lazily initialized graph edge
  * @author Mikko Hilpinen
  * @since 30.9.2022, v2.0
  */
@deprecated("Please use the new GraphEdge in template.tree instead", "v2.9")
class ViewGraphEdge[N, E](val valueView: View[E], val endView: View[ViewGraphNode[N, E]])
	extends GraphEdge[E, ViewGraphNode[N, E]]
{
	// IMPLEMENTED  -----------------------
	
	override def end = endView.value
	override def value = valueView.value
}
