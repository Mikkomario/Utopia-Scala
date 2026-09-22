package utopia.flow.collection.immutable.graph

import utopia.flow.collection.template.graph.{GraphEdge, GraphNodeLike}
import utopia.flow.operator.Identity
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy

import scala.collection.mutable

object CopyableGraphNodeLike
{
	private def map[N, E, Node <: GraphNodeLike[N, E, Node, GraphEdge[E, Node]], NC[_, _], EC[_, _], N2, E2]
	               (root: Node, factory: GraphFactory[NC, EC], mappedNodes: mutable.Map[Any, NC[N2, E2]])
	               (valueMap: N => N2)(edgeMap: E => E2): NC[N2, E2] =
	{
		// Maps the edge end nodes lazily
		val newEdges = root.leavingEdges.map { edge =>
			factory.edge(edgeMap(edge.value), Lazy {
				mappedNodes.getOrElseUpdate(edge.end,
					map[N, E, Node, NC, EC, N2, E2](edge.end, factory, mappedNodes)(valueMap)(edgeMap))
			})
		}
		val result = factory.node[N2, E2](valueMap(root.value), newEdges)
		// Stores mapping results so that same nodes will map to same instances and infinite recursive loops are avoided
		mappedNodes += (root -> result)
		result
	}
	private def flatMap[N, E, Node <: GraphNodeLike[N, E, Node, GraphEdge[E, Node]], NC[_, _], EC[_, _], N2, E2]
	                   (root: Node, newValue: N2, factory: GraphFactory[NC, EC], mappedNodes: mutable.Map[Any, NC[N2, E2]])
	                   (edgeMap: (N, N2, E) => IterableOnce[(E2, View[N2])]): NC[N2, E2] =
	{
		// Maps the edge end nodes lazily
		val newEdges = root.leavingEdges.flatMap { edge =>
			edgeMap(root.value, newValue, edge.value).iterator.map { case (newContent, endView) =>
				factory.edge(newContent,
					endView.mapValue { newValue =>
						mappedNodes.getOrElseUpdate(edge.end,
							flatMap[N, E, Node, NC, EC, N2, E2](edge.end, newValue, factory, mappedNodes)(edgeMap))
					})
			}
		}
		val result = factory.node[N2, E2](newValue, newEdges)
		// Stores mapping results so that same nodes will map to same instances and infinite recursive loops are avoided
		mappedNodes += (root -> result)
		result
	}
}

/**
 * Common trait for immutable & copyable graph node implementations
 * @tparam N Type of node values
 * @tparam E Type of edge values
 * @tparam NC Type of generic node constructors
 * @tparam EC Type of generic edge constructors
 * @tparam Repr Type of node implementations
 * @tparam Edge Type of edge implementations
 * @author Mikko Hilpinen
 * @since 11.09.2026, v2.9
 */
trait CopyableGraphNodeLike[+N, +E, NC[_, +_], EC[+_, +_], +Repr <: GraphNodeLike[N, E, Repr, Edge], +Edge <: GraphEdge[E, Repr] with EC[N, E]]
	extends GraphNodeLike[N, E, Repr, Edge]
{
	// ABSTRACT -------------------------
	
	/**
	 * @return Factory used for constructing copies of this node
	 */
	def factory: GraphFactory[NC, EC]
	
	/**
	 * @param f A filtering function applied to edges leaving from this node
	 * @return A copy of this ode with only edges accepted by the specified filtering function
	 */
	def filterDirect(f: Edge => Boolean): Repr
	/**
	 * @param f A filtering function applied to all edges within this graph.
	 *          Receives two values:
	 *          1. Value of the node from which the edge originates
	 *          1. The edge to filter
	 * @return A copy of this node with only edges accepted by the specified filtering function.
	 *         Also, nodes accessible from this node have also been filtered
	 *         to only include edges accepted by the specified filtering function.
	 */
	def filter(f: (N, Edge) => Boolean): Repr
	
	
	// OTHER ----------------------------
	
	/**
	 * @param newValue A new value to assign to this node
	 * @tparam N2 Type of the new value
	 * @return A copy of this node with the specified value
	 */
	def withValue[N2 >: N](newValue: N2) = factory.node(newValue, leavingEdges)
	/**
	 * @param f A mapping function applied to this node's value
	 * @tparam N2 Type of mapping results
	 * @return A copy of this node with a mapped value
	 */
	def mapLocalValue[N2 >: N](f: N => N2) = withValue(f(value))
	
	/**
	 * @param edges New edges to assign to this node (exclusive)
	 * @tparam N2 Type of the node values accessible via these edges
	 * @tparam E2 Type of the new edge values
	 * @return A copy of this node containing the specified edges
	 */
	def withEdges[N2 >: N, E2 >: E](edges: IterableOnce[EC[N2, E2]]) = factory.node(value, edges)
	
	/**
	 * Maps the values of all nodes in this graph
	 * @param f A mapping function applied to all node values in this graph
	 * @tparam N2 Type of the new node values
	 * @return A copy of this node with mapped node values
	 */
	def mapNodes[N2](f: N => N2): NC[N2, E] = map(f)(Identity)
	/**
	 * Maps all edge values in this graph
	 * @param f A mapping function applied to all edge values in this graph
	 * @tparam N2 Type of the new node values
	 * @tparam E2 Type of the new edge values
	 * @return A copy of this node with mapped edge values
	 */
	def mapEdges[N2 >: N, E2](f: E => E2): NC[N2, E2] = map[N2, E2](Identity)(f)
	/**
	 * Maps the values of all nodes and all edges in this graph
	 * @param mapNode A mapping function applied to all node values in this graph
	 * @param mapEdge A mapping function applied to all edge values in this graph
	 * @tparam N2 Type of the new node values
	 * @tparam E2 Type of the new edge values
	 * @return A copy of this node with mapped values
	 */
	def map[N2, E2](mapNode: N => N2)(mapEdge: E => E2): NC[N2, E2] =
		CopyableGraphNodeLike.map[N, E, Repr, NC, EC, N2, E2](self, factory, mutable.Map())(mapNode)(mapEdge)
		
	@deprecated("Renamed to .mapNodes(...)", "v2.9")
	def mapValues[N2](f: N => N2): NC[N2, E] = mapNodes(f)
	
	/**
	 * Modifies all edges in this graph, possibly adding or removing them.
	 * @param f A mapping function applied to all edges in this graph.
	 *          Receives two values:
	 *          1. Value of the node from which the edge originates
	 *          1. The edge value to map
	 *
	 *          Yields 0-n edge representations, where each contains two values:
	 *          1. New edge value to apply
	 *          1. A view to the node value at the end of this edge
	 * @tparam N2 Type of new node values
	 * @tparam E2 Type of new edge values
	 * @return A copy of this node with mapped edges
	 */
	def flatMapEdges[N2 >: N, E2](f: (N, E) => IterableOnce[(E2, View[N2])]): NC[N2, E2] =
		flatMap[N2, E2](Identity) { (value, _, edge) => f(value, edge) }
	/**
	 * Modifies all edges and node values in this graph, possibly adding or removing edges.
	 * @param mapSelf A mapping function applied to this node's local value
	 * @param mapEdge A mapping function applied to all edges in this graph.
	 *          Receives three values:
	 *          1. Original value of the node from which the edge originates
	 *          1. Mapped value of the node from which the edge originates
	 *          1. The edge value to map
	 *
	 *          Yields 0-n edge representations, where each contains two values:
	 *          1. New edge value to apply
	 *          1. A view to the node value at the end of this edge
	 * @tparam N2 Type of new node values
	 * @tparam E2 Type of new edge values
	 * @return A copy of this node with mapped values and edges
	 */
	def flatMap[N2, E2](mapSelf: N => N2)(mapEdge: (N, N2, E) => IterableOnce[(E2, View[N2])]): NC[N2, E2] =
		CopyableGraphNodeLike.flatMap[N, E, Repr, NC, EC, N2, E2](self, mapSelf(value), factory, mutable.Map())(mapEdge)
}
