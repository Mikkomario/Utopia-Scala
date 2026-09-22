package utopia.flow.test.collection.graph

import utopia.flow.collection.immutable.graph.GraphNode
import utopia.flow.collection.mutable.graph.MutableGraphNode
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.collection.CollectionExtensions._

/**
 * Tests functions defined in GraphLike using an immutable GraphNode implementation, based on a mutable version.
 * @author Mikko Hilpinen
 * @since 20.09.2026, v2.9
 */
object GraphLikeTest extends App
{
	// Sets up the test graph
	/*
					  +--3-> G
					  |
		A -1-> B -2-> C -3-> +
			   |             |
			   + <---2/4---> D -4-> E -5-> F
	 */
	private val a = {
		val nodes = Vector("A", "B", "C", "D", "E", "F", "G").iterator
			.map { v => v -> MutableGraphNode[String, Int](v) }.toMap
		nodes("A").connect(1, nodes("B"))
		nodes("B").connect(2, nodes("C"))
		nodes("C").connect(3, nodes("D"))
		nodes("D").connect(4, nodes("E"))
		nodes("B").connect(2, nodes("D"))
		nodes("D").connect(4, nodes("B"))
		nodes("E").connect(5, nodes("F"))
		nodes("C").connect(3, nodes("G"))
		
		GraphNode.from(nodes("A"))
	}
	implicit val navEquals: EqualsFunction[String] = EqualsFunction.stringCaseInsensitive
	private val nav = a.navigate
	
	// navigation && endNodes
	private val b = nav("B")
	private val c = nav("B", "C")
	private val d = nav("B", "D")
	private val e = d("E")
	private val f = e("F")
	private val g = c("G")
	
	assert(a.endNodes.only.get.value == "B")
	assert(b.endNodesIterator.map { _.value }.toVector == Vector("C", "D"))
	assert(c.endNodesIterator.map { _.value }.toVector == Vector("D", "G"))
	assert(d.endNodesIterator.map { _.value }.toVector == Vector("E", "B"))
	assert(e.endNodes.only.get.value == "F", e.leavingEdges.size)
	
	// allNodes
	assert(a.allNodesIterator.map { _.value }.toVector == Vector("A", "B", "C", "D", "E", "F", "G"))
	assert(b.allNodesIterator.map { _.value }.toVector == Vector("B", "C", "D", "E", "F", "G"))
	assert(c.allNodesIterator.map { _.value }.toVector == Vector("C", "D", "E", "F", "B", "G"))
	assert(d.allNodesIterator.map { _.value }.toVector == Vector("D", "E", "F", "B", "C", "G"))
	assert(e.allNodesIterator.map { _.value }.toVector == Vector("E", "F"))
	assert(f.allNodesIterator.map { _.value }.toVector == Vector("F"))
	assert(g.allNodesIterator.map { _.value }.toVector == Vector("G"))
	
	// orderedAllNodes
	assert(a.orderedAllNodesIterator.map { _.value }.toVector == Vector("A", "B", "C", "D", "G", "E", "F"))
	assert(b.orderedAllNodesIterator.map { _.value }.toVector == Vector("B", "C", "D", "G", "E", "F"))
	assert(c.orderedAllNodesIterator.map { _.value }.toVector == Vector("C", "D", "G", "E", "B", "F"),
		c.orderedAllNodesIterator.map { _.value }.mkString(" -> "))
	assert(d.orderedAllNodesIterator.map { _.value }.toVector == Vector("D", "E", "B", "F", "C", "G"))
	assert(e.orderedAllNodesIterator.map { _.value }.toVector == Vector("E", "F"))
	assert(f.orderedAllNodesIterator.map { _.value }.toVector == Vector("F"))
	assert(g.orderedAllNodesIterator.map { _.value }.toVector == Vector("G"))
	
	// shortestRoutesIterator
	{
		val routes = c.shortestRoutesIterator
			.map { case (route, end) => end.value -> route.map { _.end.value }.mkString }.toMap
		assert(routes("D") == "D")
		assert(routes("E") == "DE")
		assert(routes("F") == "DEF")
		assert(routes("B") == "DB")
		assert(routes("G") == "G")
		assert(!routes.contains("A"))
	}
	
	// allEdgesIterator
	assert(c.allEdgesIterator.map { _.value }.toVector == Vector(3, 3, 4, 4, 5, 2, 2),
		c.allEdgesIterator.map { _.value }.mkString(" -> "))
	
	// allValuesIterator
	assert(c.allValuesIterator.mkString == "CDEFBG")
	
	// toValueTree
	{
		val t = a.toValueTree
		assert(t.value == "A")
		assert(t.children.only.get.value == "B")
		assert(t.get("B", "C", "G").exists { _.isEmpty })
		assert(t.get("B", "D").exists { _.children.only.get.value == "E" })
		assert(t.get("B", "C", "D", "E", "F").isDefined)
	}
	
	// routesToSelf
	assert(b.routesToSelf.only.get.iterator.map { _.end.value }.mkString == "CDB")
	assert(d.routesToSelf.only.get.iterator.map { _.end.value }.mkString == "BCD")
	
	// lookup
	assert(a.lookup.get("B", "C", "D", "E", "F").isDefined)
	assert(a.lookup.get("B", "C", "G").isDefined)
	assert(a.lookup.get("B", "C", "X").isEmpty)
	
	// traverseEdges
	assert(a.traverseEdges.get(1, 2, 3).exists { _.value == "D" })
	assert(a.traverseEdges.get(1, -1).isEmpty)
	
	
	/*
					  +--3-> G
					  |
		A -1-> B -2-> C -3-> +
			   |             |
			   + <---2/4---> D -4-> E -5-> F
	 */
	
	println("Success!")
}
