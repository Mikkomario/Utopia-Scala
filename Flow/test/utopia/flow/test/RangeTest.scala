package utopia.flow.test

import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.range.{HasEnds, HasOrderedEnds}

/**
 * Tests for HasEnds etc.
 * @author Mikko Hilpinen
 * @since 27.10.2025, v2.7
 */
object RangeTest extends App
{
	private val range1 = 0 until 3
	private val ends1 = HasOrderedEnds.from(range1)
	
	assert(!range1.isInclusive)
	assert(ends1 == HasOrderedEnds.exclusive(0, 3), ends1)
	assert(ends1.inclusiveEndsOption.contains(Pair(0, 2)))
}
