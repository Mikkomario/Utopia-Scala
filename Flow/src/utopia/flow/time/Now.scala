package utopia.flow.time

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.operator.ordering.RichComparable
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.TimeUnit.JTimeUnit

import java.time.temporal.TemporalAmount
import java.time.{Instant, LocalDate, LocalDateTime, LocalTime}
import scala.language.implicitConversions

/**
  * An object that always resolves to the current time
  * @author Mikko Hilpinen
  * @since 16.3.2021, v1.9
  */
object Now extends ValueConvertible with RichComparable[Instant] with CanAppendJavaDuration[Instant]
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
	
	override protected def _max: Instant = Instant.MAX
	override protected def _min: Instant = Instant.MIN
	
	override def toValue: Value = toInstant
	
	override def compareTo(o: Instant) = toInstant.compareTo(o)
	
	override def +(amount: TemporalAmount): Instant = toInstant + amount
	override def -(amount: TemporalAmount): Instant = toInstant - amount
	
	override protected def _plus(amount: Long, unit: JTimeUnit): Instant = toInstant.plus(amount, unit)
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param instant A time instant
	  * @return The amount of time that has passed since that instant
	  */
	def -(instant: Instant) = toInstant - instant
}
