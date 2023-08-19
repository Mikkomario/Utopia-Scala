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
	@deprecated("Please use .isCertainlyTrue instead", "v2.0")
	def isTrue = isCertainlyTrue
	/**
	 * @return Whether this boolean is known to be false
	 */
	def isCertainlyFalse = isCertainlyExactly(false)
	@deprecated("Please use .isCertainlyFalse instead", "v2.0")
	def isFalse = isCertainlyFalse
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
	@deprecated("Please use toBoolean instead", "v2.0")
	def get = toBoolean
	
	
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
}

case object UncertainBoolean extends UncertainBoolean
{
	// ATTRIBUTES   --------------------------------
	
	/**
	 * All possible values of this enumeration
	 */
	lazy val values = Vector[UncertainBoolean](CertainBoolean(true), CertainBoolean(false), this)
	
	
	// COMPUTED -------------------------
	
	@deprecated("Please use Uncertain instead", "v2.0")
	def Undefined = this
	@deprecated("Please use CertainBoolean instead", "v2.2")
	def Certain = CertainBoolean
	
	
	// IMPLICIT ------------------------------------
	
	implicit def autoConvertToOption(boolean: UncertainBoolean): Option[Boolean] = boolean.exact
	implicit def autoConvertFromOption(value: Option[Boolean]): UncertainBoolean = value match {
		case Some(known) => CertainBoolean(known)
		case None => this
	}
	implicit def autoConvertFromBoolean(boolean: Boolean): UncertainBoolean = CertainBoolean(boolean)
	
	
	// IMPLEMENTED  --------------------------------
	
	override def exact: Option[Boolean] = None
	override def unary_! : UncertainBoolean = this
	
	
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
	}
}