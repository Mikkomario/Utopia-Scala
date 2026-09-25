package utopia.flow.test.collection.graph

import utopia.flow.collection.immutable.graph.{Graph, GraphNode}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.mutable.graph.MutableGraphNode

/**
 * Tests the Graph class
 * @author Mikko Hilpinen
 * @since 24.09.2026, v2.9
 */
object GraphTest extends App
{
	// Sets up the test graphs
	/*
					  +--3-> G
					  |
		A -1-> B -2-> C -3-> +
			   |             |
			   + <----4----> D -4-> E -5-> F
	 */
	private val g1 = {
		val nodes = Vector("A", "B", "C", "D", "E", "F", "G").iterator
			.map { v => v -> MutableGraphNode[String, Int](v) }.toMap
		nodes("A").connect(1, nodes("B"))
		nodes("B").connect(2, nodes("C"))
		nodes("C").connect(3, nodes("D"))
		nodes("D").connect(4, nodes("E"))
		nodes("B").connect(4, nodes("D"))
		nodes("D").connect(4, nodes("B"))
		nodes("E").connect(5, nodes("F"))
		nodes("C").connect(3, nodes("G"))
		
		GraphNode.from(nodes("A")).toGraph
	}
	/*
					  +--3-> G
					  |
		A -1-> B -2-> C -3-> +
			   |             |
			   + <----4----- D -4-> E
	 */
	private val g2 = Graph.withConnections(Vector(
		("A", 1, "B"),
		("B", 2, "C"),
		("C", 3, "D"),
		("D", 4, "E"),
		("D", 4, "B"),
		("C", 3, "G")
	))
	/*
		A <-1-> B <-2-> C <-3-> +
			    |               |
			    + <-----4-----> D <-4-> E
	 */
	private val g3 = Graph.withConnections(Vector(
		("A", 1, "B"), ("B", 2, "C"), ("C", 3, "D"), ("D", 4, "B"), ("D", 4, "E")
	), twoWayBound = true)
	
	// isTwoWayBound
	assert(!g1.isTwoWayBound)
	assert(!g2.isTwoWayBound)
	assert(g3.isTwoWayBound)
	
	// connectionsIterator
	assert(g2.connectionsIterator.take(3).toVector == Vector(("A", 1, "B"), ("B", 2, "C"), ("C", 3, "D")))
	assert(g3.connectionsIterator.take(3).toVector == Vector(("A", 1, "B"), ("B", 2, "C"), ("C", 3, "D")))
	assert(g1.connectionsIterator.take(3).toVector == Vector(("A", 1, "B"), ("B", 2, "C"), ("B", 4, "D")))
	
	// headOption
	assert(g1.headOption.get.value == "A")
	assert(g2.headOption.get.value == "A")
	assert(g3.headOption.get.value == "A")

	// nodeValues
	assert(g1.nodeValues.iterator.take(3).toVector == Vector("A", "B", "C"))
	assert(g2.nodeValues.iterator.take(3).toVector == Vector("A", "B", "C"))
	assert(g3.nodeValues.iterator.take(3).toVector == Vector("A", "B", "C"))
	
	// nodes
	assert(g1.nodes.view.take(3).map { _.leavingEdges.iterator.map { _.value }.toVector }.toVector ==
		Vector(Vector(1), Vector(2, 4), Vector(3, 3)))
	assert(g2.nodes.view.take(3).map { _.leavingEdges.iterator.map { _.value }.toVector }.toVector ==
		Vector(Vector(1), Vector(2), Vector(3, 3)))
	assert(g3.nodes.view.take(3).map { _.leavingEdges.iterator.map { _.value }.toVector }.toVector ==
		Vector(Vector(1), Vector(2), Vector(3)))
	
	// node
	assert(g1.node("C").leavingEdges.size == 2)
	assert(g2.node("C").leavingEdges.size == 2)
	assert(g3.node("C").leavingEdges.size == 2)
	
	// subgraphs
	{
		val g4 = Graph.withConnections(Vector(("A" , 1, "B"), ("B", 2, "C"), ("D", 3, "E"), ("E", 4, "D")))
		val parts = g4.subgraphs.toOptimizedSeq
		
		assert(parts.size == 2)
		
		val p1 = parts.head
		val p2 = parts(1)
		
		assert(p1.nodeValues.toVector == Vector("A", "B", "C"))
		assert(p2.nodeValues.toVector == Vector("D", "E"))
	}
	
	// filterNodeValues
	{
		val g4 = g1.filterNodeValues { v => v != "B" }
		assert(g4.nodeValues.toVector == Vector("A", "C", "D", "E", "G"))
		assert(g4("D").leavingEdges.size == 1)
	}
	
	// filterNodes
	{
		val g4 = g1.filterNodes { _.leavingEdges.hasSize > 1 }
		assert(g4.nodeValues.toVector == Vector("B", "C", "D"))
		assert(g4("D").leavingEdges.size == 1)
	}
	
	// filterEdgeValues
	{
		val g4 = g1.filterEdgeValues { _ <= 3 }
		assert(g4.nodeValues.toVector == Vector("A", "B", "C", "D", "G"))
		assert(g4("D").leavingEdges.isEmpty)
		assert(g4("C").leavingEdges.size == 2)
	}
	
	// map
	{
		val g4 = g1.map { _.toLowerCase } { -_ }
		assert(g4.nodeValues.toVector == Vector("a", "b", "c", "d", "e", "f", "g"))
		assert(g4("d").leavingEdges.map { _.value } == Vector(-4, -4))
	}
	
	// TODO: Continue
}
