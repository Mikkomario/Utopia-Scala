package utopia.flow.test

import utopia.flow.time.DateRange

import java.time.Month._
import utopia.flow.time.TimeExtensions._

/**
  * Tests date ranges
  * @author Mikko Hilpinen
  * @since 11.1.2021, v1.9
  */
object DateRangeTest extends App
{
	val testYear = 2020.year
	val janToFebInclusive = DateRange.inclusive(testYear.january.firstDay, testYear.february.lastDay)
	
	assert(janToFebInclusive == DateRange.exclusive(testYear.january.firstDay, testYear.march.firstDay))
	
	assert(janToFebInclusive.reverse == DateRange.inclusive(testYear.february.lastDay, testYear.january.firstDay))
	assert(janToFebInclusive.reverse.reverse == janToFebInclusive)
	assert(janToFebInclusive.isChronological)
	assert(janToFebInclusive.reverse.nonChronological)
	
	assert(janToFebInclusive.contains(testYear/1/1))
	assert(janToFebInclusive.contains(testYear/2/2))
	assert(janToFebInclusive.contains(testYear.february.lastDay))
	assert(!janToFebInclusive.contains(testYear/3/1))
	
	assert(janToFebInclusive(0) == testYear/1/1)
	assert(janToFebInclusive(2) == testYear/1/3)
	assert(janToFebInclusive.reverse(0) == testYear.february.lastDay)
	assert(janToFebInclusive.reverse(2) == testYear.february.lastDay - 2)
	
	val summer = 1.of(JUNE) until 1.of(AUGUST)
	val winter = 15.of(NOVEMBER) until 21.of(MARCH)
	val fullYear = FEBRUARY(1) until FEBRUARY(1)
	
	assert(fullYear.spansFullYear)
	assert(summer.at(testYear).size == 1)
	assert(winter.at(testYear).size == 2)
	
	assert(summer.contains(3.of(JULY)))
	assert(summer.contains(testYear.june/15))
	assert(!summer.contains(1.of(AUGUST)))
	assert(!summer.contains(testYear.august/1))
	assert(winter.contains(1.of(DECEMBER)))
	assert(winter.contains(3.of(JANUARY)))
	assert(winter.contains(testYear.december/3))
	assert(winter.contains(testYear.february/2))
	assert(!winter.contains(1.of(JULY)))
	assert(!winter.contains(testYear / 21.of(MARCH)))
	
	val february = FEBRUARY(1) until MARCH(1)
	val february2020 = testYear(february).head
	val febToMarchInclusive = DateRange.inclusive(testYear.february.firstDay, testYear.march.lastDay)
	
	assert(janToFebInclusive contains february2020)
	assert(febToMarchInclusive contains february2020)
	assert(janToFebInclusive.overlapWith(february2020).contains(february2020))
	assert(febToMarchInclusive.overlapWith(february2020).contains(february2020))
	assert(febToMarchInclusive.overlapWith(janToFebInclusive).contains(february2020))
	assert(!testYear(summer).head.overlapsWith(february2020))
	assert(testYear(summer).head.overlapWith(february2020).isEmpty)
	
	println("Success!")
}
