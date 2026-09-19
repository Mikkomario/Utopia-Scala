package utopia.flow.test.collection.tree

import utopia.flow.collection.immutable.tree.ValueTree
import utopia.flow.collection.mutable.tree.MutableValueTree
import utopia.flow.collection.CollectionExtensions._

/**
 * Tests MutableTreeLike and MutableValueTree
 * @author Mikko Hilpinen
 * @since 18.09.2026, v2.9
 */
object MutableTreeTest extends App
{
	// from
	private val t = MutableValueTree.from(ValueTree(1).withChild(ValueTree(2)))
	assert(t.value == 1)
	assert(t.children.only.get.value == 2)
	
	// +=
	t += ValueTree(3)
	assert(t.children.map { _.value } == Vector(2, 3))
	
	// += with navigation
	t(2) += ValueTree(4)
	assert(t.children.size == 2)
	assert(t(2).children.only.get.value == 4)
	
	// ++
	t ++= Vector(ValueTree(5), ValueTree(6))
	assert(t.children.map { _.value } == Vector(2, 3, 5, 6))
	
	// filterDirect
	t.filterDirect { _.value <= 3 }
	assert(t.children.map { _.value } == Vector(2, 3))
	assert(t.get(2).exists { _.hasChildren })
	
	// filter
	t.filter { _.value <= 2 }
	assert(t.children.only.get.value == 2)
	assert(t.children.only.get.isEmpty)
	
	// clear
	t.clear()
	assert(t.isEmpty)
	
	// removeDirect
	val n1 = MutableValueTree(2)
	t += n1
	t += ValueTree(3)
	t.removeDirect(n1)
	assert(t.children.only.get.value == 3)
	
	// -=
	val n2 = MutableValueTree(4)
	t(3) += n2
	t(3) += ValueTree(5)
	t -= n2
	assert(t.children.only.get.value == 3)
	assert(t.get(3).get.children.only.get.value == 5)
	
	println("Success!")
}
