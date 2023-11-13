package utopia.flow.operator.sign

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.enumeration.Extreme
import utopia.flow.operator.enumeration.Extreme.{Max, Min}
import utopia.flow.operator.ordering.RichComparable
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.operator.sign.SignOrZero.Neutral
import utopia.flow.operator.sign.UncertainSign.CertainSign
import utopia.flow.operator.Reversible
import utopia.flow.operator.combine.Scalable

import scala.concurrent.duration.Duration

sealed trait SignOrZero
	extends RichComparable[SignOrZero] with Reversible[SignOrZero] with Scalable[SignOrZero, SignOrZero]
{
	// ABSTRACT -------------------------------
	
	/**
	  * @return Whether this sign is positive and not zero
	  */
	def isPositive: Boolean
	/**
	  * @return Whether this sign is negative and not zero
	  */
	def isNegative: Boolean
	/**
	  * @return Whether this sign is zero / neutral
	  */
	def isNeutral: Boolean
	
	/**
	  * @return This if positive or negative. None if neutral.
	  */
	def binary: Option[Sign]
	
	/**
	  * @param num A number
	  * @param n Numeric implementation for the specified number type
	  * @tparam N Type of numbers used
	  * @return That number with this sign, i.e:
	  *             - identity if Positive,
	  *             - reversed if Negative,
	  *             - 0 if Neutral
	  */
	def *[N](num: N)(implicit n: Numeric[N]): N
	/**
	  * @param item Item to multiply by this sign
	  * @return A modified copy of the specified item
	  */
	def *[A](item: SignedOrZero[A]): A
	
	
	// COMPUTED ---------------------------
	
	/**
	  * @return Whether this sign is not Positive. I.e. that it is negative or neutral.
	  */
	def isNotPositive = !isPositive
	/**
	  * @return Whether this sign is not Negative. I.e. that it is positive or neutral.
	  */
	def isNotNegative = !isNegative
	/**
	  * @return Whether this sign is either positive or negative
	  */
	def isNotNeutral = !isNeutral
	
	/**
	  * @return The opposite of this item. Alias for -this.
	  */
	def opposite = -this
	/**
	  * @return Not this sign (uncertain)
	  */
	def unary_! = UncertainSign.not(this)
	
	
	// IMPLEMENTED  ---------------------
	
	override def *(mod: SignOrZero): SignOrZero = mod match {
		case Neutral => Neutral
		case Positive => this
		case Negative => -this
	}
	
	
	// OTHER    -------------------------
	
	/**
	  * @param replacementForNeutral Value returned in case this is neutral / zero
	  * @return A non-neutral copy of this item.
	  */
	def binaryOr(replacementForNeutral: => Sign): Sign = binary.getOrElse(replacementForNeutral)
	
	/**
	  * @param other Another possible sign
	  * @return Either of these signs
	  */
	def ||(other: SignOrZero): UncertainSign = CertainSign(this) || other
	/**
	  * @param other Another possible sign or signs
	  * @return Any of these signs
	  */
	def ||(other: UncertainSign): UncertainSign = CertainSign(this) || other
}

object SignOrZero
{
	// ATTRIBUTES   -----------------------
	
	/**
	  * All possible values of this enumeration (Positive, Negative, Neutral)
	  */
	val values = Sign.values :+ Neutral
	
	
	// VALUES   ---------------------------
	
	/**
	  * Value for the neutral / zero case which is of neither binary sign
	  */
	case object Neutral extends SignOrZero
	{
		override def isPositive = false
		override def isNegative = false
		override def isNeutral: Boolean = true
		
		override def self = this
		override def unary_- = this
		
		override def binary: Option[Sign] = None
		
		override def compareTo(o: SignOrZero) = o match {
			case Neutral => 0
			case s: Sign => -s.modifier
		}
		
		override def *[N](num: N)(implicit n: Numeric[N]) = n.zero
		override def *[A](item: SignedOrZero[A]): A = item.zero
	}
}

/**
  * An enumeration for sign (positive or negative), which can also be used as binary direction enumeration
  * @author Mikko Hilpinen
  * @since 21.9.2021, v1.12
  */
sealed trait Sign extends SignOrZero with Reversible[Sign]
{
	// ABSTRACT	-----------------------------
	
	/**
	  * @return A modified applied to double numbers that have this direction (-1 | 1)
	  */
	def modifier: Short
	/**
	  * @return The extreme on this side
	  */
	def extreme: Extreme
	
	
	// IMPLEMENTED --------------------------
	
	override def isNeutral: Boolean = false
	override def isNegative = !isPositive
	
	override def opposite: Sign = -this
	override def binary: Option[Sign] = Some(this)
	
	override def *(sign: Sign): Sign = sign match {
		case Positive => this
		case Negative => opposite
	}
	
	
	// OTHER	----------------------------
	
	/**
	  * @param r a reversible instance
	  * @tparam R instance 'Repr' type
	  * @return 'r' if this is positive, -r otherwise
	  */
	def *[R](r: Reversible[R]) = if (isPositive) r.self else -r
	/**
	  * @param ordering An ordering to possibly reverse
	  * @tparam A Type of ordered items
	  * @return Specified ordering if this is Positive, otherwise reversed ordering
	  */
	def *[A](ordering: Ordering[A]) = if (isPositive) ordering else ordering.reverse
	
	/**
	  * @param a An item
	  * @param b Another item
	  * @param ord Implicit ordering to use
	  * @tparam A Type of the two items
	  * @return The lesser of the two items, when moving along this sign.
	  *         I.e. for Positive, returns min,
	  *              for Negative, returns max
	  */
	def lesser[A](a: A, b: A)(implicit ord: Ordering[A]) = if (isPositive) ord.min(a, b) else ord.max(a, b)
	/**
	  * @param a   An item
	  * @param b   Another item
	  * @param ord Implicit ordering to use
	  * @tparam A Type of the two items
	  * @return The greater of the two items, when moving along this sign.
	  *         I.e. for Positive, returns max,
	  *         for Negative, returns min
	  */
	def greater[A](a: A, b: A)(implicit ord: Ordering[A]) = if (isPositive) ord.max(a, b) else ord.min(a, b)
}

object Sign
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * All 2 sign values (first positive, then negative)
	  */
	val values = Pair[Sign](Positive, Negative)
	
	
	// OTHER    --------------------------
	
	/**
	  * @param number A number
	  * @return Sign of that number
	  */
	def of[N](number: N)(implicit n: Numeric[N]): SignOrZero = {
		val cmp = n.compare(number, n.zero)
		if (cmp == 0)
			Neutral
		else if (cmp > 0)
			Positive
		else
			Negative
	}
	/**
	  * @param duration A time duration
	  * @return Sign of that duration
	  */
	def of(duration: Duration): SignOrZero =
		if (duration == Duration.Zero) Neutral else if (duration < Duration.Zero) Negative else Positive
	
	/**
	  * @param extreme Targeted extreme
	  * @return Sign at that extreme
	  */
	def apply(extreme: Extreme): Sign = extreme match {
		case Min => Negative
		case Max => Positive
	}
	/**
	  * @param positiveCondition A condition for returning Positive
	  * @return Positive if condition was true, Negative otherwise
	  */
	def apply(positiveCondition: Boolean): Sign =
		if (positiveCondition) Positive else Negative
	
	
	// NESTED   --------------------------
	
	/**
	  * Positive sign (+). AKA positive direction (usually right / down / clockwise)
	  */
	case object Positive extends Sign
	{
		override def isPositive = true
		override def modifier = 1
		override def extreme = Max
		
		override def unary_- = Negative
		override def self = this
		
		override def compareTo(o: SignOrZero) = o match {
			case Positive => 0
			case _ => 1
		}
		
		override def *[N](num: N)(implicit n: Numeric[N]): N = num
		override def *[A](item: SignedOrZero[A]): A = item.self
	}
	/**
	  * Negative sign (-). AKA negative direction (usually left / up / counterclockwise)
	  */
	case object Negative extends Sign
	{
		override def isPositive = false
		override def modifier = -1
		override def extreme = Min
		
		override def unary_- = Positive
		override def self = this
		
		override def compareTo(o: SignOrZero) = o match {
			case Negative => 0
			case _ => -1
		}
		
		override def *[N](num: N)(implicit n: Numeric[N]): N = n.negate(num)
		override def *[A](item: SignedOrZero[A]): A = -item
	}
}
