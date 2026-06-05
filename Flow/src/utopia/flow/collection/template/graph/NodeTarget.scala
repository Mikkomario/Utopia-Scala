package utopia.flow.collection.template.graph

import utopia.flow.collection.template.GraphEdge
import utopia.flow.view.immutable.View

import scala.language.implicitConversions

object NodeTarget
{
	// COMPUTED -------------------------
	
	/**
	  * @return A node target that accepts any node
	  */
	def any = AnyNode
	
	
	// IMPLICIT -------------------------
	
	/**
	  * @param node A specific node
	  * @return A node target that only accepts that node (using ==)
	  */
	implicit def apply(node: View[Any]): NodeTarget[Any, Any] = new SpecificNodeTarget(node)
	
	/**
	  * @param f A function that receives:
	  *          1. A graph node (as a View)
	  *          1. View to the leaving node edges
	  *
	  *          And yields whether that's the targeted node
	  * @tparam N Type of node values accepted
	  * @tparam E Type of edge values accepted
	  * @return A node target that uses the specified function
	  */
	implicit def apply[N, E](f: (View[N], View[Iterable[GraphEdge[E, View[N]]]]) => Boolean): NodeTarget[N, E] =
		new _NodeTarget[N, E](f)
	
	
	// OTHER    -------------------------
	
	/**
	  * @param value Specific node value
	  * @tparam A Type of the accepted values
	  * @return A node target that only accepts nodes with that value (using ==)
	  */
	def value[A](value: A): NodeTarget[A, Any] = new SpecificValueTarget[A](value)
	
	
	// NESTED   -------------------------
	
	object AnyNode extends NodeTarget[Any, Any]
	{
		override def apply(node: View[Any], edges: => Iterable[GraphEdge[Any, View[Any]]]): Boolean = true
	}
	
	private class SpecificNodeTarget(node: View[Any]) extends NodeTarget[Any, Any]
	{
		override def apply(node: View[Any], edges: => Iterable[GraphEdge[Any, View[Any]]]): Boolean = node == this.node
	}
	
	private class SpecificValueTarget[-A](value: A) extends NodeTarget[A, Any]
	{
		override def apply(node: View[A], edges: => Iterable[GraphEdge[Any, View[A]]]): Boolean = node.value == value
	}
	
	private class _NodeTarget[-N, -E](f: (View[N], View[Iterable[GraphEdge[E, View[N]]]]) => Boolean)
		extends NodeTarget[N, E]
	{
		override def apply(node: View[N], edges: => Iterable[GraphEdge[E, View[N]]]): Boolean = f(node, View(edges))
	}
}

/**
  * Used for finding specific graph nodes
  * @tparam N Type of tested node contents
  * @tparam E Type of tested edge contents
  * @author Mikko Hilpinen
  * @since 04.06.2026, v2.9
  */
trait NodeTarget[-N, -E]
{
	// ABSTRACT ------------------------
	
	/**
	  * Determines whether a node is the searched target node
	  * @param node A node
	  * @param edges Edges leaving from that node (call-by-name)
	  * @return Whether that's the targeted node
	  */
	def apply(node: View[N], edges: => Iterable[GraphEdge[E, View[N]]]): Boolean
}
