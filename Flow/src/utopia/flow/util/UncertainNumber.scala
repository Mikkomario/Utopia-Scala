package utopia.flow.util

import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.operator.ComparisonOperator.{Always, DirectionalComparison, Equality, Inequality, Never}
import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.flow.operator.SignOrZero.Neutral
import utopia.flow.operator.UncertainSign.NotNegative
import utopia.flow.operator.{ComparisonOperator, Reversible, Scalable, Sign, Uncertain, UncertainSign}
import utopia.flow.view.immutable.View

import scala.math.Numeric.Implicits.infixNumericOps

/**
  * Common trait for numbers that may be known or unknown, or known only partially, or as a range of possible values.
  * @author Mikko Hilpinen
  * @since 18.8.2023, v2.2
  * @tparam N Type of numeric values used
  */
// TODO: Review abs and - implementation
trait UncertainNumber[N] extends Uncertain[N] with Reversible[UncertainNumber[N]] with Scalable[N, UncertainNumber[N]]
{
	// ABSTRACT ------------------------
	
	/**
	  * @return Numeric functions -implementation for this trait's numeric type
	  */
	protected implicit def n: Numeric[N]
	
	/**
	  * @return The sign of this number (may be uncertain)
	  */
	def sign: UncertainSign
	
	/**
	  * @return The absolute value of this number
	  */
	def abs: UncertainNumber[N]
	/**
	  * @return The negative absolute value of this number
	  */
	def negativeAbs: UncertainNumber[N]
	
	
	// IMPLEMENTED  ----------------------
	
	override def self: UncertainNumber[N] = this
}

object UncertainNumber
{
	// COMPUTED   --------------------
	
	def positive[N](implicit n: Numeric[N]) = NumbersWithSign(Positive)
	def negative[N](implicit n: Numeric[N]) = NumbersWithSign(Negative)
	
	
	// NESTED   ------------------------
	
	class AnyNumber[N]()(implicit override val n: Numeric[N]) extends UncertainNumber[N]
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
	}
	
	case class CertainNumber[N](value: N)(implicit override val n: Numeric[N]) extends UncertainNumber[N] with View[N]
	{
		override def exact: Option[N] = Some(value)
		override def sign: UncertainSign = Sign.of(value)
		
		override def unary_- : UncertainNumber[N] = copy(-value)
		override def abs: UncertainNumber[N] = copy(value.abs)
		override def negativeAbs: UncertainNumber[N] = copy(-value.abs)
		
		override def mayBe(v: N): Boolean = v == value
		
		override def *(mod: N): UncertainNumber[N] = copy(value * mod)
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
		// IMPLEMENTED  ----------------
		
		override def exact: Option[N] = if (operator == Equality) Some(threshold) else None
		
		override def sign: UncertainSign = operator match {
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
	
	/**
	  * An uncertain number from a range of possible numbers
	  * @param range The range of possible values
	  * @tparam N Type of numeric values used
	  */
	case class UncertainNumberRange[N](range: NumericSpan[N]) extends UncertainNumber[N]
	{
		// IMPLEMENTED  --------------
		
		override protected implicit def n: Numeric[N] = range.n
		
		override def exact: Option[N] = range.only
		override def sign: UncertainSign = range.toPair.map { Sign.of(_) }.merge { _ || _ }
		
		override def mayBe(v: N): Boolean = range.contains(v)
		
		override def unary_- : UncertainNumber[N] = copy(range.mapEnds { -_ })
		override def abs: UncertainNumber[N] = {
			if (range.toPair.isAsymmetricBy { _.sign })
				UncertainNumberRange(NumericSpan(n.zero, range.max))
			else
				UncertainNumberRange(range.mapEnds { n.abs(_) })
		}
		override def negativeAbs: UncertainNumber[N] = -abs
		
		override def *(mod: N): UncertainNumber[N] = copy(range * mod)
	}
}