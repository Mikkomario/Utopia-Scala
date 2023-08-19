package utopia.flow.util

import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.operator.ComparisonOperator.{Always, DirectionalComparison, Equality, Inequality, Never}
import utopia.flow.operator.Extreme.{Max, Min}
import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.flow.operator.SignOrZero.Neutral
import utopia.flow.operator.UncertainSign.{CertainSign, NotNegative}
import utopia.flow.operator._
import utopia.flow.util.UncertainBoolean.CertainBoolean
import utopia.flow.view.immutable.View

import scala.math.Numeric.Implicits.infixNumericOps
import scala.language.implicitConversions
import scala.math.Ordered.orderingToOrdered

/**
  * Common trait for numbers that may be known or unknown, or known only partially, or as a range of possible values.
  * @author Mikko Hilpinen
  * @since 18.8.2023, v2.2
  * @tparam N Type of numeric values used
  */
sealed trait UncertainNumber[N]
	extends Uncertain[N] with Reversible[UncertainNumber[N]] with Scalable[N, UncertainNumber[N]]
		with Combinable[UncertainNumber[N], UncertainNumber[N]] with HasUncertainSign
{
	// ABSTRACT ------------------------
	
	/**
	  * @return Numeric functions -implementation for this trait's numeric type
	  */
	protected implicit def n: Numeric[N]
	
	/**
	  * @return The absolute value of this number
	  */
	def abs: UncertainNumber[N]
	/**
	  * @return The negative absolute value of this number
	  */
	def negativeAbs: UncertainNumber[N]
	
	/**
	  * @param other Another (certain) number
	  * @return Copy of this number increased by the specified amount (result may be uncertain)
	  */
	def +(other: N): UncertainNumber[N]
	
	/**
	  * @param other Another uncertain number
	  * @return Whether these two numbers are, or may be, the same
	  */
	def ==(other: UncertainNumber[N]): UncertainBoolean
	
	/**
	  * @param extreme Targeted extreme
	  * @return The most extreme possible finite value.
	  *         None if the most extreme value is unknown and could be positive or negative infinity.
	  */
	def options(extreme: Extreme): Option[N]
	/**
	  * @param other Another number
	  * @param extreme Targeted extreme
	  * @return If 'extreme' is Max, returns the larger of these two numbers.
	  *         For Min, returns the smaller of these two numbers.
	  */
	def minOrMax(other: N, extreme: Extreme): UncertainNumber[N]
	
	/**
	  * @param formatting A formatting function that converts (threshold) values into strings
	  * @return A string representation of this number
	  */
	def toStringWith(formatting: N => String): String
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return The largest possible value of this number.
	  *         None if the largest potential number is positive infinity.
	  */
	def largestPossibleValue = options(Max)
	/**
	  * @return The smallest possible value of this number.
	  *         None of the smallest potential number is negative infinity.
	  */
	def smallestPossibleValue = options(Min)
	
	/**
	  * Alias for max(0)
	  * @return This number if larger than zero.
	  *         If this number is less than or equal to zero, returns zero.
	  */
	def nonNegative = max(n.zero)
	/**
	  * Alias for min(0)
	  * @return This number if smaller than zero.
	  *         If this number is larger than or equal to zero, returns zero.
	  */
	def nonPositive = min(n.zero)
	
	
	// IMPLEMENTED  ----------------------
	
	override def self: UncertainNumber[N] = this
	
	override def toString = toStringWith { _.toString }
	
	
	// OTHER    -------------------------
	
	/**
	  * @param other Another uncertain number
	  * @return Whether these numbers are not equal (may be uncertain)
	  */
	def !=(other: UncertainNumber[N]) = !(this == other)
	
	/**
	  * @param other Another number
	  * @return The smaller of these two numbers
	  */
	def min(other: N) = minOrMax(other, Min)
	/**
	  * @param other Another number
	  * @return The larger of these two numbers
	  */
	def max(other: N) = minOrMax(other, Max)
}

object UncertainNumber
{
	// TYPES    ----------------------
	
	/**
	  * Type of uncertain integer values
	  */
	type UncertainInt = UncertainNumber[Int]
	/**
	  * Type of uncertain double number values
	  */
	type UncertainDouble = UncertainNumber[Double]
	
	
	// IMPLICIT ----------------------
	
	implicit def certainToUncertain(d: Double): UncertainDouble = CertainNumber(d)
	implicit def certainToUncertain(i: Int): UncertainInt = CertainNumber(i)
	implicit def rangeToUncertain[N](range: NumericSpan[N]): UncertainNumber[N] = UncertainNumberRange(range)
	
	
	// COMPUTED   --------------------
	
	/**
	  * @param n Implicit numeric implementation
	  * @tparam N Type of numeric values used
	  * @return An uncertain number that may be any actual number
	  */
	def any[N](implicit n: Numeric[N]) = AnyNumber[N]()
	/**
	  * @param n Implicit numeric implementation
	  * @tparam N Type of numeric values used
	  * @return An uncertain number that may be any positive (> 0) number
	  */
	def positive[N](implicit n: Numeric[N]) = NumbersWithSign(Positive)
	/**
	  * @param n Implicit numeric implementation
	  * @tparam N Type of numeric values used
	  * @return An uncertain number that may be any negative (< 0) number
	  */
	def negative[N](implicit n: Numeric[N]) = NumbersWithSign(Negative)
	
	
	// OTHER    ------------------------
	
	/**
	  * @param certainNumber A number
	  * @param n Implicit numeric implementation
	  * @tparam N Type of numeric values used
	  * @return That (certain) number as an uncertain number
	  */
	def apply[N](certainNumber: N)(implicit n: Numeric[N]) = CertainNumber(certainNumber)
	/**
	  * @param number A number
	  * @param n      Implicit numeric implementation
	  * @tparam N Type of numeric values used
	  * @return An uncertain number that may be any number other than the specified number
	  */
	def not[N](number: N)(implicit n: Numeric[N]) = NumberComparison(number, Inequality)
	/**
	  * @param number A number
	  * @param orEqual Whether the specified number is a possible actual number (default = false)
	  * @param n Implicit numeric implementation
	  * @tparam N Type of numeric values used
	  * @return An uncertain number that may be any number smaller than (or possibly equal to) the specified number
	  */
	def lessThan[N](number: N, orEqual: Boolean = false)(implicit n: Numeric[N]) =
		NumberComparison(number, DirectionalComparison(Negative, orEqual))
	/**
	  * @param number  A number
	  * @param orEqual Whether the specified number is a possible actual number (default = false)
	  * @param n       Implicit numeric implementation
	  * @tparam N Type of numeric values used
	  * @return An uncertain number that may be any number larger than (or possibly equal to) the specified number
	  */
	def greaterThan[N](number: N, orEqual: Boolean = false)(implicit n: Numeric[N]) =
		NumberComparison(number, DirectionalComparison(Positive, orEqual))
	/**
	  * @param number  A number
	  * @param extreme Targeted extreme, where Min indicates all smaller numbers and Max indicates all larger numbers
	  * @param includeEqual Whether the specified number is a possible actual number (default = false)
	  * @param n       Implicit numeric implementation
	  * @tparam N Type of numeric values used
	  * @return An uncertain number that may be any number more extreme than (or possibly equal to) the specified number
	  */
	def moreExtremeThan[N](number: N, extreme: Extreme, includeEqual: Boolean = false)(implicit n: Numeric[N]) =
		NumberComparison(number, DirectionalComparison(Sign(extreme), includeEqual))
	/**
	  * @param range Range of possible numbers
	  * @tparam N Type of numeric values used
	  * @return An uncertain number that may be any one of the numbers within that range
	  */
	def within[N](range: NumericSpan[N]) = UncertainNumberRange(range)
	
	
	// NESTED   ------------------------
	
	case class AnyNumber[N]()(implicit override val n: Numeric[N]) extends UncertainNumber[N]
	{
		override def exact: Option[N] = None
		override def sign: UncertainSign = UncertainSign
		
		override def unary_- : UncertainNumber[N] = this
		override def abs: UncertainNumber[N] = this
		override def negativeAbs: UncertainNumber[N] = this
		
		override def mayBe(v: N): Boolean = true
		
		// Anything times zero is zero
		override def *(mod: N): UncertainNumber[N] =
			if (mod == n.zero) CertainNumber(n.zero) else this
		
		override def +(other: N): UncertainNumber[N] = this
		override def +(other: UncertainNumber[N]): UncertainNumber[N] = this
		
		override def ==(other: UncertainNumber[N]): UncertainBoolean = UncertainBoolean
		
		override def options(extreme: Extreme): Option[N] = None
		
		// Limits the number to an open range
		override def minOrMax(other: N, extreme: Extreme): UncertainNumber[N] =
			NumberComparison(other, DirectionalComparison(Sign(extreme.opposite), includesEqual = true))
		
		override def toStringWith(formatting: N => String): String = "?"
	}
	
	case class CertainNumber[N](value: N)(implicit override val n: Numeric[N]) extends UncertainNumber[N] with View[N]
	{
		override lazy val sign: UncertainSign = Sign.of(value)
		
		override def exact: Option[N] = Some(value)
		
		override def unary_- : UncertainNumber[N] = copy(-value)
		override def abs: UncertainNumber[N] = copy(value.abs)
		override def negativeAbs: UncertainNumber[N] = copy(-value.abs)
		
		override def mayBe(v: N): Boolean = v == value
		
		override def *(mod: N): UncertainNumber[N] = copy(value * mod)
		
		override def +(other: N): UncertainNumber[N] = CertainNumber(value + other)
		override def +(other: UncertainNumber[N]): UncertainNumber[N] = {
			// Case: Adding anything to zero => Same as returning the other number
			if (value == n.zero)
				other
			else {
				other match {
					// Case: Certain + Certain => Certain
					case CertainNumber(number) => this + number
					// Case: Certain + Number with sign => Open range
					case NumbersWithSign(otherSign) => NumberComparison(value, DirectionalComparison(otherSign))
					// Case: Certain + Range => Range
					case UncertainNumberRange(range) => UncertainNumberRange(range.mapEnds { value + _ })
					// Case: Certain + Comparison => Modified comparison
					case NumberComparison(t, operator) => NumberComparison(value + t, operator)
					case _ => AnyNumber()
				}
			}
		}
		
		override def ==(other: UncertainNumber[N]): UncertainBoolean = other == value
		
		override def options(extreme: Extreme): Option[N] = Some(value)
		
		// Simply calculates min/max
		override def minOrMax(other: N, extreme: Extreme): UncertainNumber[N] = copy(extreme(value, other))
		
		override def toStringWith(formatting: N => String): String = formatting(value)
	}
	
	case class NumbersWithSign[N](targetSign: Sign)(implicit override val n: Numeric[N]) extends UncertainNumber[N]
	{
		// COMPUTED -------------------------
		
		/**
		  * @return A number that may be any of these numbers or zero
		  */
		def orZero =
			NumberComparison(n.zero, DirectionalComparison(targetSign, includesEqual = true))
		
		
		// IMPLEMENTED  ---------------------
		
		override def exact: Option[N] = None
		override def sign: UncertainSign = targetSign
		
		override def unary_- : UncertainNumber[N] = copy(-targetSign)
		override def abs: UncertainNumber[N] = copy(Positive)
		override def negativeAbs: UncertainNumber[N] = copy(Negative)
		
		override def mayBe(v: N): Boolean = Sign.of(v) == targetSign
		
		override def *(mod: N): UncertainNumber[N] = Sign.of(mod) match {
			case s: Sign => copy(s * targetSign)
			case Neutral => CertainNumber(n.zero)
		}
		
		override def +(other: N): UncertainNumber[N] = {
			// Case: Combining two numbers of different sizes with uncertain difference =>
			// Can't even determine the sign anymore
			if (Sign.of(other) == targetSign.opposite)
				new AnyNumber[N]()
			// Case: Increasing or keeping the sign => Converts into a comparison
			else
				NumberComparison(other, DirectionalComparison(targetSign))
		}
		override def +(other: UncertainNumber[N]): UncertainNumber[N] = other match {
			// Case: Other number is certain => Uses the other method
			case CertainNumber(number) => this + number
			case _ =>
				val signOfAddition = other.sign
				// Case: Mixing signs => Can't determine resulting number
				if (signOfAddition.mayBe(targetSign.opposite))
					AnyNumber[N]()
				// Case: Addition by zero => Remains the same
				else if (signOfAddition.isCertainlyNeutral)
					this
				else
					other match {
						// Case: Combining with a number comparison (equality or directional)
						case NumberComparison(t, operator) =>
							operator match {
								// Case: Equality => Becomes directional
								case Equality => NumberComparison(t, DirectionalComparison(targetSign))
								// Case: Directional => Removes the option of equality
								case _: DirectionalComparison => NumberComparison(t, DirectionalComparison(targetSign))
								case _ => this
							}
						// Case: Combining with a range of numbers => Converts to a directional open range
						case UncertainNumberRange(range) =>
							NumberComparison(range(targetSign.opposite.extreme), DirectionalComparison(targetSign))
						case _ => this
					}
		}
		
		override def ==(other: UncertainNumber[N]): UncertainBoolean = {
			if (other.sign.mayBe(targetSign))
				UncertainBoolean
			else
				CertainBoolean(false)
		}
		
		override def options(extreme: Extreme): Option[N] = if (Sign(extreme) == targetSign) None else Some(n.zero)
		
		override def minOrMax(other: N, extreme: Extreme): UncertainNumber[N] = {
			// Case: This range may contain a more extreme item => Limits the less extreme side
			if (targetSign.extreme == extreme)
				NumberComparison(other, DirectionalComparison(targetSign, includesEqual = true))
			// Case: The most extreme number in this range is next to zero
			else {
				val otherSign = Sign.of(other)
				// Case: The other number is also on the "wrong" side of the extreme => Returns a smaller range
				if (otherSign == targetSign)
					UncertainNumberRange(n.zero, other)
				// Case: The other number is known to be more extreme => Returns that
				else
					CertainNumber(other)
			}
		}
		
		override def toStringWith(formatting: N => String): String = {
			val cmp = targetSign match {
				case Positive => '>'
				case Negative => '<'
			}
			s"$cmp ${formatting(n.zero)}"
		}
	}
	
	/**
	  * An uncertain number represented with a comparison operator.
	  * For example, may represent "any number larger than 3"
	  * @param threshold The threshold number used for comparing acceptable values
	  * @param operator The comparison operator used to identify acceptable values
	  * @param n Implicit numeric implementation
	  * @tparam N Type of numeric values used
	  */
	case class NumberComparison[N](threshold: N, operator: ComparisonOperator)(implicit override val n: Numeric[N])
		extends UncertainNumber[N]
	{
		// ATTRIBUTES   ----------------
		
		override lazy val sign: UncertainSign = operator match {
			// Case: Exact mode => Knows sign for sure
			case Equality => Sign.of(threshold)
			// Case: One-directional comparison => Checks whether sign may be determined
			case DirectionalComparison(requiredDirection, includesEqual) =>
				val thresholdSign = Sign.of(threshold)
				// Case: Comparing to 0 => Checks whether 0 should be included as a possibility
				if (thresholdSign == Neutral) {
					if (includesEqual)
						NotNegative
					else
						requiredDirection
				}
				// Case: Sign never changes
				else if (thresholdSign == requiredDirection)
					requiredDirection
				// Case: This range spans all signs
				else
					UncertainSign
			// Case: Non-sign-specific comparison style => Can't determine a sign
			case _ => UncertainSign
		}
		
		
		// IMPLEMENTED  ----------------
		
		override def exact: Option[N] = if (operator == Equality) Some(threshold) else None
		
		override def unary_- : UncertainNumber[N] = copy(-threshold, -operator)
		override def abs: UncertainNumber[N] = _abs(Positive)
		override def negativeAbs: UncertainNumber[N] = _abs(Negative)
		
		override def mayBe(v: N): Boolean = operator(v, threshold)(n)
		
		override def *(mod: N): UncertainNumber[N] = {
			Sign.of(mod) match {
				// Case: Multiplying with a non-zero value => May flip the condition
				case modSign: Sign => copy(threshold * mod, operator * modSign)
				// Case: Multiplying with 0 => Always 0, no matter the number
				case Neutral => CertainNumber(n.zero)
			}
		}
		
		override def +(other: N): UncertainNumber[N] = copy(threshold = threshold + other)
		override def +(other: UncertainNumber[N]): UncertainNumber[N] = {
			other.exact match {
				// Case: Other number is exact => Adjusts the threshold only
				case Some(number) => this + number
				case None =>
					operator match {
						// Case: This number is exact => Delegates to CertainNumber
						case Equality => CertainNumber(threshold) + other
						// Case: This is an open range
						case DirectionalComparison(direction, includesEqual) =>
							other match {
								case NumbersWithSign(sign) =>
									// Case: Adding some number towards the open direction => Removes equality option
									if (direction == sign)
										copy(operator = DirectionalComparison(direction))
									// Case: Adding some number against the open direction => Can't determine the result
									else
										AnyNumber()
								// Case: Open range + closed range => Adjusts the closed side
								case UncertainNumberRange(range) =>
									val minAdjust = range(direction.opposite.extreme)
									copy(threshold = threshold + minAdjust)
								case NumberComparison(t, otherOperator) =>
									otherOperator match {
										case DirectionalComparison(otherDirection, otherIncludesEqual) =>
											// Case: Combining two open ranges of same direction => Adjusts the closed side
											if (direction == otherDirection)
												NumberComparison(threshold + t,
													DirectionalComparison(direction, includesEqual || otherIncludesEqual))
											else
												AnyNumber()
										case _ => AnyNumber()
									}
								case _ => AnyNumber()
							}
						case _ => AnyNumber()
					}
			}
		}
		
		override def ==(other: UncertainNumber[N]): UncertainBoolean = operator match {
			case Equality => other == threshold
			case Inequality => other != threshold
			case Always => UncertainBoolean
			case Never => CertainBoolean(false)
			case DirectionalComparison(dir, includesEqual) =>
				other.options(dir.extreme) match {
					case Some(v) =>
						// Checks whether the most extreme option is more or as extreme as the threshold value
						val ord = dir * n
						val cmp = ord.compare(v, threshold)
						if (cmp > 0 || (cmp == 0 && includesEqual))
							UncertainBoolean
						else
							CertainBoolean(false)
					case None => UncertainBoolean
				}
		}
		
		override def options(extreme: Extreme): Option[N] = operator match {
			case Equality => Some(threshold)
			case DirectionalComparison(direction, _) => if (direction.extreme == extreme) None else Some(threshold)
			case _ => None
		}
		
		override def minOrMax(other: N, extreme: Extreme): UncertainNumber[N] = operator match {
			// Case: Exact number => Calculates min/max
			case Equality => CertainNumber(extreme(threshold, other))
			// Case: Very uncertain number => Limits to an open range of numbers
			case Always | Never | Inequality =>
				NumberComparison(other, DirectionalComparison(Sign(extreme).opposite, includesEqual = true))
			// Case: Open range
			case DirectionalComparison(direction, _) =>
				// Case: This range may contain the more extreme value => Limits the less extreme side
				if (direction.extreme == extreme)
					NumberComparison(extreme(threshold, other), DirectionalComparison(direction, includesEqual = true))
				// Case: The specified threshold is the most extreme value available
				else {
					val diffSign = Sign.of(other - threshold)
					// Case: The other number is within the range of possible values => Limits to a closed range
					if (diffSign == direction)
						UncertainNumberRange(other, threshold)
					// Case: The other number is more extreme than any available number here => Returns it
					else
						CertainNumber(other)
				}
		}
		
		override def toStringWith(formatting: N => String): String = operator match {
			case Equality => formatting(threshold)
			case Inequality => s"not ${formatting(threshold)}"
			case Always => "?"
			case Never => "NaN"
			case DirectionalComparison(dir, includesEqual) =>
				val cmp = dir match {
					case Positive => '>'
					case Negative => '<'
				}
				s"$cmp${if(includesEqual) "=" else ""} ${formatting(threshold)}"
		}
		
		
		// OTHER    -------------------------
		
		private def _abs(sign: Sign): UncertainNumber[N] = operator match {
			// Case: Covers all numbers => Now only covers one side of numbers
			case Always => NumbersWithSign(sign).orZero
			// Case: Covers no numbers => Remains the same
			case Never => this
			// Case: Covers a single number => Converts that number to the correct sign
			case Equality => CertainNumber(sign * threshold.abs)
			// Case: Covers all but a single number => Covers every number on the targeted side
			case Inequality =>
				Sign.of(threshold) match {
					case Positive | Negative =>
						NumberComparison(n.zero, DirectionalComparison(sign, includesEqual = true))
					// Case: Excluded number was 0 => Continues to exclude it
					case Neutral => NumbersWithSign(sign)
				}
			// Case: Covers a one-sided range
			case DirectionalComparison(direction, includesEqual) =>
				val nonZero = NumbersWithSign(sign)
				val tSign = Sign.of(threshold)
				// Case: Range started from zero => Moves the range to the correct side
				if (tSign == Neutral) {
					if (includesEqual) nonZero.orZero else nonZero
				}
				// Case: Range didn't cover every number of any side => Continues that trend on the right side
				else if (tSign == direction)
					NumberComparison(sign * threshold.abs, DirectionalComparison(sign, includesEqual))
				// Case: Range covered every number on one side => Returns half of the available numbers + zero
				else
					nonZero.orZero
		}
	}
	
	object UncertainNumberRange
	{
		/**
		  * @param start The first possible value
		  * @param end The last possible value
		  * @param n Implicit numeric implementation
		  * @tparam N Number type
		  * @return A number that may be anything between the specified two values
		  */
		def apply[N](start: N, end: N)(implicit n: Numeric[N]): UncertainNumberRange[N] = apply(NumericSpan(start, end))
	}
	/**
	  * An uncertain number from a range of possible numbers
	  * @param range The range of possible values
	  * @tparam N Type of numeric values used
	  */
	case class UncertainNumberRange[N](range: NumericSpan[N]) extends UncertainNumber[N]
	{
		// ATTRIBUTES   --------------
		
		override lazy val sign: UncertainSign = {
			// Checks the covered number signs
			val signs = range.toPair.map { Sign.of(_) }.toSet
			// Case: Only one sign is covered => Sign is certain
			if (signs.size == 1)
				signs.head
			// Case: All number signs are covered => Sign is fully uncertain
			else if (Sign.values.forall(signs.contains))
				UncertainSign
			// Case: Covers neutral and one other sign => Combines them
			else
				signs.map { CertainSign(_): UncertainSign }.reduceLeft { _ || _ }
		}
		
		
		// IMPLEMENTED  --------------
		
		override protected implicit def n: Numeric[N] = range.n
		
		override def exact: Option[N] = range.only
		
		override def mayBe(v: N): Boolean = range.contains(v)
		
		override def unary_- : UncertainNumber[N] = map { -_ }
		override def abs: UncertainNumber[N] = {
			if (range.toPair.isAsymmetricBy { _.sign })
				UncertainNumberRange(NumericSpan(n.zero, range.mapEnds { _.abs }.max))
			else
				map { n.abs(_) }
		}
		override def negativeAbs: UncertainNumber[N] = -abs
		
		override def *(mod: N): UncertainNumber[N] = copy(range * mod)
		
		override def +(other: N): UncertainNumber[N] = map { _ + other }
		override def +(other: UncertainNumber[N]): UncertainNumber[N] = other match {
			// Case: Adding a certain amount => Uses the other method
			case CertainNumber(number) => this + number
			// Case: Adding two ranges of numbers => Yields a range
			case UncertainNumberRange(r) =>
				val newRange = NumericSpan[N](range.minMax.mergeWith(r.minMax) { _ + _ })
				UncertainNumberRange(newRange)
			case NumberComparison(t, operator) =>
				operator match {
					// Case: Adding an exact number => Uses the other method
					case Equality => this + t
					// Case: Adding an open range of numbers => Yields another open range with a modified threshold
					case c: DirectionalComparison =>
						NumberComparison(t + range(c.requiredDirection.opposite.extreme), c)
					case _ => AnyNumber()
				}
			case _ => AnyNumber()
		}
		
		override def ==(other: UncertainNumber[N]): UncertainBoolean = exact match {
			case Some(exact) => other == exact
			case None =>
				other.exact match {
					case Some(exact) => if (range.contains(exact)) UncertainBoolean else CertainBoolean(false)
					case None =>
						other match {
							case UncertainNumberRange(r2) =>
								if (range.overlapsWith(r2)) UncertainBoolean else CertainBoolean(false)
							// Compares other types in their own methods,
							// because determining the edge values is somewhat complicated
							case _ => other == this
						}
				}
		}
		
		override def options(extreme: Extreme): Option[N] = Some(range(extreme))
		
		override def minOrMax(other: N, extreme: Extreme): UncertainNumber[N] = {
			val ordered = range.towards(extreme)
			val ordering = extreme.ascendingToExtreme(n)
			
			// Case: The specified number is more extreme than any number in this range => Returns the specified number
			if (ordering.gteq(other, ordered.end))
				CertainNumber(other)
			// Case: The specified number is less extreme than any number in this range => Returns this range
			else if (ordering.lteq(other, ordered.start))
				this
			// Case: Specified number lies within this range => Limits to the more extreme side of that number
			else
				UncertainNumberRange(other, ordered.end)
		}
		
		override def toStringWith(formatting: N => String): String =
			range.ascending.toPair.map(formatting).mkString(" - ")
		
		
		// OTHER    -----------------------
		
		private def map(f: N => N) = UncertainNumberRange(range.mapEnds(f))
	}
}