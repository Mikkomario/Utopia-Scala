package utopia.flow.test

import java.time.{DayOfWeek, LocalDate}
import utopia.flow.generic.DataType
import utopia.flow.time.WeekDay.{Monday, Sunday, Thursday, Tuesday, Wednesday}
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.WeekDay

/**
  * Tests week days
  * @author Mikko Hilpinen
  * @since 29.9.2020, v1.9
  */
object WeekDayTest extends App
{
	DataType.setup()
	
	assert(WeekDay.iterate(Monday).take(7).toVector == WeekDay.values)
	assert(WeekDay.reverseIterate(Sunday).take(7).toVector == WeekDay.values.reverse)
	assert(Vector(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
		DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).map { d => d: WeekDay } == WeekDay.values)
	assert((1 to 7).map(WeekDay.forIndex).toVector == WeekDay.values)
	assert((1 to 7).map { DayOfWeek.of(_): WeekDay } == WeekDay.values)
	assert(Tuesday > Monday)
	assert(Tuesday < Wednesday)
	assert(Monday.previous == Sunday)
	assert(Sunday.next == Monday)
	assert(Monday.until(Tuesday) == 1.days)
	assert(Monday.until(Sunday) == 6.days)
	assert(Wednesday.until(Tuesday) == 6.days)
	assert(Thursday - Tuesday == 2.days)
	assert(Tuesday - Thursday == 5.days)
	
	val today = LocalDate.now()
	val currentWeekDay = WeekDay.current()
	
	assert(today.next(currentWeekDay) == today + 1.weeks)
	assert(today.next(currentWeekDay, includeSelf = true) == today)
	assert(today.next(currentWeekDay.next) == today + 1.days)
	assert(today.next(currentWeekDay.previous) == today + 6.days)
	assert(today.previous(currentWeekDay) == today - 1.weeks)
	assert(today.previous(currentWeekDay, includeSelf = true) == today)
	assert(today.previous(currentWeekDay.previous) == today - 1.days)
	assert(today.previous(currentWeekDay.next) == today - 6.days)
	
	println("Done")
}
