package utopia.flow.test.collection

import utopia.flow.collection.immutable.Graph
import utopia.flow.collection.CollectionExtensions._

/**
  * Tests the graph search algorithm
  * @author Mikko Hilpinen
  * @since 25.06.2024, v2.4
  */
object GraphSearchTest extends App
{
	/*
		The targeted graph looks like this:
		(X) symbols represent nodes
		[Y] symbols represent edges, where Y is edge traverse cost
		
		(1) -[1]- (2) -[8]- (3)
		 |         |
		[7]       [2]
		 |         |
		(4) -[3]- (5) -[4]- (6)
		 |         |
		[1]       [1]
		 |         |
		(7) -[1]- (8)
	 */
	val graph = Graph(Set(
		(1, 1, 2), (2, 8, 3),
		(1, 7, 4), (2, 2, 5),
		(4, 3, 5), (5, 4, 6),
		(4, 1, 7), (5, 1, 8),
		(7, 1, 8)
	), isTwoWayBound = true)
	
	// TEST 1   ------------------------
	
	// Searches for the cheapest path from (1) to (4)
	// Should eventually yield 2 routes:
	//      1) 1 -> 2 -> 5 -> 4,
	//      2) 1 -> 2 -> 5 -> 8 -> 7 -> 4
	// 1 -> 4 should become available early on as well
	val s1 = graph(1).searchForOne { _.value == 4 } { _.value }
	println("Search starts")
	println(s1.current)
	
	assert(s1.current.isEmpty)
	assert(s1.current.isPartial)
	assert(s1.current.mayBeSuboptimal)
	assert(s1.current.stages.size == 1, s1.current.stages)
	assert(s1.current.minFutureCost == 0)
	assert(s1.hasNext)
	
	// Step 1: Scans edges leaving from (1)
	// Expects (4) to become available as a result
	//         and (2) to become available as a temporary path
	println("Leaves from (1)")
	println(s1.next())
	
	assert(s1.current.foundResults)
	assert(s1.current.foundAllResults)
	assert(s1.current.mayBeSuboptimal)
	assert(s1.current.successes.only
		.exists { r => r.node.value == 4 && r.routes.only.exists { _.size == 1 } && r.cost == 7 && r.mayBeSuboptimal },
		s1.current.successes)
	assert(s1.current.temporaryStages.view.map { _.node.value }.toSet == Set(2, 4))
	assert(s1.current.minFutureCost == 1)
	assert(s1.hasNext)
	
	// Step 2: Scans edges leaving from (2)
	// Expects (3) and (5) to become available to travel to
	//         Also expects (2) to disappear from available stages
	println("Leaves from (2)")
	println(s1.next())
	
	assert(s1.current.foundResults)
	assert(s1.current.foundAllResults)
	assert(s1.current.mayBeSuboptimal)
	assert(s1.current.temporaryStages.view.map { _.node.value }.toSet == Set(3, 4, 5))
	assert(s1.current.successes.size == 1)
	assert(s1.current.minFutureCost == 3)
	assert(s1.hasNext)
	
	// Step 3: Scans edges leaving from (5)
	// Expects to discover (6) and (8), as well as a better route (of cost 4) for (4)
	println("Leaves from (5)")
	println(s1.next())
	
	assert(s1.current.minFutureCost == 4, s1.current.minFutureCost)
	assert(s1.current.mayBeSuboptimal)
	assert(s1.current.successes.only
		.exists { r => r.node.value == 4 && r.cost == 6 && r.routes.only.exists { _.size == 3 } })
	assert(s1.current.temporaryStages.view.map { _.node.value }.toSet == Set(3, 4, 6, 8))
	assert(s1.hasNext)
	
	// Step 4: Scans edges leaving from (8)
	// Expects to discover (7)
	println("Leaves from (8)")
	println(s1.next())
	
	assert(s1.current.minFutureCost == 5)
	assert(s1.current.temporaryStages.view.map { _.node.value }.toSet == Set(3, 4, 6, 7))
	assert(s1.hasNext)
	
	// Step 5: Scans edges leaving from (4) and (7)
	// Expects to discover the final route to (4)
	println("Leaves from (4) & (7)")
	println(s1.next())
	
	assert(s1.current.minFutureCost >= 6, s1.current.minFutureCost)
	assert(s1.current.isConfirmedAsOptimal)
	assert(s1.current.successes.exists { r => r.cost == 6 && r.routes.size == 2 })
	assert(!s1.hasNext)
	
	// TEST 2   ------------------------
	
	// Looks for even nodes starting from node (4):
	/*
		(1) -[1]- (2) -[8]- (3)
		 |         |
		[7]       [2]
		 |         |
		(4) -[3]- (5) -[4]- (6)
		 |         |
		[1]       [1]
		 |         |
		(7) -[1]- (8)
	 */
	// Expects to find the following routes (in order):
	//      0) 4 (cost 0)
	//      1) 4 -> 7 -> 8 (cost 2)
	//      2) 4 -> 7 -> 8 -> 5 & 4 -> 5 (cost 3)
	//      3.1) ... -> 5 -> 2 (cost 5)
	//      3.2) ... -> 5 -> 6 (cost 7)
	println("\n\nStarts test 2")
	val s2 = graph(4).search { _.value % 2 == 0 } { _.value }
	
	assert(s2.current.minFutureCost == 0, s2.current.minFutureCost)
	assert(s2.current.foundResults)
	assert(s2.current.mayBeSuboptimal)
	assert(s2.current.successes.only
		.exists { r => r.cost == 0 && r.node.value == 4 && r.routes.size == 1 && r.routes.head.isEmpty &&
			r.isConfirmedAsOptimal })
	assert(s2.hasNext)
	
	// Step 1: Leaves node (4) and discovers (1), (5) and (7)
	println(s2.next())
	
	assert(s2.current.temporaryStages.view.map { _.node.value }.toSet == Set(1, 5, 7))
	assert(s2.current.minFutureCost == 1)
	assert(s2.current.successes.size == 1)
	assert(s2.hasNext)
	
	// Step 2: Leaves node (7) and discovers (8), which is a result
	println(s2.next())
	
	assert(s2.current.temporaryStages.view.map { _.node.value }.toSet == Set(1, 5, 8))
	assert(s2.current.minFutureCost == 2)
	assert(s2.current.successes.size == 2)
	assert(s2.current.successes.forall { _.isConfirmedAsOptimal })
	assert(s2.hasNext)
	
	// Step 3: Leaves node (8)
	println(s2.next())
	
	assert(s2.current.temporaryStages.view.map { _.node.value }.toSet == Set(1, 5))
	assert(s2.current.minFutureCost == 3)
	assert(s2.current.successes.size == 2)
	assert(s2.hasNext)
	
	// Step 4: Leaves node (5), discovers (2) and (6), which are results
	println(s2.next())
	
	assert(s2.current.temporaryStages.view.map { _.node.value }.toSet == Set(1, 2, 6))
	assert(s2.current.minFutureCost == 5)
	assert(s2.current.successes.size == 4)
	assert(s2.current.successes.count { _.mayBeSuboptimal } == 1)
	assert(s2.current.isPartial)
	assert(s2.current.mayBeSuboptimal)
	assert(s2.hasNext)
	
	// Step 5: Leaves node (2), discovers (3)
	println(s2.next())
	
	assert(s2.current.temporaryStages.view.map { _.node.value }.toSet == Set(1, 3, 6))
	assert(s2.current.minFutureCost == 6)
	assert(s2.current.successes.size == 4)
	assert(s2.hasNext)
	
	// Step 6: Leaves node (1)
	println(s2.next())
	
	assert(s2.current.temporaryStages.view.map { _.node.value }.toSet == Set(3, 6))
	assert(s2.current.minFutureCost == 7)
	assert(s2.current.successes.size == 4)
	assert(s2.current.successes.forall { _.isConfirmedAsOptimal })
	assert(s2.hasNext)
	
	// Step 7: Leaves node (6)
	println(s2.next())
	
	assert(s2.current.temporaryStages.view.map { _.node.value }.toSet == Set(3))
	assert(s2.current.minFutureCost == 13)
	assert(s2.current.isPartial)
	assert(s2.current.mayBeSuboptimal)
	assert(s2.hasNext)
	
	// Step 8 (final): Leaves node (3)
	println(s2.next())
	
	assert(s2.current.temporaryStages.isEmpty)
	assert(s2.current.foundAllResults)
	assert(s2.current.isConfirmedAsOptimal)
	assert(s2.current.successes.size == 4)
	assert(!s2.hasNext)
	
	// TEST 3   ------------------------
	
	// Starting from (1), looks for any route to a node larger than (2),
	// which costs less than 7
	/*
		(1) -[1]- (2) -[8]- (3)
		 |         |
		[7]       [2]
		 |         |
		(4) -[3]- (5) -[4]- (6)
		 |         |
		[1]       [1]
		 |         |
		(7) -[1]- (8)
	 */
	// Should identify 1 -> 2 -> 5 (cost 3)
	// Having not visited (4), (3) or (5)
	// Tests exclusive search first, then the inclusive version
	println("\n\nTest 3")
	
	val s3 = graph(1).searchForOne { _.value > 2 } { _.value }
	
	assert(s3.current.isEmpty)
	assert(s3.current.temporaryStages.view.map { _.node.value }.toSet == Set(1))
	assert(s3.current.minFutureCost == 0)
	assert(s3.hasNext)
	
	// Step 1: Leaves (1), discovers (4) and (2), where 4 is considered a result
	println(s3.next())
	
	assert(s3.current.foundResults)
	assert(s3.current.successes.size == 1)
	assert(s3.current.foundAllResults)
	assert(s3.current.successes.only
		.exists { r => r.node.value == 4 && r.cost == 7 && r.routes.size == 1 && r.routes.head.size == 1 &&
			r.mayBeSuboptimal })
	assert(s3.current.minFutureCost == 1)
	assert(s3.current.mayBeSuboptimal)
	assert(s3.current.temporaryStages.view.map { _.node.value }.toSet == Set(2, 4))
	assert(s3.hasNext)
	
	// Step 2: Leaves (2), discovers (3) and (5)
	// (5) Should be counted the new result with cost 3 (optimal), (4) should no longer be considered a result
	println(s3.next())
	
	assert(s3.current.foundResults)
	assert(s3.current.foundAllResults)
	assert(s3.current.successes.only.exists { r =>
		r.node.value == 5 && r.cost == 3 && r.routes.size == 1 && r.routes.head.size == 2 && r.isDestination &&
			r.isConfirmedAsOptimal
	})
	assert(s3.current.isConfirmedAsOptimal)
	assert(s3.current.temporaryStages.view.map { _.node.value }.toSet == Set(3, 4, 5))
	assert(!s3.hasNext)
	
	// Tests using search with findOneCheaperThan
	println("\nTest 3 v2")
	val s3v2 = graph(1).search { _.value > 2 } { _.value }
	
	assert(s3v2.findOneCheaperThan(7).exists { r =>
		r.node.value == 5 && r.cost == 3 && r.isConfirmedAsOptimal
	})
	println(s3v2.current)
	assert(s3v2.current.temporaryStages.view.map { _.node.value }.toSet == Set(3, 4, 5))
	assert(s3v2.current.foundResults)
	assert(s3v2.current.isPartial)
	assert(s3v2.current.successes.size == 3)
	assert(s3v2.hasNext)
	
	println("Done!")
}
