package utopia.flow.util

import utopia.flow.operator.Reversible
import utopia.flow.util.UncertainBoolean.CertainBoolean

import scala.language.implicitConversions

/**
 * An enumeration to be used in cases where a true / false value is expected, but undefined is possible as well
 * @author Mikko Hilpinen
 * @since 31.3.2021, v1.9
 */
sealed trait UncertainBoolean extends utopia.flow.operator.Uncertain[Boolean] with Reversible[UncertainBoolean]
{
	// ABSTRACT --------------------------
	
	/**
	  * @return An inversion of this value
	  */
	def unary_! : UncertainBoolean
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return The boolean representation of this value, if known
	  */
	@deprecated("Please use .exact instead", "v2.2")
	def value: Option[Boolean] = exact
	
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
	def isCertainlyTrue = isCertainlyExactly(true)
	/**
	 * @return Whether this boolean is known to be false
	 */
	def isCertainlyFalse = isCertainlyExactly(false)
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
	 * @param other Another known boolean value
	 * @return AND of these two values (known if other is false or this value is known)
	 */
	def &&(other: Boolean): UncertainBoolean = {
		if (other)
			exact match {
				case Some(known) => CertainBoolean(known && other)
				case None => UncertainBoolean
			}
		else
			CertainBoolean(false)
	}
	/**
	 * @param other Another boolean value
	 * @return AND of these two values (known if either of these is known to be false or if both are known)
	 */
	def &&(other: UncertainBoolean): UncertainBoolean = other match {
		case CertainBoolean(known) => this && known
		case UncertainBoolean => if (isCertainlyFalse) CertainBoolean(false) else UncertainBoolean
	}
	/**
	 * @param other Another known boolean value
	 * @return OR of these two values (known if other is true or if this value is known)
	 */
	def ||(other: Boolean) = {
		if (other)
			CertainBoolean(true)
		else
			this
	}
	/**
	 * @param other Another boolean value
	 * @return OR of these two values (known if either of these is known to be true or if both are known)
	 */
	def ||(other: UncertainBoolean): UncertainBoolean = other.exact match {
		case Some(known) => this || known
		case None => if (isCertainlyTrue) CertainBoolean(true) else UncertainBoolean
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
	  * A boolean/value that is known to be true
	  */
	lazy val certainlyTrue = CertainBoolean(true)
	/**
	  * A boolean/value that is known to be false
	  */
	lazy val certainlyFalse = CertainBoolean(false)
	
	/**
	 * All possible values of this enumeration
	 */
	lazy val values = Vector[UncertainBoolean](certainlyTrue, certainlyFalse, this)
	
	
	// COMPUTED -------------------------
	
	@deprecated("Please use CertainBoolean instead", "v2.2")
	def Certain = CertainBoolean
	
	
	// IMPLICIT ------------------------------------
	
	implicit def autoConvertToOption(boolean: UncertainBoolean): Option[Boolean] = boolean.exact
	implicit def autoConvertFromOption(value: Option[Boolean]): UncertainBoolean = value match {
		case Some(known) => autoConvertFromBoolean(known)
		case None => this
	}
	implicit def autoConvertFromBoolean(boolean: Boolean): UncertainBoolean =
		if (boolean) certainlyTrue else certainlyFalse
	
	
	// IMPLEMENTED  --------------------------------
	
	override def exact: Option[Boolean] = None
	override def unary_! : UncertainBoolean = this
	
	override def toString = "?"
	
	
	// NESTED   ------------------------------------
	
	object CertainBoolean
	{
		// IMPLICIT --------------------------------
		
		implicit def autoConvertToBoolean(certain: CertainBoolean): Boolean = certain.knownValue
		implicit def autoConvertFromBoolean(boolean: Boolean): CertainBoolean = CertainBoolean(boolean)
	}
	/**
	 * Used when the boolean value is known
	 * @param knownValue The known boolean value
	 */
	case class CertainBoolean(knownValue: Boolean) extends UncertainBoolean with Reversible[CertainBoolean]
	{
		override def exact = Some(knownValue)
		
		override def self = this
		
		override def unary_! = CertainBoolean(!knownValue)
		override def unary_- = !this
		
		override def toString = knownValue.toString
	}
}