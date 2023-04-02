package utopia.flow.time

import utopia.flow.time.WeekDay.{Friday, Monday, Saturday, Sunday, Thursday, Tuesday, Wednesday}

/**
  * Different areas consider different week day as the first day of the week, hence this open enumeration.
  * @author Mikko Hilpinen
  * @since 2.4.2023, v2.1
  */
trait WeekDays extends Ordering[WeekDay]
{
	// ABSTRACT -----------------------
	
	/**
	  * @return Week days. Ordered.
	  */
	def values: Vector[WeekDay]
	
	/**
	  * @param day A weekday
	  * @return A 0-based index of that day in this calendar [0,6]
	  */
	def indexOf(day: WeekDay): Int
	
	
	// COMPUTED -----------------------
	
	/**
	  * @return The first day of the week
	  */
	def first = values.head
	/**
	  * @return The last day of the week
	  */
	def last = values.last
	
	
	// IMPLEMENTED  ------------------
	
	override def compare(x: WeekDay, y: WeekDay): Int = values.indexOf(x) - values.indexOf(y)
	
	
	// OTHER    ---------------------
	
	/**
	  * @param index Targeted week day index (0-based)
	  * @return A week day that matches that index
	  */
	def apply(index: Int) = if (index >= 0) values(index % 7) else values(7 + (index % 7))
}

object WeekDays
{
	// NESTED   -----------------------
	
	/**
	  * Monday to Sunday -week
	  */
	case object MondayToSunday extends WeekDays
	{
		private lazy val indices = values.iterator.zipWithIndex.toMap
		override val values = Vector[WeekDay](Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday)
		
		override def indexOf(day: WeekDay): Int = indices(day)
	}
	/**
	  * Sunday to Saturday -week
	  */
	case object SundayToSaturday extends WeekDays
	{
		private lazy val indices = values.iterator.zipWithIndex.toMap
		override val values = Vector[WeekDay](Sunday, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday)
		
		override def indexOf(day: WeekDay): Int = indices(day)
	}
}
