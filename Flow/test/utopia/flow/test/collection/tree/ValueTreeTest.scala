package utopia.flow.test.collection.tree

import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.tree.ValueTree
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.util.EitherExtensions._

/**
 * Tests the template and immutable ValueTree implementations / functions.
 * @author Mikko Hilpinen
 * @since 18.09.2026, v2.9
 */
object ValueTreeTest extends App
{
	// 1. Tests the functions defined in the template class using the immutable version
	// 1 -> 2 -> 3 -> 4
	//                  -> 5
	//                  -> 6
	//   -> 7
	//        -> 8
	//        -> 9
	private val t1 = ValueTree(1).withChildren(
		ValueTree(2).withChild(ValueTree(3).withChild(ValueTree(4).withChildren(
			ValueTree(5),
			ValueTree(6)))),
		ValueTree(7).withChildren(
			ValueTree(8),
			ValueTree(9))
	)
	
	assert(t1.valuesIterator.toVector == Vector(1, 2, 3, 4, 5, 6, 7, 8, 9))
	
	assert(t1.containsDirectValue(2))
	assert(t1.containsDirectValue(7))
	assert(!t1.containsDirectValue(9))
	assert(!t1.containsDirectValue(10))
	assert(!t1.containsDirectValue(3))
	
	assert(t1.containsValue(9))
	assert(t1.containsValue(3))
	assert(!t1.containsValue(10))
	assert(t1.containsValue(2))
	
	assert(t1.commonParentOfValues(Pair(2, 7)).get.value == 1)
	assert(t1.commonParentOfValues(Pair(5, 6)).get.value == 4)
	assert(t1.commonParentOfValues(Pair(4, 6)).get.value == 3, t1.commonParentOfValues(Pair(4, 6)).get)
	assert(t1.commonParentOfValues(Pair(8, 9)).get.value == 7)
	assert(t1.commonParentOfValues(Pair(2, 8)).get.value == 1)
	assert(t1.commonParentOfValues(Pair(6, 9)).get.value == 1)
	assert(t1.commonParentOfValues(Pair(-1, 9)).isEmpty)
	
	assert(t1.commonParentOfValues(Vector(3, 4, 6)).get.value == 2)
	assert(t1.commonParentOfValues(Vector(5, 6, 7)).get.value == 1)
	assert(t1.commonParentOfValues(Vector(4, 5, 6)).get.value == 3)
	
	assert(t1.pathToValue(3).get.map { _.value } == Vector(1, 2, 3),
		t1.pathToValue(3).get.iterator.map { _.value }.mkString(", "))
	assert(t1.pathToValue(9).get.map { _.value } == Vector(1, 7, 9))
	assert(t1.pathToValue(-1).isEmpty)
	assert(t1.pathToValue(1).get.map { _.value } == Vector(1))
	assert(t1.pathToValue(10).isEmpty)
	assert(t1.pathToValue(6).get.map { _.value } == Vector(1, 2, 3, 4, 6))
	
	// Tests tree navigation
	private val n1 = t1.navigate
	
	assert(n1.get(2, 3, 4).get.value == 4)
	assert(n1.get(2, 3, 4, 5).get.value == 5)
	assert(n1.get(2, 3, 4, 5, 6).isEmpty)
	assert(n1.get(7, 9).get.value == 9)
	assert(n1.get(10).isEmpty)
	
	assert(n1(2, 3, 4).value == 4)
	assert(n1(10, 11, 12).value == 12)
	
	// Tests functions defined in FilterableTreeLike
	
	// 1 -> 2 -> 3
	//   -> 4
	private val t2 = ValueTree(1).withChildren(
		ValueTree(2).withChild(ValueTree(3)),
		ValueTree(4))
	
	// withoutChildren
	{
		val t = t2.withoutChildren
		assert(t.value == 1)
		assert(t.children.isEmpty)
	}
	
	// filterDirect
	{
		val t = t2.filterDirect { _.value <= 2 }
		assert(t.value == 1)
		assert(t.children.size == 1)
		assert(t.get(4).isEmpty)
		assert(t.get(2).exists { _.hasChildren })
	}
	
	// filter
	{
		val t = t2.filter { _.value <= 2 }
		assert(t.value == 1)
		assert(t.children.size == 1)
		assert(t.get(4).isEmpty)
		assert(t.get(2).exists { _.isEmpty })
	}
	
	// -
	{
		val n1 = ValueTree(2).withoutChildren
		val n2 = ValueTree(1).withChild(n1)
		val t = ValueTree(3).withChildren(n2, ValueTree(4))
		
		val t2 = t - n2
		assert(t2.value == 3)
		assert(t2.children.only.get.value == 4)
		
		val t3 = t - n1
		assert(t3.value == 3)
		assert(t3.children.size == 2)
		assert(t3.get(1).exists { _.isEmpty })
	}
	
	// withoutDirect
	{
		val n1 = ValueTree(2).withoutChildren
		val n2 = ValueTree(1).withChild(n1)
		val t = ValueTree(3).withChildren(n2, ValueTree(4))
		
		val t2 = t.withoutDirect(n2)
		assert(t2.value == 3)
		assert(t2.children.only.get.value == 4)
		
		assert(t.withoutDirect(n1) == t)
	}
	
	
	// Tests immutable ValueTree functions
	
	// withValue
	{
		val t = t2.withValue(10)
		assert(t.value == 10)
		assert(t.children.size == 2)
		assert(t.get(4).isDefined)
	}
	
	// ++
	{
		val t = t2 ++ Pair(ValueTree(5).withoutChildren, ValueTree(6).withChild(ValueTree(7)))
		assert(t.value == 1)
		assert(t.children.size == 4)
		assert(t.get(4).isDefined)
		assert(t.get(5).isDefined)
		assert(t.get(6).exists { _.children.only.get.value == 7 })
	}
	
	// withChildren
	{
		val t = t2.withChildren(Pair(
			ValueTree(5).withoutChildren,
			ValueTree(6).withChild(ValueTree(7))
		))
		assert(t.value == 1)
		assert(t.children.size == 2)
		assert(t.get(4).isEmpty)
		assert(t.get(5).isDefined)
		assert(t.get(6).exists { _.children.only.get.value == 7 })
	}
	
	// mapLocalValue
	{
		val t = t2.mapLocalValue { _ + 1 }
		assert(t.value == 2)
		assert(t.children.size == 2)
		assert(t.get(2).isDefined)
		assert(t.get(3).isEmpty)
		assert(t.children.map { _.value } == Pair(2, 4))
	}
	
	// mapDirectValues
	{
		val t = t2.mapDirectValues { _ + 1 }
		assert(t.value == 1)
		assert(t.children.size == 2)
		assert(t.children.map { _.value } == Pair(3, 5))
		assert(t.get(3, 3).exists { _.value == 3 })
	}
	
	// mapDirect
	{
		val t = t2.mapDirect { _ :+ ValueTree(10) }
		assert(t.value == 1)
		assert(t.children.size == 2)
		assert(t.get(4).isDefined)
		assert(t.get(5).isEmpty)
		assert(t.get(10).isEmpty)
		assert(t.get(2).exists { _.valuesIterator.toVector == Vector(2, 3, 10) })
		assert(t.get(4).exists { _.children.only.get.value == 10 }, t.get(4))
	}
	
	// replaceDirectValue
	{
		val t = t2.replaceDirectValue(4, ValueTree(10)).toOption.get
		assert(t.value == 1)
		assert(t.children.size == 2, t.children)
		assert(t.get(4).isEmpty)
		assert(t.get(2).isDefined)
		assert(t.get(10).exists { _.isEmpty })
		
		assert(t2.replaceDirectValue(10, { throw new IllegalStateException("Unreachable") }).leftOption.contains(t2))
	}
	
	// mapRootsWhere
	{
		val t3 = t2 :+ ValueTree(5).withChild(ValueTree(6))
		
		assert(t3.children.size == 3)
		
		val t4 = t3.mapRootsWhere { _.value >= 3 } { _.mapLocalValue { -_ } }
		assert(t4.value == 1)
		assert(t4.children.size == 3)
		assert(t4.get(2).exists { _.children.only.get.value == -3 })
		assert(t4.get(-4).isDefined)
		assert(t4.get(-5).exists { _.children.only.get.value == 6 })
		assert(t4.get(4).isEmpty)
		assert(t4.get(5).isEmpty)
	}
	
	// mapFirstWhere
	{
		val t = t1.mapFirstWhere { _.value >= 3 } { _.mapLocalValue { -_ } }.toOption.get
		assert(t.value == 1)
		assert(t.children.size == 2)
		assert(t.get(2, -3).exists { _.children.only.get.value == 4 })
		assert(t.get(7).exists { _.valuesBelowIterator.toVector == Vector(8, 9) })
		
		assert(t2.mapFirstWhere { _.value < 0 } { _ => throw new IllegalStateException("Unreachable") }
			.leftOption.contains(t2))
	}
	
	// replaceDirectBranch
	{
		val t = t2.replaceDirectBranch(ValueTree(2).withChild(ValueTree(10))).toOption.get
		assert(t.value == 1)
		assert(t.children.size == 2)
		assert(t.get(2).exists { _.children.only.get.value == 10 })
		assert(t.get(4).exists { _.children.isEmpty })
		
		assert(t2.replaceDirectBranch(ValueTree(3).withChild(ValueTree(10).withoutChildren)).leftOption.contains(t2))
	}
	
	// replaceBranch
	{
		val t3 = t2.replaceBranch(ValueTree(2).withChild(ValueTree(10))).toOption.get
		assert(t3.value == 1)
		assert(t3.children.size == 2)
		assert(t3.get(2).exists { _.children.only.get.value == 10 })
		assert(t3.get(4).exists { _.children.isEmpty })
		
		val t4 = t2.replaceBranch(ValueTree(3).withChild(ValueTree(10).withoutChildren)).toOption.get
		assert(t4.value == 1)
		assert(t4.children.size == 2)
		assert(t4.get(2, 3).exists { _.children.only.get.value == 10 })
		assert(t4.get(4).exists { _.children.isEmpty })
	}
	
	// mergeDirectBranch
	{
		val t = t1.mergeDirectBranch(ValueTree(7).withChildren(
			ValueTree(9).withChild(ValueTree(-9)),
			ValueTree(10).withChild(ValueTree(11))
		)).toOption.get
		assert(t.value == 1)
		assert(t.children.size == 2)
		assert(t.get(2).exists { _.valuesBelowIterator.toVector == Vector(3, 4, 5, 6) })
		
		val merged = t.get(7).get
		assert(merged.children.size == 3)
		assert(merged.children.map { _.value } == Vector(8, 9, 10))
		assert(merged.get(9).exists { _.children.only.get.value == -9 })
		assert(merged.get(10).exists { _.children.only.get.value == 11 })
		
		val r2 = t1.mergeDirectBranch(ValueTree(4).withChildren(
			ValueTree(5).withChild(ValueTree(-5)),
			ValueTree(10).withChild(ValueTree(11)))
		)
		assert(r2.leftOption.contains(t1), r2)
	}
	
	// mergeBranch
	{
		val t3 = t1.mergeBranch(ValueTree(7).withChildren(
			ValueTree(9).withChild(ValueTree(-9)),
			ValueTree(10).withChild(ValueTree(11))
		)).toOption.get
		assert(t3.value == 1)
		assert(t3.children.size == 2)
		assert(t3.get(2).exists { _.valuesBelowIterator.toVector == Vector(3, 4, 5, 6) })
		
		val merged1 = t3.get(7).get
		assert(merged1.children.size == 3)
		assert(merged1.children.map { _.value } == Vector(8, 9, 10))
		assert(merged1.get(9).exists { _.children.only.get.value == -9 })
		assert(merged1.get(10).exists { _.children.only.get.value == 11 })
		
		val t4 = t1.mergeBranch(ValueTree(4).withChildren(
			ValueTree(5).withChild(ValueTree(-5)),
			ValueTree(10).withChild(ValueTree(11)))
		).toOption.get
		assert(t4.value == 1)
		assert(t4.children.size == 2)
		assert(t4.get(7).exists { _.valuesBelowIterator.toVector == Vector(8, 9) })
		
		val merged2 = t4.get(2, 3, 4).get
		assert(merged2.children.size == 3)
		assert(merged2.children.map { _.value } == Vector(5, 6, 10))
		assert(merged2.get(5).exists { _.children.only.get.value == -5 })
		assert(merged2.get(10).exists { _.children.only.get.value == 11 })
	}
	
	// joinBranches
	{
		val t = t2.joinBranches(Vector(
			ValueTree(2).withChild(ValueTree(3).withChild(ValueTree(-3))),
			ValueTree(4).withChild(ValueTree(-4)),
			ValueTree(-1).withChild(ValueTree(10))
		))
		
		assert(t.value == 1)
		assert(t.children.size == 3)
		assert(t.children.map { _.value } == Vector(2, 4, -1))
		assert(t.get(2).exists { _.children.only.get.children.only.get.value == -3 })
		assert(t.get(4).exists { _.children.only.get.value == -4 })
		assert(t.get(-1).exists { _.children.only.get.value == 10 })
	}
	
	// Tests immutable value tree mutators
	private val m1 = t2.mutate
	
	// included
	{
		val t3 = m1(10).included
		assert(t3.value == 1, t3)
		assert(t3.children.size == 3)
		assert(t3.children.map { _.value } == Vector(2, 4, 10))
		assert(t3.get(2) == t2.get(2))
		assert(t3.get(4) == t2.get(4))
		assert(t3.get(10).exists { _.isEmpty })
		
		val t4 = m1(10, 11).included
		assert(t4.value == 1, t4)
		assert(t4.children.size == 3)
		assert(t4.children.map { _.value } == Vector(2, 4, 10))
		assert(t4.get(2) == t2.get(2))
		assert(t4.get(4) == t2.get(4))
		assert(t4.get(10).exists { _.children.only.get.value == 11 })
		
		val t5 = m1(2, 10).included
		assert(t5.value == 1)
		assert(t5.children.size == 2)
		assert(t5.children.map { _.value } == Vector(2, 4))
		assert(t5.get(4) == t2.get(4))
		assert(t5.get(2).exists { _.valuesBelowIterator.toVector == Vector(3, 10) })
		
		assert(m1(2, 3).included == t2)
	}
	
	// excluded
	{
		val t3 = m1(2, 3).excluded
		assert(t3.value == 1)
		assert(t3.children.size == 2, t3)
		assert(t3.children.map { _.value } == Vector(2, 4))
		assert(t3.get(4) == t2.get(4))
		assert(t3.get(2).exists { _.isEmpty }, t3)
		
		val t4 = m1(2).excluded
		assert(t4.value == 1)
		assert(t4.children.only.get.value == 4)
		
		assert(m1(10).excluded == t2)
	}
	
	// mapped
	{
		val t3 = m1(2, 3).mapped { _ :+ ValueTree(10) }
		assert(t3.value == 1)
		assert(t3.children.size == 2)
		assert(t3.children.map { _.value } == Vector(2, 4))
		assert(t3.get(4) == t2.get(4))
		assert(t3.get(2, 3, 10).isDefined)
		
		val t4 = m1(10).mapped { _.mapLocalValue { -_ } }
		assert(t4.value == 1)
		assert(t4.children.map { _.value } == Vector(2, 4, -10))
	}
	
	// replacedWith
	{
		val t3 = m1(2).replacedWith(ValueTree(10))
		assert(t3.value == 1)
		assert(t3.children.map { _.value } == Vector(10, 4))
		assert(t3.get(10).exists { _.isEmpty })
		
		val t4 = m1(10).replacedWith(ValueTree(-10).withChild(ValueTree(-11)))
		assert(t4.value == 1)
		assert(t4.children.map { _.value } == Vector(2, 4, -10))
		assert(t4.get(-10).exists { _.children.only.get.value == -11 })
	}
	
	private val m2 = t1.mutate
	
	// Treating mutator like a tree
	{
		val t3 = m1(2) :+ ValueTree(10)
		assert(t3.value == 1)
		assert(t3.children.map { _.value } == Vector(2, 4))
		assert(t3.get(2).exists { _.children.map { _.value } == Vector(3, 10) })
		
		val t4 = m2(7).mapFirstWhere { _.value > 0 } { _.mapLocalValue { -_ } }.toOption.get
		assert(t4.get(2) == t1.get(2))
		assert(t4.get(7).exists { _.children.map { _.value } == Vector(-8, 9) })
	}
	
	println("Success!")
}
