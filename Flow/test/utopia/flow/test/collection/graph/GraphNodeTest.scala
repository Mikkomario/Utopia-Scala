package utopia.flow.test.collection.graph

import utopia.flow.collection.immutable.graph.GraphNode
import utopia.flow.collection.mutable.graph.MutableGraphNode
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.template.graph.NodeTarget

/**
 * Tests functions defined in GraphLike using an immutable GraphNode implementation, based on a mutable version.
 * @author Mikko Hilpinen
 * @since 20.09.2026, v2.9
 */
object GraphNodeTest extends App
{
	// Sets up the test graph
	/*
					  +--3-> G
					  |
		A -1-> B -2-> C -3-> +
			   |             |
			   + <----4----> D -4-> E -5-> F
	 */
	private val a = {
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
	assert(c.allEdgesIterator.map { _.value }.toVector == Vector(3, 3, 4, 4, 5, 2, 4),
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
	
	// routesTo
	assert(b.routesTo(e).map { _.iterator.map { _.end.value }.mkString }.toSet == Set("DE", "CDE"))
	assert(b.routesTo(d).map { _.iterator.map { _.end.value }.mkString }.toSet == Set("D", "CD"))
	
	// routesToSelf
	assert(b.routesToSelf.iterator.map { _.iterator.map { _.end.value }.mkString }.toSet == Set("CDB", "DB"),
		b.routesToSelf.map { _.map { _.end.value }.mkString }.mkString(" & "))
	assert(d.routesToSelf.iterator.map { _.iterator.map { _.end.value }.mkString }.toSet == Set("BCD", "BD"),
		d.routesToSelf.map { _.map { _.end.value }.mkString }.mkString(" & "))
	
	// lookup
	assert(a.lookup.get("B", "C", "D", "E", "F").isDefined)
	assert(a.lookup.get("B", "C", "G").isDefined)
	assert(a.lookup.get("B", "C", "X").isEmpty)
	
	// traverseEdges
	assert(a.traverseEdges.get(1, 2, 3).exists { _.value == "D" })
	assert(a.traverseEdges.get(1, -1).isEmpty)
	
	// isDirectlyConnectedTo
	assert(b.isDirectlyConnectedTo(c))
	assert(c.isDirectlyConnectedTo { (n, _) => n.value == "G" })
	assert(!b.isDirectlyConnectedTo(e))
	
	// isConnectedTo
	assert(b.isConnectedTo(c))
	assert(b.isConnectedTo(e))
	assert(b.isConnectedTo { (n, _) => n.value == "G" })
	assert(!b.isConnectedTo(a))
	
	// edgeTo
	assert(b.edgesTo(c).exists { _.value == 2 })
	
	// shortestRoutesToOne
	assert(b.shortestRoutesToOne(e).get.routes.only.get.iterator.map { _.end.value }.mkString == "DE")
	assert(b.shortestRoutesToOne { (_, edges) => edges.isEmpty }.get.node.value == "G",
		b.shortestRoutesToOne { (_, edges) => edges.isEmpty })
	
	// cheapestRoutesToOne
	assert(b.cheapestRoutesToOne(e) { _.value }.get.routes.only.get.iterator.map { _.end.value }.mkString == "DE")
	assert(b.cheapestRoutesToOne(e) { _.value - 2 }.get.routes.only.get.iterator.map { _.end.value }.mkString == "CDE",
		b.cheapestRoutesToOne(e) { _.value - 2 }.get.routes.only.get.iterator.map { _.end.value }.mkString)
	assert(b.cheapestRoutesToOne(e) { _.value - 1 }.get.routes.size == 2)
	
	// shortestRoutesTo
	assert(b.shortestRoutesTo { (_, edges) => edges.isEmpty }.optimalSuccesses
		.iterator.flatMap { _.routes }.map { _.iterator.map { _.end.value }.mkString }.toSet == Set("CG", "DEF"),
		b.shortestRoutesTo { (_, edges) => edges.isEmpty }.optimalSuccesses
			.iterator.map { _.anyRoute.iterator.map { _.end.value }.mkString(">") }.mkString(" & "))
	
	// shortestRoutesToEach
	assert(b.shortestRoutesToEach(Pair(e, NodeTarget { (_, edges) => edges.value.isEmpty })).optimalSuccesses
		.iterator.flatMap { _.routes }.map { _.iterator.map { _.end.value }.mkString }.toSet == Set("DE", "CG"))
	
	// filterDirect
	{
		val b2 = b.filterDirect { _.value <= 2 }
		
		assert(b2.leavingEdges.only.get.value == 2)
		assert(b2("C", "D").leavingEdges.size == 2)
	}
	
	// filter
	{
		val b2 = b.filter { (_, edge) => edge.value <= 3 }
		
		assert(b2.leavingEdges.only.get.value == 2)
		assert(b2.get("C", "D").get.isEmpty)
	}
	
	// Mutable nodes
	{
		val m = MutableGraphNode.from(a)
		val nav = m.lookup
		
		assert(m.leavingEdges.only.get.end.value == "B")
		assert(nav.get("B", "C").get.leavingEdges.size == 2)
		
		val md = nav.get("B", "C", "D").get
		assert(md.leavingEdges.size == 2)
		
		m.disconnectFrom(md)
		
		assert(nav.get("B", "C", "D").isEmpty)
		assert(nav.get("B", "C").get.leavingEdges.size == 1)
		assert(nav.get("B").get.leavingEdges.size == 1)
	}
	
	/*
					  +--3-> G
					  |
		A -1-> B -2-> C -3-> +
			   |             |
			   + <----4----> D -4-> E -5-> F
	 */
	
	println("Success!")
}
