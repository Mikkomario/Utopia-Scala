package utopia.flow.test.collection

import utopia.flow.collection.immutable.IntSet
import utopia.flow.collection.immutable.range.NumericSpan

/**
  * A test for the IntSet collection type
  * @author Mikko Hilpinen
  * @since 05.08.2024, v2.5
  */
object IntSetTest extends App
{
	private val set = IntSet(1,2,3,5,6,7,9,10,12)
	
	assert(set.toSeq == Vector(1,2,3,5,6,7,9,10,12))
	
	assert(set.ranges.size == 4)
	assert(set.ranges.head == NumericSpan(1,3))
	assert(set.ranges(1) == NumericSpan(5,7))
	assert(set.ranges(2) == NumericSpan(9,10))
	assert(set.ranges(3) == NumericSpan.singleValue(12))
	
	assert(set.contains(6))
	assert(!set.contains(11))
	
	println("Done!")
}
