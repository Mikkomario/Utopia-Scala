package utopia.flow.test

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, LocalDateTime, Month, Year}
import java.util.concurrent.TimeUnit

import utopia.flow.time.TimeExtensions._

import scala.concurrent.duration.FiniteDuration

/**
  * This test checks time number equality
  * @author Mikko Hilpinen
  * @since 5.6.2019, v1+
  */
object TimeNumberTest extends App
{
	assert(13.nanos == FiniteDuration(13, TimeUnit.NANOSECONDS))
	assert(13.millis == FiniteDuration(13, TimeUnit.MILLISECONDS))
	assert(13.seconds == FiniteDuration(13, TimeUnit.SECONDS))
	assert(13.512.seconds == FiniteDuration(13512, TimeUnit.MILLISECONDS))
	assert(13.minutes == FiniteDuration(13, TimeUnit.MINUTES))
	assert(1.5.minutes == FiniteDuration(90, TimeUnit.SECONDS))
	assert(13.hours == FiniteDuration(13, TimeUnit.HOURS))
	
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
	val weeksAtAug2019 = (Year.of(2019) + Month.AUGUST).weeks()
	assert(weeksAtAug2019.head.size == 4)
	assert(weeksAtAug2019.last.size == 6)
	// println(LocalDate.now.yearMonth.weeks().map { _.map { d => s"${d.getDayOfMonth} ${d.weekDay}" }.mkString(", ") }.mkString("\n"))
	
	assert((LocalDate.of(2020, 1, 1) + 2.weeks).isEqual(LocalDate.of(2020, 1, 15)))
	assert((LocalDate.of(2020, 1, 1) + 26.hours).isEqual(LocalDateTime.of(2020, 1, 2, 2, 0)))
	
	println("Success!")
}
