package utopia.flow.time

import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.operator.Steppable
import utopia.flow.operator.enumeration.Extreme
import utopia.flow.operator.enumeration.Extreme.{Max, Min}
import utopia.flow.operator.ordering.SelfComparable
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.time.TimeUnit.{JTimeUnit, divideToConvert, multiplyToConvert}

import java.util.concurrent
import scala.language.implicitConversions

/**
  * An advanced version of Java's TimeUnit enumeration
  * @author Mikko Hilpinen
  * @since 11.11.2024, v2.5.1
  */
sealed trait TimeUnit extends SelfComparable[TimeUnit] with Steppable[TimeUnit]
{
	// ATTRIBUTES   -------------------------
	
	/**
	 * @return Unit length duration, based on this unit
	 */
	lazy val unit: Duration = apply(1)
	
	
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
	
	/**
	 * @param larger A larger time unit
	 * @return A multiplier applied to that unit's value to get this unit's values
	 */
	protected def _multiplierFrom(larger: TimeUnit): Long
	/**
	 * @param smaller A smaller time unit
	 * @return A divider applied to that unit's value to get this unit's values
	 */
	protected def _dividerFrom(smaller: TimeUnit): Long
	
	
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
	def apply(count: Long) = Duration(count, this)
	/**
	  * @param count Length of time in these units
	  * @return Specified length of time
	  */
	def apply(count: Int): Duration = apply(count: Long)
	/**
	  * @param count Length of time in these units, in double precision
	  * @return Specified length of time. May be rounded to the previous nanosecond.
	  */
	def apply(count: Double): Duration = Duration(count, this)
	
	/**
	  * @param duration A duration of time
	  * @return Number of these units of time within that time duration.
	  */
	@deprecated("Deprecated for removal. Please use duration.to(this) instead", "v2.7")
	@throws[ArithmeticException]("If the specified duration is infinite")
	def countIn(duration: Duration): Long = duration.to(this)
	/**
	 * @param amount Amount of the instances of another unit
	 * @param unit The unit in which 'amount' is specified
	 * @return Number of instances of this unit in the specified amount. Rounded down, if applicable.
	 */
	def countIn(amount: Long, unit: TimeUnit): Long = {
		// Checks the relationship between these units
		val comparison = compareTo(unit)
		// Case: Same unit => No conversion
		if (comparison == 0)
			amount
		// Case: This unit is larger => Divides the specified amount
		else if (comparison > 0)
			amount / divideToConvert(unit, this)
		// Case: This unit is smaller => Multiplies the specified amount
		else
			amount * multiplyToConvert(unit, this)
	}
	/**
	 * @param amount Amount of the instances of another unit
	 * @param unit The unit in which 'amount' is specified
	 * @return Number of instances of this unit in the specified amount
	 */
	def countPreciselyIn(amount: Double, unit: TimeUnit) = amount * fromModifier(unit)
	
	/**
	 * Counts the instances of this unit in the remainder of another unit conversion.
	 * E.g. MicroSecond.countRemainderIn(1234, MilliSecond, Second)
	 * would calculate microseconds from the remainder of 1234ms / 1s (i.e. from 234 ms).
	 * @param amount The number of time units from which the remainder is calculated
	 * @param unit Unit in which 'amount' is given
	 * @param afterUnit Unit from which the remainder is calculated.
	 * @return Number of instances of this time unit in the remainder of 'amount' * 'unit' / 1 * 'afterUnit'.
	 */
	def countRemainderFrom(amount: Long, unit: TimeUnit, afterUnit: TimeUnit) = {
		// Case: There can't be a remainder, because the input or output have no remainder after 'afterUnit'
		if (this >= afterUnit || unit >= afterUnit)
			0L
		// Case: Remainder may be found => Acquires the remainder first (in 'unit')
		else {
			val remainder = amount % divideToConvert(unit, afterUnit)
			// Then converts that remainder to this unit
			countIn(remainder, unit)
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
	
	private val kilo = 1000L
	private val mega = 1000000L
	
	private val divideToConvertCache: Cache[Pair[TimeUnit], Long] =
		Cache { ends => ends.second._dividerFrom(ends.first) }
	private val multiplyToConvertCache: Cache[Pair[TimeUnit], Long] =
		Cache { ends => ends.second._multiplierFrom(ends.first) }
	
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
	
	
	// OTHER    -----------------------------
	
	private def multiplyToConvert(from: TimeUnit, to: TimeUnit): Long = multiplyToConvertCache(Pair(from, to))
	private def divideToConvert(from: TimeUnit, to: TimeUnit): Long = divideToConvertCache(Pair(from, to))
	
	
	// VALUES   -----------------------------
	
	/**
	  * Time unit of 1 second
	  */
	case object Second extends TimeUnit
	{
		override val toSecondsModifier: Double = 1.0
		override lazy val toString = "seconds"
		
		override def toJava: (Int, JTimeUnit) = 1 -> concurrent.TimeUnit.SECONDS
		
		override def next(direction: Sign): TimeUnit = direction match {
			case Positive => Minute
			case Negative => MilliSecond
		}
		override def is(extreme: Extreme): Boolean = false
		
		override protected def _multiplierFrom(larger: TimeUnit): Long = larger match {
			case Second => 1
			case Minute => 60
			case Hour => 3600
			case unit if unit < this => throw new IllegalArgumentException(s"Can't multiply $unit to get seconds")
			case unit => multiplyToConvert(unit, Hour) * 3600
		}
		override protected def _dividerFrom(smaller: TimeUnit): Long = smaller match {
			case Second => 1
			case MilliSecond => kilo
			case MicroSecond => mega
			case NanoSecond => 1000000000L
			case unit => throw new IllegalArgumentException(s"Can't divide $unit to get seconds")
		}
	}
	/**
	  * Time unit of 1 millisecond, i.e. 1/1000 seconds
	  */
	case object MilliSecond extends TimeUnit
	{
		override val toSecondsModifier: Double = 0.001
		override lazy val toString = "millis"
		
		override def toJava: (Int, JTimeUnit) = 1 -> concurrent.TimeUnit.MILLISECONDS
		
		override def next(direction: Sign): TimeUnit = direction match {
			case Positive => Second
			case Negative => MicroSecond
		}
		override def is(extreme: Extreme): Boolean = false
		
		override protected def _multiplierFrom(larger: TimeUnit): Long = larger match {
			case MilliSecond => 1
			case Second => kilo
			case MicroSecond | NanoSecond => throw new IllegalArgumentException("Can't multiply a smaller unit")
			case unit => multiplyToConvert(unit, Second) * kilo
		}
		override protected def _dividerFrom(smaller: TimeUnit): Long = smaller match {
			case MilliSecond => 1
			case MicroSecond => kilo
			case NanoSecond => mega
			case unit => throw new IllegalArgumentException(s"Can't divide $unit to get milliseconds")
		}
	}
	/**
	  * Time unit of 1 microsecond, i.e. 1/1000 milliseconds
	  */
	case object MicroSecond extends TimeUnit
	{
		override val toSecondsModifier: Double = 0.001 * MilliSecond.toSecondsModifier
		override lazy val toString = "microseconds"
		
		override def toJava: (Int, JTimeUnit) = 1 -> concurrent.TimeUnit.MICROSECONDS
		
		override def next(direction: Sign): TimeUnit = direction match {
			case Positive => MilliSecond
			case Negative => NanoSecond
		}
		override def is(extreme: Extreme): Boolean = false
		
		override protected def _multiplierFrom(larger: TimeUnit): Long = larger match {
			case MicroSecond => 1
			case MilliSecond => kilo
			case Second => mega
			case NanoSecond => throw new IllegalArgumentException("Can't multiply nanos to get micros")
			case unit => multiplyToConvert(unit, Second) * mega
		}
		override protected def _dividerFrom(smaller: TimeUnit): Long = smaller match {
			case MicroSecond => 1
			case NanoSecond => kilo
			case unit => throw new IllegalArgumentException(s"Can't divide $unit to get microseconds")
		}
	}
	/**
	  * Time unit of 1 nanosecond, i.e. 1/1000 microseconds
	  */
	case object NanoSecond extends TimeUnit
	{
		override val toSecondsModifier: Double = 0.001 * MicroSecond.toSecondsModifier
		override lazy val toString = "nanos"
		
		override def toJava: (Int, JTimeUnit) = 1 -> concurrent.TimeUnit.NANOSECONDS
		
		override def next(direction: Sign): TimeUnit = direction match {
			case Positive => MicroSecond
			case Negative => this
		}
		override def is(extreme: Extreme): Boolean = extreme == Min
		
		override protected def _multiplierFrom(larger: TimeUnit): Long = larger match {
			case NanoSecond => 1
			case MicroSecond => kilo
			case MilliSecond => mega
			case Second => 1000000000L
			case unit => multiplyToConvert(unit, MilliSecond) * mega
		}
		override protected def _dividerFrom(smaller: TimeUnit): Long = {
			if (smaller == this)
				1
			else
				throw new IllegalArgumentException(s"Can't divide $smaller to get nanoseconds")
		}
	}
	/**
	  * Time unit of 1 minute, i.e. 60 seconds
	  */
	case object Minute extends TimeUnit
	{
		override val toSecondsModifier: Double = 60
		override lazy val toString = "minutes"
		
		override def toJava: (Int, JTimeUnit) = 1 -> concurrent.TimeUnit.MINUTES
		
		override def next(direction: Sign): TimeUnit = direction match {
			case Positive => Hour
			case Negative => Second
		}
		override def is(extreme: Extreme): Boolean = false
		
		override protected def _multiplierFrom(larger: TimeUnit): Long = larger match {
			case Minute => 1
			case Hour => 60
			case unit if unit < this => throw new IllegalArgumentException(s"Can't divide $unit to get minutes")
			case unit => multiplyToConvert(unit, Hour) * 60
		}
		override protected def _dividerFrom(smaller: TimeUnit): Long = smaller match {
			case Minute => 1
			case Second => 60
			case unit if unit > this => throw new IllegalArgumentException(s"Can't multiply $unit to get minutes")
			case unit => divideToConvert(unit, Second) * 60
		}
	}
	/**
	  * Time unit of 1 hour, i.e. 60 minutes
	  */
	case object Hour extends TimeUnit
	{
		override val toSecondsModifier: Double = 3600
		override lazy val toString = "hours"
		
		override def toJava: (Int, JTimeUnit) = 1 -> concurrent.TimeUnit.HOURS
		
		override def next(direction: Sign): TimeUnit = direction match {
			case Positive => Day
			case Negative => Minute
		}
		override def is(extreme: Extreme): Boolean = false
		
		override protected def _multiplierFrom(larger: TimeUnit): Long = larger match {
			case Hour => 1
			case Day => 24
			case unit if unit < this => throw new IllegalArgumentException(s"Can't divide $unit to get hours")
			case unit => multiplyToConvert(unit, Day) * 24
		}
		override protected def _dividerFrom(smaller: TimeUnit): Long = smaller match {
			case Hour => 1
			case Minute => 60
			case Second => 3600
			case unit if unit > this => throw new IllegalArgumentException(s"Can't multiply $unit to get hours")
			case unit => divideToConvert(unit, Second) * 3600
		}
	}
	/**
	  * Time unit of 1 (calendar) day, i.e. 24 hours
	  */
	case object Day extends TimeUnit
	{
		override val toSecondsModifier: Double = Hour.toSecondsModifier * 24
		override lazy val toString = "days"
		
		override def toJava: (Int, JTimeUnit) = 1 -> concurrent.TimeUnit.DAYS
		
		override def next(direction: Sign): TimeUnit = direction match {
			case Positive => Week
			case Negative => Hour
		}
		override def is(extreme: Extreme): Boolean = false
		
		override protected def _multiplierFrom(larger: TimeUnit): Long = larger match {
			case Day => 1
			case Week => 7
			case unit => throw new IllegalArgumentException(s"Can't multiply $unit to get days")
		}
		override protected def _dividerFrom(smaller: TimeUnit): Long = smaller match {
			case Day => 1
			case Hour => 24
			case Week => throw new IllegalArgumentException(s"Can't divide weeks to get days")
			case unit => divideToConvert(unit, Hour) * 24
		}
	}
	/**
	  * Time unit of 1 (calendar) week, i.e. 7 days
	  */
	case object Week extends TimeUnit
	{
		override val toSecondsModifier: Double = Day.toSecondsModifier * 7
		override lazy val toString = "weeks"
		
		override def toJava: (Int, JTimeUnit) = 7 -> concurrent.TimeUnit.DAYS
		
		override def next(direction: Sign): TimeUnit = direction match {
			case Positive => this
			case Negative => Day
		}
		override def is(extreme: Extreme): Boolean = extreme == Max
		
		override protected def _multiplierFrom(larger: TimeUnit): Long = {
			if (larger == this)
				1
			else
				throw new IllegalArgumentException(s"Can't multiply $larger to get weeks")
		}
		override protected def _dividerFrom(smaller: TimeUnit): Long = smaller match {
			case Week => 1
			case Day => 7
			case unit => divideToConvert(unit, Day) * 7
		}
	}
}
