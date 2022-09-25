package utopia.flow.test.datastructure

import utopia.flow.collection.mutable.GraphNode

/**
 * This test tests the features implemented in graph, graphNode and graphEdge
 */
object GraphTest extends App
{
	println("Running Graph Test")
	
	type IntNode = GraphNode[Int, Int]
	
	// Creates a test graph first
	val node1 = new IntNode(1)
	val node2 = new IntNode(2)
	val node3 = new IntNode(3)
	val node4 = new IntNode(4)
	val node5 = new IntNode(5)
	
	// Connects the nodes (1 -> 2 -> 3 -> 5, 1 -> 4 -> 5) using weighted edges
	node1.connect(node2, 1)
	node2.connect(node3, 5)
	node3.connect(node5, 1)
	
	node1.connect(node4, 4)
	node4.connect(node5, 4)
	
	assert(node1.allNodeContent == Set(1, 2, 3, 4, 5))
	assert(node2.allNodeContent == Set(2, 3, 5))
	
	// Makes sure there are correct number of edges in nodes
	assert(node1.leavingEdges.size == 2)
	assert(node2.leavingEdges.size == 1)
	assert(node5.leavingEdges.isEmpty)
	assert(node1.endNodes.contains(node2))
	assert(node1.isDirectlyConnectedTo(node2))
	assert(!node1.isDirectlyConnectedTo(node5))
	assert(node1.allNodes.size == 5)
	assert(node2.allNodes.size == 3)
	
	// Finds the routes from 1 to 5. Should have 2 routes
	assert(node1.routesTo(node5).size == 2)
	assert(node1.isConnectedTo(node5))
	
	// Tests node traversing
	assert(node1 / Vector(1, 5, 1) == Set(node5))
	
	// The shortest route should be 1 -> 4 -> 5
	assert(node1.shortestRouteTo(node5).get.size == 2)
	// The cheapest route (weights considered) should be 1 -> 2 -> 3 -> 5
	println()
	node1.routesTo(node5).foreach { route =>
		println(s"- ${route.map { e => s"to ${e.end.value} (${e.value})" }.mkString(" => ")} (${ route.foldLeft(0) { _ + _.value } })")
	}
	println()
	val cheapestRoutes = node1.cheapestRoutesTo(node5) { _.value }
	cheapestRoutes._1.foreach { r => println(r.map { e => s"to ${e.end.value} (${e.value})" }.mkString(" => ")) }
	println(cheapestRoutes._2)
	/*
	val cheapestRoute = node1.cheapestRouteTo(node5) { edge => edge.content }
	assert(cheapestRoute.get.size == 3, cheapestRoute.get.map { _.end.content })
	
	// After disconnecting node 5 from node 4. Only one route should remain
	node4.disconnectDirect(node5)
	assert(node1.routesTo(node5).size == 1)
	assert(!node1.endNodes.contains(node5))
	
	// Tests circular node foreach
	val cNode1 = new IntNode(1)
	val cNode2 = new IntNode(2)
	val cNode3 = new IntNode(3)
	val cNode4 = new IntNode(4)
	
	cNode1.connect(cNode2, 1)
	cNode2.connect(cNode3, 1)
	cNode3.connect(cNode4, 1)
	cNode4.connect(cNode1, 1)
	cNode1.connect(cNode4, 1)
	cNode2.connect(cNode1, 1)
	cNode3.connect(cNode2, 1)
	cNode4.connect(cNode3, 1)
	cNode1.connect(cNode1, 1)
	cNode2.connect(cNode4, 1)
	
	assert(cNode1.allNodeContent == Set(1, 2, 3, 4))
	assert(cNode3.allNodeContent == Set(1, 2, 3, 4))
	
	// Tests the shortest routes -function
	val n1 = new IntNode(1)
	val n2 = new IntNode(2)
	val n3 = new IntNode(3)
	val n4 = new IntNode(4)
	
	n1.connect(n2, 1)
	n1.connect(n3, 1)
	
	n2.connect(n4, 2)
	n3.connect(n4, 3)
	
	val shorts = n1.shortestRoutesTo(n4)
	val cheaps = n1.cheapestRoutesTo(n4) { _.content }._1
	
	assert(shorts.size == 2)
	assert(cheaps.size == 1)
	assert(cheaps.head.size == 2)
	
	print("Success")
	 */
}
