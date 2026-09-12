package utopia.flow.collection.immutable.graph

import utopia.flow.collection.template.graph.{GraphEdgeLike, GraphNodeLike}
import utopia.flow.operator.Identity
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy

import scala.collection.mutable

object CopyableGraphNodeLike
{
	private def map[N, E, Node <: GraphNodeLike[N, E, Node, GraphEdgeLike[E, Node]], NC[_, _], EC[_, _], N2, E2]
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
	private def flatMap[N, E, Node <: GraphNodeLike[N, E, Node, GraphEdgeLike[E, Node]], NC[_, _], EC[_, _], N2, E2]
	                   (root: Node, newValue: N2, factory: GraphFactory[NC, EC], mappedNodes: mutable.Map[Any, NC[N2, E2]])
	                   (edgeMap: (N, N2, E) => IterableOnce[(E2, View[N2])]): NC[N2, E2] =
	{
		// Maps the edge end nodes lazily
		val newEdges = root.leavingEdges.flatMap { edge =>
			edgeMap(root.value, newValue, edge.value).iterator.map { case (newContent, endView) =>
				factory.edge(newContent,
					endView.mapValue { newValue =>
						mappedNodes.getOrElseUpdate(edge.end, flatMap(edge.end, newValue, factory, mappedNodes)(edgeMap))
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
trait CopyableGraphNodeLike[+N, +E, NC[_, +_], EC[+_, +_], +Repr <: GraphNodeLike[N, E, Repr, Edge], +Edge <: GraphEdgeLike[E, Repr] with EC[N, E]]
	extends GraphNodeLike[N, E, Repr, Edge]
{
	// ABSTRACT -------------------------
	
	// TODO: Document
	
	protected def factory: GraphFactory[NC, EC]
	
	def filterDirect(f: Edge => Boolean): Repr
	def filter(f: (N, Edge) => Boolean): Repr
	
	
	// OTHER ----------------------------
	
	def withValue[N2 >: N](newValue: N2) = factory.node(newValue, leavingEdges)
	def mapLocalValue[N2 >: N](f: N => N2) = withValue(f(value))
	
	def withEdges[N2 >: N, E2 >: E](edges: IterableOnce[EC[N2, E2]]) = factory.node(value, edges)
	
	def mapNodes[N2](f: N => N2): NC[N2, E] = map(f)(Identity)
	def mapEdges[N2 >: N, E2](f: E => E2): NC[N2, E2] = map[N2, E2](Identity)(f)
	def map[N2, E2](mapNode: N => N2)(mapEdge: E => E2): NC[N2, E2] =
		CopyableGraphNodeLike.map(self, factory, mutable.Map())(mapNode)(mapEdge)
	
	def flatMapEdges[N2 >: N, E2](f: (N, E) => IterableOnce[(E2, View[N2])]): NC[N2, E2] =
		flatMap[N2, E2](Identity) { (value, _, edge) => f(value, edge) }
	def flatMap[N2, E2](mapSelf: N => N2)(mapEdge: (N, N2, E) => IterableOnce[(E2, View[N2])]): NC[N2, E2] =
		CopyableGraphNodeLike.flatMap(self, mapSelf(value), factory, mutable.Map())(mapEdge)
}
