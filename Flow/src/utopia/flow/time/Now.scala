package utopia.flow.time

import scala.language.implicitConversions
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConvertible
import utopia.flow.generic.ValueConversions._
import TimeExtensions._
import utopia.flow.util.RichComparable

import java.time.{Instant, LocalDate, LocalDateTime, LocalTime}
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * An object that always resolves to the current time
  * @author Mikko Hilpinen
  * @since 16.3.2021, v1.9
  */
object Now extends ValueConvertible with RichComparable[Instant]
{
	// IMPLICIT -------------------------------
	
	implicit def currentLocalDateTime(now: Now.type): LocalDateTime = now.toLocalDateTime
	
	implicit def currentInstant(now: Now.type): Instant = now.toInstant
	
	implicit def currentLocalTime(now: Now.type): LocalTime = now.toLocalTime
	
	
	// COMPUTED 	---------------------------
	
	/**
	  * @return Current date (local)
	  */
	def toLocalDate: LocalDate = LocalDate.now()
	/**
	* @return Current local date & time
	*/
	def toLocalDateTime: LocalDateTime = LocalDateTime.now()
	/**
	  * @return Current time (local)
	  */
	def toLocalTime: LocalTime = LocalTime.now()
	/**
	  * @return Current time instant
	  */
	def toInstant: Instant = Instant.now()
	
	
	// IMPLEMENTED	--------------------------
	
	override def toString = LocalDateTime.now().toString
	
	override implicit def toValue: Value = toInstant
	
	override def compareTo(o: Instant) = toInstant.compareTo(o)
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param duration Duration to add
	  * @return Time after a specific amount of duration has passed
	  */
	def +(duration: FiniteDuration) = toInstant + duration
	
	/**
	  * @param duration Duration to subtract
	  * @return Time before specified amount of duration passed
	  */
	def -(duration: FiniteDuration) = toInstant - duration
	/**
	  * @param instant A time instant
	  * @return The amount of time that has passed since that instant
	  */
	def -(instant: Instant) = toInstant - instant
}
