package utopia.flow.util

import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.operator.Reversible
import utopia.flow.util.UncertainBoolean.{CertainBoolean, CertainlyFalse, CertainlyTrue}
import utopia.flow.view.immutable.View

import scala.language.implicitConversions

/**
 * An enumeration to be used in cases where a true / false value is expected, but undefined is possible as well
 * @author Mikko Hilpinen
 * @since 31.3.2021, v1.9
 */
sealed trait UncertainBoolean
	extends utopia.flow.operator.Uncertain[Boolean] with Reversible[UncertainBoolean] with ValueConvertible
{
	// ABSTRACT --------------------------
	
	/**
	  * @return An inversion of this value
	  */
	def unary_! : UncertainBoolean
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return Whether this value is known for certain to be either true or false
	  */
	def isCertain = isExact
	/**
	  * @return Whether it isn't known whether this value is true or false
	  */
	def isUncertain = !isCertain
	
	/**
	 * @return Whether this value is known to be true
	 */
	def isCertainlyTrue = isCertainly(true)
	/**
	 * @return Whether this boolean is known to be false
	 */
	def isCertainlyFalse = isCertainly(false)
	/**
	  * @return If this boolean is not known to be true
	  */
	def mayBeFalse = !isCertainlyTrue
	/**
	  * @return If this boolean is not known to be false
	  */
	def mayBeTrue = !isCertainlyFalse
	
	/**
	 * @return This value as a boolean. False if unknown.
	 */
	def toBoolean = getOrElse(false)
	
	
	// IMPLEMENTED  ----------------------
	
	override def self = this
	override def unary_- = !this
	
	override def mayBe(v: Boolean): Boolean = exact.forall { _ == v }
	
	
	// OTHER    --------------------------
	
	/**
	 * @param other Another uncertain boolean value (call-by-name)
	 * @return This value if defined, otherwise the other value
	 */
	def orElse(other: => UncertainBoolean) = if (isExact) this else other
	
	/**
	 * @param other Another known boolean value (call-by-name)
	 * @return AND of these two values (known if other is false or this value is known)
	 */
	def &&(other: => Boolean): UncertainBoolean = {
		if (isCertainlyFalse)
			this
		else {
			val o = other
			if (o)
				exact match {
					case Some(known) => CertainBoolean(known && o)
					case None => UncertainBoolean
				}
			else
				CertainlyFalse
		}
	}
	/**
	 * @param other Another boolean value
	 * @return AND of these two values (known if either of these is known to be false or if both are known)
	 */
	def &&(other: UncertainBoolean): UncertainBoolean = other match {
		case c: CertainBoolean => this && c.value
		case uncertain => uncertain
	}
	/**
	 * @param other Another known boolean value
	 * @return OR of these two values (known if other is true or if this value is known)
	 */
	def ||(other: => Boolean) = if (isCertainlyTrue || !other) this else CertainlyTrue
	/**
	 * @param other Another boolean value
	 * @return OR of these two values (known if either of these is known to be true or if both are known)
	 */
	def ||(other: UncertainBoolean): UncertainBoolean = other.exact match {
		case Some(known) => this || known
		case None => UncertainBoolean
	}
	
	/**
	  * @param condition A condition that, if met, causes this value to become uncertain
	  *                  (call-by-name, only called for certain values)
	  * @return This value that is marked as uncertain if the specified condition is met
	  */
	def uncertainIf(condition: => Boolean) = if (isUncertain || !condition) this else UncertainBoolean
	/**
	  * @param condition A condition that, if met, causes this value to be no longer certainly true.
	  *                  Call-by-name, only called for certainly true values.
	  * @return This value that has been marked as uncertain if both this and the specified condition were true
	  */
	def notCertainlyTrueIf(condition: => Boolean) =
		if (isCertainlyTrue && condition) UncertainBoolean else this
	/**
	  * @param condition A condition that, if met, causes this value to be no longer certainly false.
	  *                  Call-by-name, only called for certainly false values.
	  * @return This value that has been marked as uncertain if both this was false and the specified condition was true
	  */
	def notCertainlyFalseIf(condition: => Boolean) =
		if (isCertainlyFalse && condition) UncertainBoolean else this
}

case object UncertainBoolean extends UncertainBoolean
{
	// ATTRIBUTES   --------------------------------
	
	/**
	 * All possible values of this enumeration
	 */
	lazy val values = CertainBoolean.values :+ this
	
	
	// COMPUTED -------------------------
	
	/**
	 * A boolean/value that is known to be true
	 */
	@deprecated("Please call CertainlyTrue directly, instead", "v2.7")
	def certainlyTrue = CertainlyTrue
	/**
	 * A boolean/value that is known to be false
	 */
	@deprecated("Please call CertainlyFalse directly, instead", "v2.7")
	def certainlyFalse = CertainlyFalse
	
	
	// IMPLEMENTED  ---------------------
	
	override implicit def toValue: Value = Value.empty
	
	
	// IMPLICIT ------------------------------------
	
	implicit def apply(value: Option[Boolean]): UncertainBoolean = value match {
		case Some(known) => apply(known)
		case None => this
	}
	implicit def apply(boolean: Boolean): UncertainBoolean = if (boolean) CertainlyTrue else CertainlyFalse
	
	implicit def autoConvertToOption(boolean: UncertainBoolean): Option[Boolean] = boolean.exact
	
	
	// IMPLEMENTED  --------------------------------
	
	override def exact: Option[Boolean] = None
	override def unary_! : UncertainBoolean = this
	
	override def toString = "?"
	
	
	// NESTED   ------------------------------------
	
	object CertainBoolean
	{
		// ATTRIBUTES   ----------------------------
		
		/**
		 * The values of this enumeration: First false, then true.
		 */
		lazy val values = Pair[CertainBoolean](CertainlyFalse, CertainlyTrue)
		
		
		// IMPLICIT --------------------------------
		
		implicit def apply(boolean: Boolean): CertainBoolean = if (boolean) CertainlyTrue else CertainlyFalse
		
		implicit def autoConvertToBoolean(certain: CertainBoolean): Boolean = certain.value
	}
	/**
	 * Used when the boolean value is known
	 */
	sealed trait CertainBoolean extends UncertainBoolean with Reversible[CertainBoolean] with View[Boolean]
	{
		// ABSTRACT -----------------------------
		
		override def unary_! : CertainBoolean
		
		
		// COMPUTED -----------------------------
		
		@deprecated("Please call .value instead", "v2.7")
		def knownValue = value
		
		
		// IMPLEMENTED  -------------------------
		
		override def self = this
		override def unary_- : CertainBoolean = !this
		
		override def exact = Some(value)
		
		override def toString = value.toString
		override implicit def toValue: Value = ValueOfBoolean(value)
	}
	
	/**
	 * A certain true value of UncertainBoolean
	 */
	case object CertainlyTrue extends CertainBoolean
	{
		override val value: Boolean = true
		
		override def unary_! : CertainBoolean = CertainlyFalse
	}
	/**
	 * A certain false value of UncertainBoolean
	 */
	case object CertainlyFalse extends CertainBoolean
	{
		override val value: Boolean = false
		
		override def unary_! : CertainBoolean = CertainlyTrue
	}
}