package utopia.flow.collection.immutable

import utopia.flow.collection.immutable.caching.iterable.CachingSeq
import utopia.flow.collection.mutable.iterator.LazyInitIterator
import utopia.flow.collection.template.GraphNode
import utopia.flow.operator.Identity
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
	/**
	  * Creates a new lazily initialized graph node
	  * @param value A function that yields the value of this node (lazily called)
	  * @param leavingEdges A function that yields the edges that leave from this node (lazily called, cached)
	  * @tparam N Type of node values
	  * @tparam E Type of edge values
	  * @return A new graph view node
	  */
	def lazily[N, E](value: => N)(leavingEdges: => IterableOnce[ViewGraphEdge[N, E]]) =
		apply(Lazy(value), CachingSeq(LazyInitIterator(leavingEdges)))
	
	/**
	  * Creates a new graph by utilizing an edge-generating function.
	  * The graph is created lazily. I.e. new nodes are created only when they're requested through using the graph.
	  *
	  * Note: It is expected here that a single node content of type N only appears in a single node.
	  * All other references to that value will be considered references to the first node that was
	  * created with/wrapping that value.
	  *
	  * For example, if we started with node of value 1,
	  * and edgesFrom(1) yielded an edge pointing to value 2,
	  * and edgesFrom(2) yielded an edge pointing to value 1,
	  * that second edge (i.e. edge from 2 to 1) would be pointing to the starting node,
	  * i.e. the first node that was created with value 1, and would **not** result in a new node being created.
	  * Hence, in the resulting graph, there would be exactly one node with value 1.
	  * Similarly, there can never be more than one node with any unique value -
	  * Note that unique here refers to equality, when == is used, or equal hashCode.
	  *
	  * The edges don't have a similar restriction. For example, there can be multiple edges with value "X",
	  * even leaving from the same node.
	  *
	  * @param start The value of this starting node (i.e. value of the resulting node)
	  * @param edgesFrom A function that accepts a (unique) node value and yields a collection of edges (1) leaving
	  *                  from that node.
	  *
	  *                  (1): The edges are represented with two values:
	  *                     - 1: The value of that edge, as a View (for example View or Lazy)
	  *                     - 2: The value of the node this edge points to. Again, as a View
	  *                     (where Lazy would be a likely choice).
	  *
	  *                  This function is called lazily, i.e. only before they're used in a graph operation.
	  *                  Similarly, the iterator/collection returned by this function is consumed lazily and cached.
	  *                  Therefore, it is recommended that you return an Iterator and not a pre-initialized collection,
	  *                  where possible and reasonable.
	  *
	  *                  This is not critical if you return the edge end values as Lazily initialized views
	  *                  (i.e. as instances of Lazy), as that will also cause the end nodes to be initialized lazily.
	  *                  If this function returns a pre-initialized collection with non-lazy or pre-initialized
	  *                  end values, that results in all the end nodes being initialized at once.
	  *                  This is acceptable, but not necessarily the behaviour you want.
	  *
	  * @tparam N Type of graph node values (recommended to extend Equals)
	  * @tparam E Type of edge values
	  * @return The generated "starting" node, representing a new lazily initialized graph.
	  *         The other nodes in this graph are initialized whenever they're called through the use of this graph.
	  */
	def iterate[N, E](start: N)(edgesFrom: N => IterableOnce[(View[E], View[N])]): ViewGraphNode[N, E] = {
		val createdNodes = mutable.Map[N, ViewGraphNode[N, E]]()
		_iterate[N, E](createdNodes, start)(edgesFrom)
	}
	private def _iterate[N, E](createdNodes: mutable.Map[N, ViewGraphNode[N, E]], value: N)
	                          (edgesFrom: N => IterableOnce[(View[E], View[N])]): ViewGraphNode[N, E] =
	{
		// The leaving edges are initialized lazily
		val edges = CachingSeq.from(LazyInitIterator { edgesFrom(value) }
			.map { case (edgeValueView, endValueView) =>
				// Checks whether a referenced node has already been cached,
				// creates a new node only for new values
				val endView = endValueView.mapValue { endValue =>
					createdNodes.getOrElse(endValue,  _iterate[N, E](createdNodes, endValue)(edgesFrom))
				}
				ViewGraphEdge(edgeValueView, endView)
			})
		val node = apply(View(value), edges)
		// Caches nodes as soon as they've been created
		createdNodes += (value -> node)
		node
	}
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
	
	/**
	  * Type of nodes in this graph
	  */
	type Node = ViewGraphNode[N, E]
	/**
	  * Type of edges in this graph
	  */
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
	def mapAll[N2, E2](valueMap: N => N2)(edgeValueMap: E => E2) =
		_map { _.mapValue(valueMap) } { _.mapValue(edgeValueMap) }
	/**
	  * Maps all node values in this graph
	  * @param f A mapping function applied to graph node values
	  * @tparam N2 New node value type
	  * @return A mapped copy of this node / graph
	  */
	def mapValues[N2](f: N => N2) = _map { _.mapValue(f) }(Identity)
	/**
	  * Maps all edge values in this graph
	  * @param f A mapping function applied to this graph's edge values
	  * @tparam E2 Mapping result type
	  * @return A mapped copy of this node / graph
	  */
	def mapEdgeValues[E2](f: E => E2) = _map(Identity) { _.mapValue(f) }
	
	private def _map[N2, E2](valueMap: View[N] => View[N2])(edgeMap: View[E] => View[E2]): ViewGraphNode[N2, E2] = {
		val mappedNodes = mutable.Map[ViewGraphNode[N, E], ViewGraphNode[N2, E2]]()
		_map(mappedNodes)(valueMap)(edgeMap)
	}
	private def _map[N2, E2](mappedNodes: mutable.Map[Node, ViewGraphNode[N2, E2]])
	                        (valueMap: View[N] => View[N2])(edgeMap: View[E] => View[E2]): ViewGraphNode[N2, E2] =
	{
		// Maps the edge end nodes lazily
		val newEdges = leavingEdges.map { edge => ViewGraphEdge(edgeMap(edge.valueView),
			Lazy { mappedNodes.getOrElse(edge.end, edge.end._map(mappedNodes)(valueMap)(edgeMap)) }) }
		val result = ViewGraphNode(valueMap(valueView), newEdges)
		// Stores mapping results so that same nodes will map to same instances and infinite recursive loops are avoided
		mappedNodes += (this -> result)
		result
	}
}
