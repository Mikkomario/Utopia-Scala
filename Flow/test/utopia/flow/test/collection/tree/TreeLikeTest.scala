package utopia.flow.test.collection.tree

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.tree.ValueTree

/**
 * Tests [[utopia.flow.collection.template.tree.TreeLike]] using [[ValueTree]]
 * @author Mikko Hilpinen
 * @since 17.09.2026, v2.9
 */
object TreeLikeTest extends App
{
	// 1
	//  -> 2
	//      -> 3
	//  -> 4
	private val n2 = ValueTree(2).withChild(ValueTree(3))
	private val n4 = ValueTree(4).withoutChildren
	private val t1 = ValueTree[Int](1).withChildren(n2, n4)
	private val t2 = ValueTree[Int](1).withoutChildren
	
	assert(t1.children.size == 2)
	assert(t2.children.isEmpty)
	assert(t1.hasChildren)
	assert(!t2.hasChildren)
	assert(t1.nonEmpty)
	assert(t2.isEmpty)
	
	assert(t1.size == 3)
	assert(t2.size == 0)
	assert(t1.depth == 2)
	assert(t2.depth == 0)
	
	assert(t1.allNodesIterator.size == 4)
	assert(t2.allNodesIterator.size == 1)
	
	assert(t1.allNodesWithDepthIterator.find { _._2 == 2 }.get._1.value == 3)
	assert(t2.allNodesWithDepthIterator.only().get._1.value == 1)
	
	assert(t1.nodesBelowIterator.size == 3)
	assert(t2.nodesBelowIterator.isEmpty)
	assert(t1.nodesBelowIterator.contains(n2), t1.nodesBelowIterator.map { _.value }.mkString(", "))
	assert(t1.nodesBelowIterator.contains(n4), t1.nodesBelowIterator.map { _.value }.mkString(", "))
	
	assert(t1.topDownNodesIterator.map { _.value }.toVector == Vector(1, 2, 4, 3))
	assert(t2.topDownNodesIterator.map { _.value }.toVector == Vector(1))
	
	assert(t1.bottomToTopNodesIterator.map { _.value }.toVector == Vector(3, 2, 4, 1))
	assert(t1.leavesIterator.map { _.value }.toVector == Vector(3, 4))
	assert(t2.leavesIterator.only().get.value == 1)
	
	assert(t1.branchesIterator.map { _.map { _.value } }.toVector == Vector(Vector(1, 2, 3), Vector(1, 4)),
		t1.branchesIterator.map { b => s"[${b.map { _.value }.mkString(", ")}]" }.mkString(", "))
	assert(t1.allPathsIterator.map { _.map { _.value } }.toVector == Vector(Vector(1), Vector(1, 2), Vector(1, 2, 3), Vector(1, 4)))
	
	assert(t1.follow(Pair(2, 3)) { _.value == _ }.get.value == 3)
	
	assert(t1.pathTo(n2).get.map { _.value } == Vector(1, 2))
	assert(t1.pathTo(n4).get.map { _.value } == Vector(1, 4))
	
	assert(t1.findCommonParentOf(Pair(3, 4)) { _.value == _ }.get.value == 1)
	assert(t1.rootsWhereIterator { _.value >= 2 }.map { _.value }.toVector == Vector(2, 4))
	assert(t1.findWithPath { _.value >= 2 }.get.map { _.value } == Vector(1, 2))
	assert(t1.pathsToRootsWhereIterator { _.value >= 3 }.map { _.map { _.value } }.toVector ==
		Vector(Vector(1, 2, 3), Vector(1, 4)))
	
	println("Success!")
}
