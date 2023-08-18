package utopia.flow.operator

import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.flow.operator.SignOrZero.Neutral
import utopia.flow.operator.UncertainSign.UncertainBinarySign
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
	
	
	// COMPUTED -----------------------------
	
	/**
	  * @return Whether this item represents the neutral / zero state.
	  *         Result may be uncertain.
	  */
	def isNeutral: UncertainBoolean = this == Neutral
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
	override def binary: Option[UncertainBinarySign] = Some(UncertainBinarySign)
	
	override def mayBe[B >: SignOrZero](v: B): Boolean = true
	
	
	// IMPLICIT -----------------------------
	
	implicit def certainToUncertain(sign: SignOrZero): UncertainSign = CertainSign(sign)
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param sign A sign
	  * @return Uncertain sign that is known not to be the specified sign
	  */
	def not(sign: SignOrZero) = NotSign(sign)
	
	
	// VALUES   -----------------------------
	
	case object UncertainBinarySign extends UncertainBinarySign
	{
		// ATTRIBUTES   ---------------------
		
		/**
		  * All values of this enumeration (positive, negative, uncertain)
		  */
		val values = CertainBinarySign.values :+ this
		
		
		// IMPLICIT -------------------------
		
		implicit def certainToUncertain(sign: Sign): UncertainBinarySign = CertainBinarySign(sign)
		
		
		// IMPLEMENTED  ---------------------
		
		override def exact: Option[Sign] = None
		override def unary_- : UncertainSign = this
	}
	/**
	  * Common traits for uncertain selections between Positive and Negative
	  */
	sealed trait UncertainBinarySign extends UncertainSign with Uncertain[Sign]
	{
		override def binary: Option[UncertainBinarySign] = Some(this)
		override def mayBe[B >: Sign](v: B): Boolean = exact.forall { _ == v }
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
	sealed trait CertainSign extends UncertainSign with Signed[CertainSign]
	{
		override def self = this
		override def exact: Option[SignOrZero] = Some(sign)
		override def mayBe[B >: SignOrZero](v: B): Boolean = sign == v
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
	sealed trait CertainBinarySign extends CertainSign with UncertainBinarySign with BinarySigned[CertainBinarySign]
	{
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
	}
	/**
	  * Sign that is known to be Negative (exactly)
	  */
	case object CertainlyNegative extends CertainBinarySign
	{
		override def sign: Sign = Negative
		override def unary_- : CertainBinarySign = CertainlyPositive
	}
	/**
	  * Sign that is known to be Neutral / Zero
	  */
	case object CertainlyNeutral extends CertainSign
	{
		override def sign: SignOrZero = Neutral
		override def unary_- = this
		override def binary: Option[UncertainBinarySign] = None
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
		override def mayBe[B >: SignOrZero](v: B): Boolean = v != nonValue
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
	case object NotNeutral extends NotSign
	{
		override def nonValue: SignOrZero = Neutral
		override def binary: Option[UncertainBinarySign] = Some(UncertainBinarySign)
		override def unary_- : UncertainSign = this
	}
}
