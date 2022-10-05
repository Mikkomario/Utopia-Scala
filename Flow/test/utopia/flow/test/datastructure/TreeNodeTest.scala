package utopia.flow.test.datastructure

import utopia.flow.collection.mutable.MutableTree
import utopia.flow.collection.template.TreeLike
import utopia.flow.operator.EqualsFunction
import utopia.flow.collection.CollectionExtensions._

/**
 *
 * @author Mikko Hilpinen
 * @since 5.4.2021, v
 */
object TreeNodeTest extends App
{
	implicit val equals: EqualsFunction[Any] = EqualsFunction.default
	
	private def basicCheck(tree: TreeLike[_, _]) =
	{
		assert(tree.children.size == 2)
		assert(!tree.isEmpty)
		assert(tree.size == 5)
		assert(tree.depth == 3)
	}
	
	println("Running TreeNodeTest")
	
	// Creates tree
	/*
        1 ->
            [2 ->
                [3 ->
                    4,
                 5],
             6]
     */
	val root = MutableTree(1)
	val bottomNode = MutableTree(4)
	root += MutableTree(2, MutableTree(3, bottomNode), MutableTree(5))
	val secondChild = MutableTree(6)
	root += secondChild
	
	// Performs tests on tree (not mutating)
	println(root)
	basicCheck(root)
	
	assert(root.nodesBelowIterator.map { _.nav }.toVector == Vector(2, 3, 4, 5, 6))
	assert(root.nodesBelow.map { _.nav }.toSet == Set(2, 3, 4, 5, 6))
	assert((root / 2 / 3).children.size == 1)
	// assert((root / 5).children.isEmpty)
	assert((root / 2).nodesBelow.map { _.nav }.toSet == Set(3, 4, 5))
	assert(root.leaves.map { _.nav } == Vector(4, 5, 6))
	assert(root.nodesBelow.contains(bottomNode))
	assert(root.containsNode(bottomNode))
	assert(root.containsNode(secondChild))
	assert(root.get(6).contains(secondChild))
	assert(root.nodesBelowIterator.find { _.nav == 4 }.contains(bottomNode))
	
	// Creates an immutable copy of the tree
	val copy = root.immutableCopy
	
	// Performs tests on immutable tree
	basicCheck(copy)
	
	val increased = copy :+ 7
	
	assert(increased.nodesBelowIterator.exists { _.nav == 7 })
	assert(increased.nodesBelowIterator.exists { _.nav == 4 })
	
	val decreased = copy - 2
	
	assert(decreased.size == 1)
	assert(decreased.children.exists { _.nav == 6 })
	
	assert(copy.findWithPath { _.nav == 4 }.contains(Vector(2, 3, 4)))
	assert(copy.findWithPath { _.nav == 5 }.contains(Vector(2, 5)))
	assert(copy.findWithPath { _.nav == 6 }.contains(Vector(6)))
	assert(copy.findWithPath { _.nav == 7 }.isEmpty)
	assert(copy.findWithPath { _.nav == 1 }.isEmpty)
	
	assert(copy.filterWithPaths { _.nav == 4 }.map { _.map { _.nav } } == Vector(Vector(2, 3, 4)))
	assert(copy.filterWithPaths { _.nav == 5 }.map { _.map { _.nav } } == Vector(Vector(2, 5)))
	assert(copy.filterWithPaths { _.nav > 3 }.containsAll(Vector(Vector(2, 3, 4), Vector(2, 5), Vector(6))))
	assert(copy.filterWithPaths { _.nav < 3 }.map { _.map { _.nav } } == Vector(Vector(2)))
	assert(copy.filterWithPaths { _.nav == 7 }.isEmpty)
	
	// Mutates mutable tree & tests
	root.removeChild(secondChild)
	assert(root.children.size == 1)
	
	root -= bottomNode
	assert(root.depth == 2)
	
	root.clear()
	assert(root.isEmpty)
	
	println("Success")
}
