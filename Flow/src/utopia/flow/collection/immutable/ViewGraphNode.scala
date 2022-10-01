package utopia.flow.collection.immutable

import utopia.flow.collection.immutable.Graph.GraphViewNode
import utopia.flow.collection.template.GraphNode
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy

import scala.collection.mutable

object ViewGraphNode
{
	/**
	  * Creates a new graph node
	  * @param value A view that yields the (current) value of this node
	  * @param leavingEdges Edges that leave this node.
	  *                     For lazy initialization, you may wish to utilize CachingSeq or LazySeq
	  * @tparam N Type of node values
	  * @tparam E Type of edge values
	  * @return A new node
	  */
	def apply[N, E](value: View[N], leavingEdges: Iterable[ViewGraphEdge[N, E]]) =
		new ViewGraphNode[N, E](value, leavingEdges)
}

/**
  * A graph node that utilizes views to enable lazy initialization
  * (which is often required when dealing with self-referential items, in this case cyclic graphs)
  * @author Mikko Hilpinen
  * @since 30.9.2022, v2.0
  */
class ViewGraphNode[N, E](valueView: View[N], override val leavingEdges: Iterable[ViewGraphEdge[N, E]])
	extends GraphNode[N, E, ViewGraphNode[N, E], ViewGraphEdge[N, E]]
{
	// TYPES    --------------------------------
	
	type Node = ViewGraphNode[N, E]
	type Edge = ViewGraphEdge[N, E]
	
	
	// IMPLEMENTED  ----------------------------
	
	override protected def repr = this
	
	override def value = valueView.value
	
	
	// OTHER    --------------------------------
	
	/**
	  * Maps values in these nodes, as well as edge values.
	  * Note that this mapping is done for all nodes in this graph.
	  * The mapping is performed lazily.
	  * @param valueMap A mapping function applied for node values
	  * @param edgeValueMap A mapping function applied for edge values
	  * @tparam N2 Type of new node value
	  * @tparam E2 Type of new edge value
	  * @return A new mapped node
	  */
	def mapAll[N2, E2](valueMap: N => N2)(edgeValueMap: E => E2) = {
		val mappedNodes = mutable.Map[ViewGraphNode[N, E], ViewGraphNode[N2, E2]]()
		_mapAll(mappedNodes)(valueMap)(edgeValueMap)
	}
	private def _mapAll[N2, E2](mappedNodes: mutable.Map[Node, ViewGraphNode[N2, E2]])
	                           (valueMap: N => N2)(edgeMap: E => E2): ViewGraphNode[N2, E2] =
	{
		// Maps the edge end nodes lazily
		val newEdges = leavingEdges.map { edge => ViewGraphEdge(edge.valueView.mapValue(edgeMap),
			Lazy { mappedNodes.getOrElse(edge.end, edge.end._mapAll(mappedNodes)(valueMap)(edgeMap)) }) }
		val result = ViewGraphNode(valueView.mapValue(valueMap), newEdges)
		// Stores mapping results so that same nodes will map to same instances and infinite recursive loops are avoided
		mappedNodes += (this -> result)
		result
	}
	
	/**
	  * Maps all node values in this graph
	  * @param f A mapping function applied to graph node values
	  * @tparam N2 New node value type
	  * @return A mapped copy of this node / graph
	  */
	def mapValues[N2](f: N => N2) = {
		val mappedNodes = mutable.Map[ViewGraphNode[N, E], ViewGraphNode[N2, E]]()
		_mapValues(mappedNodes)(f)
	}
	// TODO: WET WET
	private def _mapValues[N2](mappedNodes: mutable.Map[Node, ViewGraphNode[N2, E]])
	                          (valueMap: N => N2): ViewGraphNode[N2, E] =
	{
		// Maps the edge end nodes lazily
		val newEdges = leavingEdges.map { edge => ViewGraphEdge(edge.valueView,
			Lazy { mappedNodes.getOrElse(edge.end, edge.end._mapValues(mappedNodes)(valueMap)) }) }
		val result = ViewGraphNode(valueView.mapValue(valueMap), newEdges)
		// Stores mapping results so that same nodes will map to same instances and infinite recursive loops are avoided
		mappedNodes += (this -> result)
		result
	}
}
