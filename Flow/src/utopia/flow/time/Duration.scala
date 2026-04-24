package utopia.flow.time

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.MayBeAboutZero
import utopia.flow.operator.combine.Combinable.SelfCombinable
import utopia.flow.operator.combine.LinearScalable
import utopia.flow.operator.equality.{ApproxSelfEquals, EqualsBy, EqualsFunction}
import utopia.flow.operator.ordering.SelfComparable
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.operator.sign.SignOrZero.Neutral
import utopia.flow.operator.sign.{HasSign, Sign, SignOrZero}
import utopia.flow.time.Duration.{JDuration, SDuration, equalsByMillis, kilo, lengthsEqual, mega, secondToNano}
import utopia.flow.time.TimeExtensions.{CanAppendJavaDuration, ExtendedInstant, ExtendedLocalDateTime, timeUnitToChronoUnit}
import utopia.flow.time.TimeUnit.{Day, Hour, MicroSecond, MilliSecond, Minute, NanoSecond, Second, Week}
import utopia.flow.view.immutable.caching.Lazy

import java.time.{Instant, LocalDateTime}
import scala.Double.NaN
import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

object Duration
{
	// ATTRIBUTES   --------------------
	
	private val kilo = 1000
	private val mega = 1000000
	private val secondToNano = 1000000000L
	
	private lazy val equalsByMillis: EqualsFunction[Duration] = equalsBy(MilliSecond)
	
	/**
	 * A duration of length 0
	 */
	lazy val zero = apply(0, Second)
	/**
	 * A duration with infinite length
	 */
	lazy val infinite = apply(scala.concurrent.duration.Duration.Inf)
	/**
	 * A duration with infinite length to the negative direction
	 */
	lazy val minusInfinite = apply(scala.concurrent.duration.Duration.MinusInf)
	/**
	 * A duration with an undefined (NaN) length
	 */
	lazy val undefined = apply(scala.concurrent.duration.Duration.Undefined)
	
	
	// TYPES    ------------------------
	
	/**
	 * Scala's Duration data type
	 */
	type SDuration = scala.concurrent.duration.Duration
	/**
	 * Java's Duration data type
	 */
	type JDuration = java.time.Duration
	
	
	// IMPLICIT -------------------------
	
	/**
	 * @param duration A Scala-based Duration instance
	 * @return A duration wrapping that instance
	 */
	implicit def apply(duration: SDuration): Duration = new Duration(Left(Right(duration)))
	/**
	 * @param duration A Java-based Duration instance
	 * @return A duration wrapping that instance
	 */
	implicit def apply(duration: JDuration): Duration = new Duration(Left(Left(duration)))
	
	
	// OTHER    -------------------------
	
	/**
	 * @param length Length of this duration
	 * @param unit Unit in which 'length' is given
	 * @return A new duration with the specified length
	 */
	def apply(length: Int, unit: TimeUnit): Duration = apply(length: Long, unit)
	/**
	 * @param length Length of this duration
	 * @param unit Unit in which 'length' is given
	 * @return A new duration with the specified length
	 */
	def apply(length: Long, unit: TimeUnit): Duration = new Duration(Right(length -> unit))
	/**
	 * @param length Length of this duration
	 * @param unit Unit in which 'length' is given
	 * @return A new duration with the specified length
	 */
	// TODO: Review whether this is more optimal than using %
	def apply(length: Double, unit: TimeUnit): Duration = {
		// Checks whether this unit is too inaccurate to exactly represent the specified value
		Try { BigDecimal.decimal(length).scale } match {
			case Success(scale) =>
				// Case: This unit is accurate enough (i.e. no decimal places are being used)
				if (scale <= 0 || unit.isMin)
					apply(length.toLong, unit)
				// Case: This unit is too inaccurate => Represents the specified number in a smaller unit (recursive)
				else {
					val smaller = unit.less
					apply(smaller.countPreciselyIn(length, unit), smaller)
				}
			// TODO: Add better logging
			case Failure(error) =>
				error.printStackTrace()
				apply(length.toLong, unit)
		}
	}
	
	/**
	 * @param sign Direction of this duration. [[Neutral]] if undefined / not applicable.
	 * @return An infinite or undefined duration matching the specified sign.
	 */
	def infiniteOf(sign: SignOrZero) = sign match {
		case Positive => infinite
		case Negative => minusInfinite
		case Neutral => undefined
	}
	
	/**
	 * @param unit Unit in which comparisons are performed
	 * @return An equality function in the specified precision (rounding down)
	 */
	def equalsBy(unit: TimeUnit): EqualsFunction[Duration] = { (a, b) => a.to(unit) == b.to(unit) }
	/**
	 * @param unit Unit in which computations (such as integer conversions) are performed
	 * @return A numeric implementation for durations, in the specified unit
	 */
	def isNumericIn(unit: TimeUnit): Numeric[Duration] = DurationIsNumericIn(unit)
	
	private def lengthsEqual(lengths: Pair[(Long, TimeUnit)]) =
		mergeLengths(lengths) { case (a, b, _) => a == b }
	
	/**
	 * Acquires a value by merging the immediately available lengths of two durations, or using a backup function
	 * @param durations Durations to merge
	 * @param f A merge function that accepts the lengths of these durations,
	 *          and the unit in which merging is performed (which is the more precise unit of the two available).
	 * @param onInfinite A function called if one or both of these durations don't have a finite length
	 * @param orElse A function called if the lengths are not immediately available.
	 *               Yields None if the lengths should be used, anyway.
	 * @tparam A Type of merge results
	 * @return Merge results
	 */
	private def tryMergeLengthsOf[A](durations: Pair[Duration])(f: (Long, Long, TimeUnit) => A)(onInfinite: => A)
	                                (orElse: => Option[A]) =
	{
		// Case: One or both of the durations is infinite
		if (durations.exists { _.isInfinite })
			onInfinite
		// Case: No infinite values (expected) => Attempts to acquire immediate lengths
		else
			durations.findForBoth { _.lazyLength.current } match {
				// Case: Both lengths are immediately available => Merges them, if possible
				case Some(lengths) =>
					lengths.findForBoth { _.toOption } match {
						case Some(lengths) => mergeLengths(lengths)(f)
						case None => onInfinite
					}
				// Case: Lengths are not immediately available => Uses the specified backup function
				case None =>
					orElse.getOrElse {
						// Case: Backup function didn't yield results
						//       => Acquires the missing lengths and uses them instead
						mergeLengthsOf(durations)(f)(onInfinite)
					}
			}
	}
	/**
	 * Merges two durations based on their lengths, if possible
	 * @param durations Durations to merge
	 * @param f A function that merges the lengths of these two durations
	 * @param onInfinite A function called if one or both of these durations are infinite
	 * @tparam A Type of function results
	 * @return Result of either 'f' or 'onInfinite'
	 */
	private def mergeLengthsOf[A](durations: Pair[Duration])(f: (Long, Long, TimeUnit) => A)(onInfinite: => A) =
		durations.findForBoth { _.tryLength.toOption } match {
			case Some(lengths) => mergeLengths(lengths)(f)
			case None => onInfinite
		}
	/**
	 * Merges two duration lengths. Converts the lengths to a common unit before merging.
	 * @param lengths Lengths to merge
	 * @param f A function that merges the lengths
	 * @tparam A Type of merge results
	 * @return 'f' results
	 */
	private def mergeLengths[A](lengths: Pair[(Long, TimeUnit)])(f: (Long, Long, TimeUnit) => A) = {
		val compareUnit = lengths.mapAndMerge { _._2 } { _ min _ }
		lengths.mapAndMerge { case (length, unit) => compareUnit.countIn(length, unit) } { f(_, _, compareUnit) }
	}
	
	
	// NESTED   ---------------------------
	
	/**
	 * Provides numeric functions for instances of [[Duration]].
	 * Note The * (i.e. times) implementation doesn't correctly yield unit to the power of two,
	 * as that is not supported by the current data type structures.
	 * @param unit Unit in which comparisons and other functions are performed.
	 *             Affects, toX & fromX -functions, for example.
	 */
	case class DurationIsNumericIn(unit: TimeUnit) extends Numeric[Duration]
	{
		override def zero: Duration = Duration.zero
		override def one: Duration = unit.unit
		
		override def fromInt(x: Int): Duration = unit(x)
		// TODO: Could add unit parsing as well
		override def parseString(str: String): Option[Duration] = str.toDoubleOption.map { unit(_) }
		
		override def toInt(x: Duration): Int = x.to(unit).toInt
		override def toLong(x: Duration): Long = x.to(unit)
		override def toFloat(x: Duration): Float = x.toPrecise(unit).toFloat
		override def toDouble(x: Duration): Double = x.toPrecise(unit)
		
		override def sign(x: Duration): Duration = x.sign match {
			case Positive => one
			case Negative => unit(-1)
			case Neutral => zero
		}
		
		override def negate(x: Duration): Duration = -x
		override def abs(x: Duration): Duration = x.abs
		
		override def plus(x: Duration, y: Duration): Duration = x + y
		override def minus(x: Duration, y: Duration): Duration = x - y
		
		// Yields the result in a 'unit' duration
		override def times(x: Duration, y: Duration): Duration =
			Pair(x, y).findForBoth { _.tryLength.toOption } match {
				case Some(lengths) =>
					unit(lengths.mapAndMerge { case (length, from) => unit.countIn(length, from) } { _ * _ })
				case None => Duration.infiniteOf(x.sign * y.sign)
			}
		
		override def compare(x: Duration, y: Duration): Int = x.compareTo(y)
	}
}

/**
 * Combines the Scala's and Java's duration representations, and offers various utility functions.
 * @author Mikko Hilpinen
 * @since 15.10.2025, v2.7
 */
// Wraps either:
//      1. A length and a unit (Right)
//      2. A Scala Duration (Left -> Right)
//      3. A Java Duration (Left -> Left)
class Duration(wrapped: Either[Either[JDuration, SDuration], (Long, TimeUnit)])
	extends Equals with ApproxSelfEquals[Duration] with SelfComparable[Duration] with LinearScalable[Duration]
		with SelfCombinable[Duration] with MayBeAboutZero[Duration, Duration] with HasSign
{
	// ATTRIBUTES   --------------------------
	
	/**
	 * A lazily initialized length and unit of this duration. Contains a failure for infinite durations.
	 */
	private val lazyLength = wrapped match {
		// Case: Already specified as length + unit => Wraps these values without lazy containment
		case Right(length) => Lazy.initialized(Success(length))
		// Case: Wraps another duration instance => The calculation is performed lazily
		case Left(wrapped) =>
			Lazy {
				wrapped match {
					// Case: Scala duration => Wraps the length and unit properties, if available
					case Right(duration) =>
						duration match {
							case finite: FiniteDuration => Success(finite.length -> TimeUnit(finite.unit))
							case infinite => Failure(new ArithmeticException(s"$infinite has no length"))
						}
					// Case: Java duration => Converts the seconds and nanos -parts to a valid unit length
					case Left(duration) =>
						val result = {
							// Case: No nanos => Uses the second unit
							if (duration.getNano == 0)
								duration.getSeconds -> Second
							else if (duration.getNano % kilo == 0) {
								// Case: Full milliseconds used
								if (duration.getNano % mega == 0)
									(duration.getSeconds * kilo + duration.getNano / mega) -> MilliSecond
								// Case: Full microseconds used
								else
									(duration.getSeconds * mega + duration.getNano / kilo) -> MicroSecond
							}
							// Case: The duration contains smaller parts than microseconds
							//       => Uses NanoSecond as the unit
							else
								(duration.getSeconds * secondToNano + duration.getNano) -> NanoSecond
						}
						Success(result)
				}
			}
	}
	/**
	 * A lazily initialized Scala version of this duration
	 */
	private val lazyScala = wrapped match {
		// Case: Already a Scala duration => Wraps it without lazy containment
		case Left(Right(duration)) => Lazy.initialized(duration)
		// Case: Some other form => Acquires the length and unit lazily and maps them lazily to a Scala duration
		case _ =>
			lazyLength.map {
				case Success((length, unit)) =>
					val (multiplier, jUnit) = unit.toJava
					FiniteDuration(length * multiplier, jUnit)
				
				// Case: Length and unit are not available (unexpected) => Yields undefined duration
				case Failure(_) => scala.concurrent.duration.Duration.Undefined
			}
	}
	/**
	 * A lazily initialized Java version of this duration
	 */
	private val lazyJava = wrapped match {
		// Case: This duration consists of an length + unit => Lazily converts it to a Java duration
		case Right((length, unit)) =>
			Lazy {
				val (multiplier, jUnit) = unit.toJava
				Try { java.time.Duration.of(length * multiplier, jUnit) }
			}
		// Case: Wraps another duration
		case Left(wrapped) =>
			wrapped match {
				// Case: Already a Java duration => Skips the lazy containment
				case Left(duration) => Lazy.initialized(Success(duration))
				// Case: A Scala duration => Attempts to lazily convert it to a Java duration
				case Right(duration) =>
					Lazy {
						duration match {
							case finite: FiniteDuration => Try { java.time.Duration.of(finite.length, finite.unit) }
							// Case: Infinite duration => Can't convert
							case infinite => Failure(
								new ArithmeticException(s"$infinite can't be converted to a finite Java duration"))
						}
					}
			}
	}
	
	/**
	 * Whether this is a finite, defined duration.
	 * False if infinite or undefined.
	 */
	lazy val isFinite = wrapped match {
		case Left(Right(duration)) => duration.isFinite
		case _ => true
	}
	
	override lazy val sign: SignOrZero =
		optimized { (length, _) => Sign.of(length) } { d =>
			if (d.isFinite)
				Sign.of(d.length)
			else
				d match {
					case scala.concurrent.duration.Duration.Inf => Positive
					case scala.concurrent.duration.Duration.MinusInf => Negative
					case _ => Neutral
				}
		} { d => if (d.isNegative) Negative else if (d.isZero) Neutral else Positive } {
			lazyScala.current match {
				case Some(scala.concurrent.duration.Duration.Inf) => Positive
				case Some(scala.concurrent.duration.Duration.MinusInf) => Negative
				case _ => Neutral
			}
		}
	override lazy val isZero: Boolean =
		optimized { case (length, _) => length == 0 } { d => d.isFinite && d.length == 0 } {
			d => d.getSeconds == 0 && d.getNano == 0 } { false }
	override lazy val isAboutZero: Boolean = tryLength match {
		case Success((length, unit)) =>
			if (unit >= MilliSecond) length == 0 else MilliSecond.countIn(length.abs, unit) == 0
		case _ => false
	}
	
	// Caches the generated hashCode
	private lazy val _hashCode = tryLength match {
		case Success((length, unit)) => EqualsBy.hashCodeFrom(length, unit)
		case _ => sign.hashCode()
	}
	
	
	// COMPUTED ----------------------------
	
	/**
	 * @return A positive version of this duration
	 */
	def abs = if (isNegative) -this else this
	
	/**
	 * @return This duration in nanoseconds
	 */
	def toNanos = to(NanoSecond)
	/**
	 * @return This duration in microseconds (rounded down)
	 */
	def toMicros = to(MicroSecond)
	/**
	 * @return This duration in milliseconds (rounded down)
	 */
	def toMillis = to(MilliSecond)
	/**
	 * @return This duration in seconds (rounded down)
	 */
	def toSeconds = to(Second)
	/**
	 * @return This duration in minutes (rounded down)
	 */
	def toMinutes = to(Minute)
	/**
	 * @return This duration in hours (rounded down)
	 */
	def toHours = to(Hour)
	/**
	 * @return This duration in days (rounded down)
	 */
	def toDays = to(Day)
	/**
	 * @return This duration in weeks (rounded down)
	 */
	def toWeeks = to(Week)
	
	/**
	 * @return This duration in milliseconds, in double precision
	 */
	def toPreciseMillis = toPrecise(MilliSecond)
	/**
	 * @return This duration in seconds, in double precision
	 */
	def toPreciseSeconds = toPrecise(Second)
	/**
	 * @return This duration in minutes, in double precision
	 */
	def toPreciseMinutes = toPrecise(Minute)
	/**
	 * @return This duration in hours, in double precision
	 */
	def toPreciseHours = toPrecise(Hour)
	/**
	 * @return This duration in days, in double precision
	 */
	def toPreciseDays = toPrecise(Day)
	/**
	 * @return This duration in weeks, in double precision
	 */
	def toPreciseWeeks = toPrecise(Week)
	
	/**
	 * @return The length and unit of this duration.
	 *         Failure if this duration is infinite or undefined.
	 */
	def tryLength = lazyLength.value
	/**
	 * @throws java.lang.ArithmeticException If this duration is infinite or undefined
	 * @return The length and unit of this duration.
	 */
	@throws[ArithmeticException]("If this duration is not finite")
	def length = tryLength.get
	
	/**
	 * @return Whether this is an infinite or undefined duration
	 */
	def isInfinite = !isFinite
	/**
	 * @return Some(this) if this is a finite duration. None otherwise.
	 */
	def ifFinite = if (isFinite) Some(this) else None
	/**
	 * Alias for [[ifFinite]]
	 */
	def finite = ifFinite
	/**
	 * @return If this duration is finite and defined, returns this. Otherwise returns a zero length duration.
	 */
	def finiteOrZero = if (isFinite) this else Duration.zero
	
	/**
	 * @return A [[scala.concurrent.duration.Duration]] based on this duration
	 */
	def toScala = lazyScala.value
	/**
	 * @return A [[java.time.Duration]] based on this duration.
	 *         Failure if this duration was infinite or undefined.
	 */
	def tryToJava = lazyJava.value
	/**
	 * @throws java.lang.ArithmeticException if this duration was infinite or undefined
	 * @return A [[java.time.Duration]] based on this duration
	 */
	@throws[ArithmeticException]("If this duration is infinite or undefined")
	def toJava = tryToJava.get
	
	/**
	 * @return Either:
	 *              - Right: The length and unit of this duration
	 *              - Left: A Scala duration based on this instance
	 */
	def lengthOrScala = {
		// Checks whether an immediate length value is available
		val immediateLength = lazyLength.current
		immediateLength.flatMap { _.toOption } match {
			// Case: Length available => Yields it
			case Some(length) => Right(length)
			// Case: Length not immediately available => Checks if a Scala instance is immediately available
			case None =>
				lazyScala.current match {
					// Case: Scala instance available => Yields that
					case Some(scala) => Left(scala)
					case None =>
						// Case: Immediately available length was undefined => Yields a scala instance instead
						if (immediateLength.isDefined)
							Left(toScala)
						// Case: Length needs to be calculated
						else
							tryLength match {
								// Case: Success => Yields length
								case Success(length) => Right(length)
								// Case: Failure => Yields a Scala version instead
								case _ => Left(toScala)
							}
				}
		}
	}
	/**
	 * @return Either:
	 *              - Right: the length and unit of this duration
	 *              - Left: A Java duration based on this instance
	 *
	 *         Yields a failure if this is an infinite or undefined duration
	 */
	def tryLengthOrJava: Try[Either[JDuration, (Long, TimeUnit)]] = lazyLength.current match {
		// Case: Length immediately available => Yields that
		case Some(length) => length.map(Right.apply)
		case None =>
			lazyJava.current match {
				// Case: Java instance immediately available => Yields that
				case Some(java) => java.map(Left.apply)
				// Case: Neither is immediately available => Acquires the length value
				case None => tryLength.map(Right.apply)
			}
	}
	
	/**
	 * Converts this duration into a [[FiniteDuration]]
	 * @return A finite Scala duration based on this instance. Failure if this was not finite.
	 */
	def tryFiniteScala = toScala match {
		case finite: FiniteDuration => Success(finite)
		case infinite => Failure(new ArithmeticException(s"$infinite is not finite"))
	}
	/**
	 * Converts this duration into a [[FiniteDuration]]
	 * @throws java.lang.ArithmeticException If this duration was not finite
	 * @return A finite Scala duration based on this instance
	 */
	@throws[ArithmeticException]("If this duration is not finite")
	def finiteScala = tryFiniteScala.get
	
	/**
	 * @return Describes this duration in a suitable unit and precision
	 */
	def description: String = {
		if (isInfinite)
			"infinite"
		else if (isZero)
			"0s"
		else {
			val seconds = toPreciseSeconds
			if (seconds.abs < 0.1) {
				val millis = toPreciseMillis
				if (millis < 0.1)
					s"$toNanos nanos"
				else if (millis < 1)
					f"$millis%1.2f millis"
				else
					s"${ millis.toInt.toString } millis"
			}
			else if (seconds.abs >= 120) {
				val hoursPart = (seconds / 3600).toInt
				val minutesPart = ((seconds % 3600) / 60).toInt
				val secondsPart = (seconds % 60).toInt
				
				if (hoursPart.abs > 72) {
					if (hoursPart.abs > 504) {
						val weeks = toPreciseWeeks
						f"$weeks%1.2f weeks"
					}
					else {
						val hours = toPreciseHours
						val daysPart = (hours / 24.0).toInt
						val dayHoursPart = hours % 24
						if (dayHoursPart >= 23.995)
							s"${ daysPart + 1 } days"
						else if (dayHoursPart <= -23.995)
							s"${ daysPart - 1 } days"
						else if (dayHoursPart.abs >= 0.005)
							f"$daysPart days $dayHoursPart%1.2f hours"
						else
							s"$daysPart days"
					}
				}
				else if (hoursPart != 0)
					s"$hoursPart h $minutesPart min"
				else
					s"$minutesPart min $secondsPart s"
			}
			else
				f"$seconds%1.2f seconds"
		}
	}
	
	
	// IMPLEMENTED  --------------------------
	
	override def self: Duration = this
	
	override def unary_- : Duration = this * -1
	
	override def approxEqualsFunction: EqualsFunction[Duration] = equalsByMillis
	
	override def hashCode(): Int = _hashCode
	// Can equal with Java and Scala -based instances, also
	override def canEqual(that: Any): Boolean =
		that.isInstanceOf[Duration] || that.isInstanceOf[SDuration] || that.isInstanceOf[JDuration]
	override def equals(obj: Any): Boolean = obj match {
		// Case: Comparing two durations => Compares using the best available versions
		case other: Duration =>
			mergeWith(other) { case (a, b, _) => a == b } {
				_.isSymmetric } { _.merge { _.equals(_) } } { isFinite == other.isFinite && sign == other.sign }
		// Case: Comparing with a scala duration
		case duration: SDuration =>
			lazyScala.current match {
				// Case: Wraps a scala version => Delegates comparison to those
				case Some(d) => d == duration
				case None =>
					// Case: Comparing with a finite scala duration => Compares by length
					if (duration.isFinite)
						tryLength match {
							case Success(myLength) =>
								lengthsEqual(Pair(myLength, duration.length -> TimeUnit(duration.unit)))
							case _ => false
						}
					// Case: Compared duration is not finite => Delegates to the comparison to the Scala implementation
					else
						toScala == duration
			}
		// Case: Comparing with a Java duration => Delegates comparison to the Java version
		case duration: JDuration =>
			tryToJava match {
				case Success(asJava) => asJava.equals(duration)
				case _ => false
			}
		case _ => false
	}
	
	// Delegates the comparison to the appropriate instance
	override def compareTo(o: Duration): Int =
		mergeWith(o) {
			(a, b, _) => a.compareTo(b) } { _.merge { _ compareTo _ } } { _.merge { _ compareTo _ } } {
			// Or handles the infinite use-cases
			if (isInfinite)
				if (o.isInfinite)
					sign.compareTo(o.sign)
				else if (isNegative)
					-1
				else
					1
			else if (o.isNegative)
				1
			else
				-1
		}
	
	override def +(other: Duration): Duration = {
		// Case: Adding zero => No change
		if (other.isZero)
			this
		// Case: Adding to zero => Can just yield the other instance
		else if (isZero)
			other
		else
			mergeWith(other) {
				(a, b, unit) => Duration(a + b, unit) } { _.merge { _ + _ } } { _.merge { _ plus _ } } {
				if (sign == other.sign) Duration.infiniteOf(sign) else Duration.undefined }
	}
	
	override def *(mod: Double): Duration = {
		// Case: Multiplication by zero => 0
		if (mod == 0)
			Duration.zero
		// Case: Multiplying zero => 0
		else if (isZero)
			this
		// Case: Other => May delegate multiplication to a Scala instance
		else
			lengthOrScala match {
				case Right((length, unit)) => Duration(length * mod, unit)
				case Left(scala) => Duration(scala * mod)
			}
	}
	override def /(divider: Double) = {
		// Case: Division by zero => Yields infinite
		if (divider == 0)
			Duration.infiniteOf(sign)
		// Case: Division of zero => zero
		else if (isZero)
			this
		else
			tryLength match {
				case Success((length, unit)) => Duration(length / divider, unit)
				case _ => Duration.infiniteOf(sign * Sign.of(divider))
			}
	}
	
	override def zero: Duration = Duration.zero
	
	override def toString: String = tryLength match {
		case Success((length, unit)) => s"$length $unit"
		case _ =>
			sign match {
				case Positive => "infinite"
				case Negative => "-inf"
				case Neutral => "undefined"
			}
	}
	
	
	// OTHER    ------------------------------
	
	/**
	 * @param unit Targeted unit
	 * @throws java.lang.ArithmeticException If this duration is infinite or undefined
	 * @return Number of instances of the specified unit in this duration, rounded down
	 */
	@throws[ArithmeticException]("If this duration is infinite or undefined")
	def to(unit: TimeUnit): Long = {
		val (length, myUnit) = this.length
		unit.countIn(length, myUnit)
	}
	/**
	 * @param unit Targeted unit
	 * @throws java.lang.ArithmeticException If this duration is infinite or undefined
	 * @return Number of instances of the specified unit in this duration, in decimal precision
	 */
	@throws[ArithmeticException]("If this duration is infinite or undefined")
	def toPrecise(unit: TimeUnit): Double = {
		val (length, myUnit) = this.length
		unit.countPreciselyIn(length.toDouble, myUnit)
	}
	/**
	 * Calculates the remainder of some unit conversion and converts it to a specific unit.
	 * @param in Unit in which the result is given
	 * @param after Unit for which the remainder is calculated
	 * @throws java.lang.ArithmeticException If this duration is infinite or undefined
	 * @return The remainder of this duration (in 'in'), after it has been rounded to the specified unit ('after')
	 */
	@throws[ArithmeticException]("If this duration is infinite or undefined")
	def toRemainder(in: TimeUnit, after: TimeUnit) = {
		val (length, myUnit) = this.length
		in.countRemainderFrom(length, myUnit, after)
	}
	
	/**
	 * @param unit Unit in which the result is given
	 * @return Number of instances of the specified unit in this duration, rounded down.
	 *         None if this duration was infinite or undefined.
	 */
	def toFinite(unit: TimeUnit): Option[Long] =
		tryLength.toOption.map { case (length, myUnit) => unit.countIn(length, myUnit) }
	/**
	 * @param unit Unit in which the result is given
	 * @return Number of instances of the specified unit in this duration, in decimal precision.
	 *         None if this duration was infinite or undefined.
	 */
	def toPreciseFinite(unit: TimeUnit): Option[Double] =
		tryLength.toOption.map { case (length, myUnit) => unit.countPreciselyIn(length.toDouble, myUnit) }
	
	/**
	 * Rounds this duration to the specified unit
	 * @param unit Lowest applied unit of precision
	 * @return This unit in no higher precision than the one specified
	 */
	def roundTo(unit: TimeUnit) = tryLength match {
		case Success((length, myUnit)) =>
			if (myUnit >= unit) this else new Duration(Right(unit.countPreciselyIn(length, myUnit).round -> unit))
		case _ => this
	}
	
	/**
	 * Multiplies this duration
	 * @param multiplier Applied multiplier
	 * @return A multiplied copy of this duration
	 */
	def *(multiplier: Int): Duration = this * (multiplier: Long)
	/**
	 * Multiplies this duration
	 * @param multiplier Applied multiplier
	 * @return A multiplied copy of this duration
	 */
	def *(multiplier: Long) = {
		// Case: 0 * X = 0
		if (isZero)
			this
		// Case: X * 0 = 0
		else if (multiplier == 0)
			Duration.zero
		else
			optimized { (length, unit) => Duration(length * multiplier, unit) } { _ * multiplier.toDouble } {
				_.multipliedBy(multiplier) } { Duration.infiniteOf(sign * Sign.of(multiplier)) }
	}
	/**
	 * Calculates the ratio between two durations
	 * @param divider Another duration
	 * @throws java.lang.ArithmeticException If dividing by zero
	 * @return The ratio between these durations
	 */
	@throws[ArithmeticException]("If dividing by zero")
	def /(divider: Duration): Double = {
		// Case: Division by zero => Throws
		if (divider.isZero)
			throw new ArithmeticException("Division by zero")
		// Case: 0 / X = 0
		else if (isZero)
			0
		else
			// Java duration doesn't provide a division function, so we'll convert that to a length value first
			mergeWithExcludingJava(divider) { (length, div, _) => length / div.toDouble } { _.merge { _ / _ } } {
				if (isInfinite) {
					if (divider.isInfinite || sign == Neutral)
						NaN
					// Infinite divided by a non-infinite value is still infinite
					else if (sign == Positive)
						Double.PositiveInfinity
					else
						Double.NegativeInfinity
				}
				// A non-infinite value divided by an infinite value is practically 0
				else
					0.0
			}
	}
	
	/**
	 * @param unit A threshold unit, after which the remainder is calculated
	 * @return The remainder of this duration after rounding to a specific unit precision
	 */
	def %(unit: TimeUnit) = tryLength match {
		case Success((length, myUnit)) =>
			val remainder = myUnit.countRemainderFrom(length, myUnit, unit)
			Duration(remainder, myUnit)
		case _ => Duration.undefined
	}
	/**
	 * @param divider A dividing duration
	 * @return Remainder of this duration, after being divided by the specified duration
	 */
	def %(divider: Duration) = Duration.mergeLengthsOf(Pair(this, divider)) {
		(length, divider, unit) => Duration(length % divider, unit) } {
		if (isInfinite) Duration.undefined else Duration.zero }
	
	/**
	 * @param instant Origin instant
	 * @return An instant 'this' before the origin instant
	 */
	def before[T](instant: CanAppendJavaDuration[T]) = instant - this
	/**
	 * @param instant Origin instant
	 * @return An instant 'this' after the origin instant
	 */
	def after[T](instant: CanAppendJavaDuration[T]) = instant + this
	
	/**
	 * @param threshold A time threshold
	 * @return Whether this duration has passed since that time threshold
	 */
	def hasPassedSince(threshold: Instant) = if (isFinite) Now >= threshold + this else isNegative
	/**
	 * @param threshold A time threshold
	 * @return Whether this duration has passed since that time threshold
	 */
	def hasPassedSince(threshold: LocalDateTime) = if (isFinite) Now.toLocalDateTime >= threshold + this else isNegative
	
	/**
	 * Performs a calculation in the optimal format, based on the wrapped values
	 * @param fromLength A function for calculating the result based on the length of this duration
	 * @param fromScala A function for calculating the result based on a Scala instance
	 * @param fromJava A function for calculating this result based on a Java instance
	 * @param onInfinite A function called if this duration is infinite
	 * @tparam A Type of the acquired result
	 * @return Result of one of the specified functions
	 */
	private def optimized[A](fromLength: (Long, TimeUnit) => A)(fromScala: SDuration => A)(fromJava: JDuration => A)
	                        (onInfinite: => A) =
	{
		if (isFinite)
			lazyLength.current match {
				// Case: Length immediately available => Calls 'fromLength' or 'onInfinite'
				case Some(length) =>
					length match {
						case Success((length, unit)) => fromLength(length, unit)
						case _ => onInfinite
					}
				case None =>
					lazyScala.current match {
						// Case: Scala version immediately available => Calls 'fromScala'
						case Some(duration) => fromScala(duration)
						case None =>
							lazyJava.current match {
								// Case: Java version immediately available
								//       => Calls 'fromJava' or 'onInfinite' (unexpected)
								case Some(duration) =>
									duration match {
										case Success(duration) => fromJava(duration)
										case _ => onInfinite
									}
								// Case: No value available (unexpected)
								//       => Acquires the length and calls 'fromLength' or 'onInfinite'
								case None =>
									tryLength match {
										case Success((length, unit)) => fromLength(length, unit)
										case _ => onInfinite
									}
							}
					}
			}
		// Case: This duration is infinite => Calls 'onInfinite'
		else
			onInfinite
	}
	
	/**
	 * Merges this duration with another, using the optimal format.
	 * However, won't consider the Java format.
	 * @param other Another duration
	 * @param mergeLengths A function for merging the length values
	 * @param mergeAsScala A function for merging two Scala representations
	 * @param resultOnInfinite A function called if either duration is infinite
	 * @tparam A Type of function results
	 * @return Result of one of the specified functions
	 */
	private def mergeWithExcludingJava[A](other: Duration)
	                                     (mergeLengths: (Long, Long, TimeUnit) => A)
	                                     (mergeAsScala: Pair[SDuration] => A)
	                                     (resultOnInfinite: => A) =
	{
		val parties = Pair(this, other)
		Duration.tryMergeLengthsOf(parties)(mergeLengths)(resultOnInfinite) {
			parties.findForBoth { _.lazyScala.current }.map(mergeAsScala)
		}
	}
	/**
	 * Merges this duration with another, using the optimal format.
	 * @param other Another duration
	 * @param mergeLengths A function for merging the length values
	 * @param mergeAsScala A function for merging two Scala representations
	 * @param mergeAsJava A function for merging two Java representations
	 * @param resultOnInfinite A function called if either duration is infinite
	 * @tparam A Type of function results
	 * @return Result of one of the specified functions
	 */
	private def mergeWith[A](other: Duration)(mergeLengths: (Long, Long, TimeUnit) => A)
	                        (mergeAsScala: Pair[SDuration] => A)
	                        (mergeAsJava: Pair[JDuration] => A)
	                        (resultOnInfinite: => A) =
	{
		val parties = Pair(this, other)
		Duration.tryMergeLengthsOf(parties)(mergeLengths)(resultOnInfinite) {
			parties.findForBoth { _.lazyScala.current } match {
				case Some(scalaVersions) => Some(mergeAsScala(scalaVersions))
				case None =>
					parties.findForBoth { _.lazyJava.current }.map { javaVersions =>
						javaVersions.findForBoth { _.toOption } match {
							case Some(javaVersions) => mergeAsJava(javaVersions)
							case None => resultOnInfinite
						}
					}
			}
		}
	}
}
