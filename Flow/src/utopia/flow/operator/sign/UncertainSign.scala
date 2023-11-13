package utopia.flow.operator.sign

import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.operator.sign.SignOrZero.Neutral
import utopia.flow.operator.sign.UncertainSign.UncertainBinarySign
import utopia.flow.operator.{Reversible, Uncertain}
import utopia.flow.util.UncertainBoolean

import scala.language.implicitConversions

/**
  * Represents a state of being possibly positive, negative or neutral (0)
  * @author Mikko Hilpinen
  * @since 18.8.2023, v2.2
  */
sealed trait UncertainSign extends Uncertain[SignOrZero] with Reversible[UncertainSign]
{
	// ABSTRACT -----------------------------
	
	/**
	  * @return Copy of this item when the neutral / zero state is ruled out.
	  *         None if this item was known to be neutral / zero.
	  */
	def binary: Option[UncertainBinarySign]
	
	/**
	  * @param other Another possible sign
	  * @return copy of this sign that may also be the other sign
	  */
	def ||(other: SignOrZero): UncertainSign
	/**
	  * @param other Another possible sign
	  * @return copy of this sign that may also be the other sign
	  */
	def ||(other: UncertainSign): UncertainSign
	
	
	// COMPUTED -----------------------------
	
	/**
	  * @return Whether this sign is positive (may be uncertain)
	  */
	def isPositive = this == Positive
	/**
	  * @return Whether this sign is negative (may be uncertain)
	  */
	def isNegative = this == Negative
	/**
	  * @return Whether this item represents the neutral / zero state.
	  *         Result may be uncertain.
	  */
	def isNeutral: UncertainBoolean = this == Neutral
	
	/**
	  * @return Whether this sign is negative or neutral (may be uncertain)
	  */
	def nonPositive = !isPositive
	/**
	  * @return Whether this sign is positive or neutral (may be uncertain)
	  */
	def nonNegative = !isNegative
	/**
	  * @return Whether this sign is positive or negative (may be uncertain)
	  */
	def nonNeutral = !isNeutral
	
	/**
	  * @return Whether it is possible that this sign is positive
	  */
	def mayBePositive = isPositive.mayBeTrue
	/**
	  * @return Whether it is possible that this sign is negative
	  */
	def mayBeNegative = isNegative.mayBeTrue
	/**
	  * @return Whether neutral / 0 is a potential value of this item
	  */
	def mayBeNeutral = isNeutral.mayBeTrue
	
	/**
	  * @return Whether this item is known to be neutral / 0
	  */
	def isCertainlyNeutral = isNeutral.isCertainlyTrue
	/**
	  * @return Whether this item is known to not be neutral / 0
	  */
	def isCertainlyNotNeutral = isNeutral.isCertainlyFalse
	
	
	// IMPLEMENTED  -------------------------
	
	override def self: UncertainSign = this
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param replacementForZero Sign to return in case this item was known to be neutral / zero
	  * @return Copy of this item where the zero / neutral state has been ruled out.
	  */
	def binaryOr(replacementForZero: => UncertainBinarySign) = binary.getOrElse(replacementForZero)
}

object UncertainSign extends UncertainSign
{
	// IMPLEMENTED  -------------------------
	
	override def exact: Option[SignOrZero] = None
	override def unary_- : UncertainSign = this
	override def binary: Option[UncertainBinarySign] = Some(NotNeutral)
	
	override def mayBe(v: SignOrZero): Boolean = true
	
	override def ||(other: SignOrZero): UncertainSign = this
	override def ||(other: UncertainSign): UncertainSign = this
	
	
	// IMPLICIT -----------------------------
	
	implicit def certainToUncertain(sign: SignOrZero): UncertainSign = CertainSign(sign)
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param sign A sign
	  * @return Uncertain sign that is known not to be the specified sign
	  */
	def not(sign: SignOrZero) = NotSign(sign)
	
	
	// VALUES   -----------------------------
	
	case object UncertainBinarySign
	{
		// ATTRIBUTES   ---------------------
		
		/**
		  * All values of this enumeration (positive, negative, uncertain)
		  */
		val values = CertainBinarySign.values :+ this
		
		
		// IMPLICIT -------------------------
		
		implicit def certainToUncertain(sign: Sign): UncertainBinarySign = CertainBinarySign(sign)
	}
	/**
	  * Common traits for uncertain selections between Positive and Negative
	  */
	sealed trait UncertainBinarySign extends UncertainSign
	{
		// ABSTRACT -------------------
		
		override def exact: Option[Sign]
		
		
		// IMPLEMENTED  ---------------
		
		override def binary: Option[UncertainBinarySign] = Some(this)
		
		override def mayBe(v: SignOrZero): Boolean = v match {
			case Neutral => false
			case s: Sign => exact.forall { _ == s }
		}
	}
	
	object CertainSign
	{
		// ATTRIBUTES   ---------------------
		
		/**
		  * All values of this enumeration (positive, negative, neutral)
		  */
		val values = CertainBinarySign.values :+ CertainlyNeutral
		
		
		// OTHER    -------------------------
		
		/**
		  * @param sign A sign
		  * @return That sign as a certain value
		  */
		implicit def apply(sign: SignOrZero): CertainSign = sign match {
			case binary: Sign => CertainBinarySign(binary)
			case Neutral => CertainlyNeutral
		}
	}
	/**
	  * Common traits for certainly known sign selections
	  */
	sealed trait CertainSign extends UncertainSign
	{
		/**
		  * @return Certainly known sign
		  */
		def sign: SignOrZero
		
		override def self = this
		override def exact: Option[SignOrZero] = Some(sign)
		override def mayBe(v: SignOrZero): Boolean = sign == v
		
		override def ||(other: UncertainSign): UncertainSign = if (other.mayBe(sign)) other else other || sign
	}
	object CertainBinarySign
	{
		// ATTRIBUTES   --------------------
		
		/**
		  * All values of this enumeration (positive, negative)
		  */
		val values = Vector[CertainBinarySign](CertainlyPositive, CertainlyNegative)
		
		
		// OTHER    -----------------------
		
		/**
		  * @param sign A binary sign
		  * @return That sign as a certain value
		  */
		implicit def apply(sign: Sign): CertainBinarySign = sign match {
			case Positive => CertainlyPositive
			case Negative => CertainlyNegative
		}
	}
	/**
	  * Common trait for certain wrappers of Positive and Negative
	  */
	sealed trait CertainBinarySign extends CertainSign with UncertainBinarySign
	{
		override def sign: Sign
		
		override def self = this
		override def exact = Some(sign)
	}
	
	/**
	  * Sign that is known to be Positive (exactly)
	  */
	case object CertainlyPositive extends CertainBinarySign
	{
		override def sign: Sign = Positive
		override def unary_- : CertainBinarySign = CertainlyNegative
		
		override def ||(other: SignOrZero): UncertainSign = other match {
			case Positive => this
			case Negative => NotNeutral
			case Neutral => NotNegative
		}
	}
	/**
	  * Sign that is known to be Negative (exactly)
	  */
	case object CertainlyNegative extends CertainBinarySign
	{
		override def sign: Sign = Negative
		override def unary_- : CertainBinarySign = CertainlyPositive
		
		override def ||(other: SignOrZero): UncertainSign = other match {
			case Positive => NotNeutral
			case Negative => this
			case Neutral => NotPositive
		}
	}
	/**
	  * Sign that is known to be Neutral / Zero
	  */
	case object CertainlyNeutral extends CertainSign
	{
		override def sign: SignOrZero = Neutral
		override def unary_- = this
		override def binary: Option[UncertainBinarySign] = None
		
		override def ||(other: SignOrZero): UncertainSign = other match {
			case Neutral => this
			case s: Sign => NotSign(s.opposite)
		}
	}
	
	object NotSign
	{
		// ATTRIBUTES   -----------------
		
		/**
		  * All values of this enumeration
		  */
		val values = Vector[NotSign](NotPositive, NotNegative, NotNeutral)
		
		
		// OTHER    ---------------------
		
		/**
		  * @param sign A sign
		  * @return An uncertain sign where the value is certainly not the specified sign
		  */
		def apply(sign: SignOrZero): NotSign = sign match {
			case Positive => NotPositive
			case Negative => NotNegative
			case Neutral => NotNeutral
		}
	}
	/**
	  * Common trait for uncertain situations where it is known that the sign is not a certain value
	  */
	sealed trait NotSign extends UncertainSign
	{
		// ABSTRACT ---------------------
		
		/**
		  * @return The value this sign is known not to be
		  */
		def nonValue: SignOrZero
		
		
		// IMPLEMENTED  -----------------
		
		override def exact: Option[SignOrZero] = None
		override def mayBe(v: SignOrZero): Boolean = v != nonValue
		
		override def ||(other: SignOrZero): UncertainSign = if (other == nonValue) UncertainSign else this
		override def ||(other: UncertainSign): UncertainSign = if (other.mayBe(nonValue)) UncertainSign else this
	}
	
	/**
	  * Case when the sign is either neutral or negative
	  */
	case object NotPositive extends NotSign
	{
		override def nonValue: SignOrZero = Positive
		override def unary_- : UncertainSign = NotNegative
		override def binary: Option[UncertainBinarySign] = Some(CertainlyNegative)
	}
	/**
	  * Case when the sign is either neutral or positive
	  */
	case object NotNegative extends NotSign
	{
		override def nonValue: SignOrZero = Negative
		override def unary_- : UncertainSign = NotPositive
		override def binary: Option[UncertainBinarySign] = Some(CertainlyPositive)
	}
	/**
	  * Case when the sign is known to be binary (i.e. not neutral)
	  */
	case object NotNeutral extends NotSign with UncertainBinarySign
	{
		override def nonValue: SignOrZero = Neutral
		override def exact = None
		override def binary: Option[UncertainBinarySign] = Some(this)
		override def unary_- : UncertainSign = this
	}
}
