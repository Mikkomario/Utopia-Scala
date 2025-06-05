package utopia.flow.test.time

import utopia.flow.time.DateRange
import utopia.flow.time.Month.{August, December, February, January, July, June, March, November}
import utopia.flow.time.TimeExtensions._

/**
 * Tests date ranges
 * @author Mikko Hilpinen
 * @since 11.1.2021, v1.9
 */
object DateRangeTest extends App
{
	val testYear = 2020.year
	val janToFebInclusive = DateRange.inclusive(testYear(January).firstDay, testYear(February).lastDay)
	
	assert(janToFebInclusive == DateRange.exclusive(testYear(January).firstDay, testYear(March).firstDay))
	
	assert(janToFebInclusive.reverse == DateRange.inclusive(testYear(February).lastDay, testYear(January).firstDay))
	assert(janToFebInclusive.reverse.reverse == janToFebInclusive)
	assert(janToFebInclusive.isChronological)
	assert(janToFebInclusive.reverse.nonChronological)
	
	assert(janToFebInclusive.contains(testYear(1)(1)))
	assert(janToFebInclusive.contains(testYear(2)(2)))
	assert(janToFebInclusive.contains(testYear(February).lastDay))
	assert(!janToFebInclusive.contains(testYear(3)(1)))
	
	assert(janToFebInclusive(0) == testYear(1)(1))
	assert(janToFebInclusive(2) == testYear(1)(3))
	assert(janToFebInclusive.reverse(0) == testYear(February).lastDay)
	assert(janToFebInclusive.reverse(2) == testYear(February).lastDay - 2)
	
	val summer = 1.of(June) until 1.of(August)
	val winter = 15.of(November) until 21.of(March)
	val fullYear = February(1) until February(1)
	
	assert(fullYear.spansFullYear)
	assert(summer.at(testYear).size == 1)
	assert(winter.at(testYear).size == 2)
	
	assert(summer.contains(3.of(July)))
	assert(summer.contains(testYear(June) / 15))
	assert(!summer.contains(1.of(August)))
	assert(!summer.contains(testYear(August) / 1))
	assert(winter.contains(1.of(December)))
	assert(winter.contains(3.of(January)))
	assert(winter.contains(testYear(December) / 3))
	assert(winter.contains(testYear(February) / 2))
	assert(!winter.contains(1.of(July)))
	assert(!winter.contains(testYear / 21.of(March)))
	
	val february = February(1) until March(1)
	val february2020 = testYear(february).head
	val febToMarchInclusive = DateRange.inclusive(testYear(February).firstDay, testYear(March).lastDay)
	
	assert(janToFebInclusive contains february2020)
	assert(febToMarchInclusive contains february2020)
	assert(janToFebInclusive.overlapWith(february2020).contains(february2020))
	assert(febToMarchInclusive.overlapWith(february2020).contains(february2020))
	assert(febToMarchInclusive.overlapWith(janToFebInclusive).contains(february2020))
	assert(!testYear(summer).head.overlapsWith(february2020))
	assert(testYear(summer).head.overlapWith(february2020).isEmpty)
	
	println(february2020)
	println(febToMarchInclusive)
	
	println("Success!")
}
