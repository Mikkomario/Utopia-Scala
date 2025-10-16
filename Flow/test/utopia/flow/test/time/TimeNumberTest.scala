package utopia.flow.test.time

import utopia.flow.time.Month.March
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.TimeUnit.{Hour, MilliSecond, Minute, NanoSecond, Second}
import utopia.flow.time.WeekDays.MondayToSunday
import utopia.flow.time.{Duration, WeekDays, Year}

import java.time._
import java.time.format.DateTimeFormatter

/**
 * This test checks time number equality
 * @author Mikko Hilpinen
 * @since 5.6.2019, v1+
 */
object TimeNumberTest extends App
{
	implicit val weekdays: WeekDays = MondayToSunday
	
	assert(13.nanos == Duration(13, NanoSecond))
	assert(13.millis == Duration(13, MilliSecond))
	assert(13.seconds == Duration(13, Second))
	assert(13.512.seconds == Duration(13512, MilliSecond))
	assert(13.minutes == Duration(13, Minute))
	assert(1.5.minutes == Duration(90, Second))
	assert(13.hours == Duration(13, Hour))
	
	println(13.5231.nanos.description)
	println(13.5231.millis.description)
	println(324.5231.millis.description)
	println(13.5231.seconds.description)
	println(13.5231.minutes.description)
	println(13.5231.hours.description)
	
	// Makes sure extended instant support works (natural instant would throw on both)
	println(Instant.now() + 3.months)
	println(Instant.now().toStringWith(DateTimeFormatter.ISO_DATE_TIME))
	
	// Also tests some other time extensions
	val weeksAtMar2023 = (Year(2023) + March).weeks
	assert(weeksAtMar2023.head.size == 5, weeksAtMar2023.head)
	assert(weeksAtMar2023.last.size == 5, weeksAtMar2023.last)
	// println(LocalDate.now.yearMonth.weeks().map { _.map { d => s"${d.getDayOfMonth} ${d.weekDay}" }.mkString(", ") }.mkString("\n"))
	
	assert((LocalDate.of(2020, 1, 1) + 2.weeks).isEqual(LocalDate.of(2020, 1, 15)))
	assert((LocalDate.of(2020, 1, 1) + 26.hours).isEqual(LocalDateTime.of(2020, 1, 2, 2, 0)))
	
	println("Success!")
}
