package utopia.flow.time

import utopia.flow.operator.Steppable
import utopia.flow.operator.enumeration.Extreme
import utopia.flow.operator.enumeration.Extreme.{Max, Min}
import utopia.flow.operator.ordering.SelfComparable
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.time.TimeUnit.JTimeUnit

import java.util.concurrent
import scala.annotation.tailrec
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.language.implicitConversions

/**
  * An advanced version of Java's TimeUnit enumeration
  * @author Mikko Hilpinen
  * @since 11.11.2024, v2.5.1
  */
sealed trait TimeUnit extends SelfComparable[TimeUnit] with Steppable[TimeUnit]
{
	// ABSTRACT -----------------------------
	
	/**
	  * @return A multiplier applied to this unit in order to get seconds.
	  *         E.g. a minute has a multiplier of 60 and a millisecond has a multiplier of 1/1000.
	  */
	def toSecondsModifier: Double
	
	/**
	  * @return A multiplier that needs to be applied to this unit,
	  *         as well as the resulting Java time unit.
	  *
	  *         For most time units, the returned multiplier is 1,
	  *         but for time units that are not present in Java (e.g. a week), this multiplier may be larger.
	  */
	def toJava: (Int, JTimeUnit)
	
	
	// IMPLEMENTED  -------------------------
	
	override def self = this
	
	override def compareTo(o: TimeUnit) = toSecondsModifier.compareTo(o.toSecondsModifier)
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param targetUnit Unit to which values will be converted **to**
	  * @return A modifier that must be applied to a value with this unit in order to get the same value in 'targetUnit'
	  */
	def toModifier(targetUnit: TimeUnit) = toSecondsModifier / targetUnit.toSecondsModifier
	/**
	  * Alias for [[toModifier]]
	  */
	def /(targetUnit: TimeUnit) = toModifier(targetUnit)
	/**
	  * @param originalUnit Time unit of the original value
	  * @return A modifier that must be applied to that value in order to produce values in this unit
	  */
	def fromModifier(originalUnit: TimeUnit) = originalUnit.toModifier(this)
	
	/**
	  * @param count Length of time in these units
	  * @return Specified length of time
	  */
	def apply(count: Long) = {
		val (multiplier, jUnit) = toJava
		FiniteDuration(count * multiplier, jUnit)
	}
	/**
	  * @param count Length of time in these units
	  * @return Specified length of time
	  */
	def apply(count: Int): FiniteDuration = apply(count: Long)
	/**
	  * @param count Length of time in these units, in double precision
	  * @return Specified length of time. May be rounded to the previous nanosecond.
	  */
	@tailrec
	final def apply(count: Double): FiniteDuration = {
		// Checks whether this unit is too inaccurate to exactly represent the specified value
		val (multiplier, jUnit) = toJava
		val multipliedCount = count * multiplier
		val scale = BigDecimal.decimal(multipliedCount).scale
		// Case: This unit is accurate enough (i.e. no decimal places are being used)
		if (scale <= 0 || isMin)
			FiniteDuration(multipliedCount.toLong, jUnit)
		// Case: This unit is too inaccurate => Represents the specified number in a smaller unit (recursive)
		else {
			val smaller = less
			smaller.apply(count * toModifier(smaller))
		}
	}
	
	/**
	  * @param duration A duration of time
	  * @return Number of these units of time within that time duration.
	  *         For non-finite durations, yields positive or negative infinity or NaN.
	  */
	def countIn(duration: Duration) = {
		if (duration.isFinite)
			duration.length * fromModifier(duration.unit)
		else
			duration match {
				case Duration.Inf => Double.PositiveInfinity
				case Duration.MinusInf => Double.NegativeInfinity
				case _ => Double.NaN
			}
	}
}

object TimeUnit
{
	// TYPES    -----------------------------
	
	/**
	  * Java's TimeUnit enumeration
	  */
	type JTimeUnit = concurrent.TimeUnit
	
	
	// ATTRIBUTES   -------------------------
	
	/**
	  * All supported time unit values from smallest to largest
	  */
	val values = Vector(NanoSecond, MicroSecond, MilliSecond, Second, Minute, Hour, Day, Week)
	
	private val fromJavaMap = Map[JTimeUnit, TimeUnit](
		concurrent.TimeUnit.NANOSECONDS -> NanoSecond,
		concurrent.TimeUnit.MICROSECONDS -> MicroSecond,
		concurrent.TimeUnit.MILLISECONDS -> MilliSecond,
		concurrent.TimeUnit.SECONDS -> Second,
		concurrent.TimeUnit.MINUTES -> Minute,
		concurrent.TimeUnit.HOURS -> Hour,
		concurrent.TimeUnit.DAYS -> Day
	)
	
	
	// IMPLICIT -----------------------------
	
	/**
	  * @param unit A Java-based time unit
	  * @return Matching time unit
	  */
	implicit def apply(unit: JTimeUnit): TimeUnit = fromJavaMap(unit)
	
	
	// VALUES   -----------------------------
	
	/**
	  * Time unit of 1 second
	  */
	case object Second extends TimeUnit
	{
		override val toSecondsModifier: Double = 1.0
		override def toJava: (Int, JTimeUnit) = 1 -> concurrent.TimeUnit.SECONDS
		
		override def toString = "seconds"
		
		override def next(direction: Sign): TimeUnit = direction match {
			case Positive => Minute
			case Negative => MilliSecond
		}
		override def is(extreme: Extreme): Boolean = false
	}
	/**
	  * Time unit of 1 millisecond, i.e. 1/1000 seconds
	  */
	case object MilliSecond extends TimeUnit
	{
		override val toSecondsModifier: Double = 0.001
		override def toJava: (Int, JTimeUnit) = 1 -> concurrent.TimeUnit.MILLISECONDS
		
		override def toString = "millis"
		
		override def next(direction: Sign): TimeUnit = direction match {
			case Positive => Second
			case Negative => MicroSecond
		}
		override def is(extreme: Extreme): Boolean = false
	}
	/**
	  * Time unit of 1 microsecond, i.e. 1/1000 milliseconds
	  */
	case object MicroSecond extends TimeUnit
	{
		override val toSecondsModifier: Double = 0.001 * MilliSecond.toSecondsModifier
		override def toJava: (Int, JTimeUnit) = 1 -> concurrent.TimeUnit.MICROSECONDS
		
		override def toString = "microseconds"
		
		override def next(direction: Sign): TimeUnit = direction match {
			case Positive => MilliSecond
			case Negative => NanoSecond
		}
		override def is(extreme: Extreme): Boolean = false
	}
	/**
	  * Time unit of 1 nanosecond, i.e. 1/1000 microseconds
	  */
	case object NanoSecond extends TimeUnit
	{
		override val toSecondsModifier: Double = 0.001 * MicroSecond.toSecondsModifier
		override def toJava: (Int, JTimeUnit) = 1 -> concurrent.TimeUnit.NANOSECONDS
		
		override def toString = "nanos"
		
		override def next(direction: Sign): TimeUnit = direction match {
			case Positive => MicroSecond
			case Negative => this
		}
		override def is(extreme: Extreme): Boolean = extreme == Min
	}
	/**
	  * Time unit of 1 minute, i.e. 60 seconds
	  */
	case object Minute extends TimeUnit
	{
		override val toSecondsModifier: Double = 60
		override def toJava: (Int, JTimeUnit) = 1 -> concurrent.TimeUnit.MINUTES
		
		override def toString = "minutes"
		
		override def next(direction: Sign): TimeUnit = direction match {
			case Positive => Hour
			case Negative => Second
		}
		override def is(extreme: Extreme): Boolean = false
	}
	/**
	  * Time unit of 1 hour, i.e. 60 minutes
	  */
	case object Hour extends TimeUnit
	{
		override val toSecondsModifier: Double = 360
		override def toJava: (Int, JTimeUnit) = 1 -> concurrent.TimeUnit.HOURS
		
		override def toString = "hours"
		
		override def next(direction: Sign): TimeUnit = direction match {
			case Positive => Day
			case Negative => Minute
		}
		override def is(extreme: Extreme): Boolean = false
	}
	/**
	  * Time unit of 1 (calendar) day, i.e. 24 hours
	  */
	case object Day extends TimeUnit
	{
		override val toSecondsModifier: Double = Hour.toSecondsModifier * 24
		override def toJava: (Int, JTimeUnit) = 1 -> concurrent.TimeUnit.DAYS
		
		override def toString = "days"
		
		override def next(direction: Sign): TimeUnit = direction match {
			case Positive => Week
			case Negative => Hour
		}
		override def is(extreme: Extreme): Boolean = false
	}
	/**
	  * Time unit of 1 (calendar) week, i.e. 7 days
	  */
	case object Week extends TimeUnit
	{
		override val toSecondsModifier: Double = Day.toSecondsModifier * 7
		override def toJava: (Int, JTimeUnit) = 7 -> concurrent.TimeUnit.DAYS
		
		override def toString = "weeks"
		
		override def next(direction: Sign): TimeUnit = direction match {
			case Positive => this
			case Negative => Day
		}
		override def is(extreme: Extreme): Boolean = extreme == Max
	}
}
