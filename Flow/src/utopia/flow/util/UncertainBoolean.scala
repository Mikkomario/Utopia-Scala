package utopia.flow.util

import utopia.flow.operator.Reversible
import utopia.flow.util.UncertainBoolean.{Certain, Uncertain}

import scala.language.implicitConversions

/**
 * An enumeration to be used in cases where a true / false value is expected, but undefined is possible as well
 * @author Mikko Hilpinen
 * @since 31.3.2021, v1.9
 */
sealed trait UncertainBoolean extends Reversible[UncertainBoolean]
{
	// ABSTRACT --------------------------
	
	/**
	 * @return The boolean representation of this value, if known
	 */
	def value: Option[Boolean]
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return Whether this value is known for certain to be either true or false
	  */
	def isCertain = value.isDefined
	/**
	  * @return Whether it isn't known whether this value is true or false
	  */
	def isUncertain = !isCertain
	
	/**
	 * @return True if this boolean value is known
	 */
	@deprecated("Please use .isCertain instead", "v2.0")
	def isDefined = value.isDefined
	/**
	 * @return True if this boolean value is unknown / undefined
	 */
	@deprecated("Please use .isUncertain instead", "v2.0")
	def isUndefined = value.isEmpty
	
	/**
	 * @return Whether this value is known to be true
	 */
	def isCertainlyTrue = value.contains(true)
	@deprecated("Please use .isCertainlyTrue instead", "v2.0")
	def isTrue = isCertainlyTrue
	/**
	 * @return Whether this boolean is known to be false
	 */
	def isCertainlyFalse = value.contains(false)
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
	
	/**
	  * @return An inversion of this value
	  */
	def unary_! : UncertainBoolean = value match {
		case Some(known) => Certain(!known)
		case None => Uncertain
	}
	
	
	// IMPLEMENTED  ----------------------
	
	override def self = this
	override def unary_- = !this
	
	
	// OTHER    --------------------------
	
	/**
	 * @param default Value returned in uncertain cases
	 * @return This value if known, otherwise the default value
	 */
	def getOrElse(default: => Boolean) = value.getOrElse(default)
	/**
	 * @param other Another uncertain boolean value (call-by-name)
	 * @return This value if defined, otherwise the other value
	 */
	def orElse(other: => UncertainBoolean) = if (isCertain) this else other
	
	/**
	 * @param other Another known boolean value
	 * @return AND of these two values (known if other is false or this value is known)
	 */
	def &&(other: Boolean): UncertainBoolean = {
		if (other)
			value match {
				case Some(known) => Certain(known && other)
				case None => Uncertain
			}
		else
			Certain(false)
	}
	/**
	 * @param other Another boolean value
	 * @return AND of these two values (known if either of these is known to be false or if both are known)
	 */
	def &&(other: UncertainBoolean): UncertainBoolean = other match {
		case Certain(known) => this && known
		case Uncertain => if (isCertainlyFalse) Certain(false) else Uncertain
	}
	/**
	 * @param other Another known boolean value
	 * @return OR of these two values (known if other is true or if this value is known)
	 */
	def ||(other: Boolean) = {
		if (other)
			Certain(true)
		else
			this
	}
	/**
	 * @param other Another boolean value
	 * @return OR of these two values (known if either of these is known to be true or if both are known)
	 */
	def ||(other: UncertainBoolean): UncertainBoolean = other.value match {
		case Some(known) => this || known
		case None => if (isCertainlyTrue) Certain(true) else Uncertain
	}
}

object UncertainBoolean
{
	// ATTRIBUTES   --------------------------------
	
	/**
	 * All possible values of this enumeration
	 */
	lazy val values = Vector[UncertainBoolean](Certain(true), Certain(false), Uncertain)
	
	
	// COMPUTED -------------------------
	
	@deprecated("Please use Uncertain instead", "v2.0")
	def Undefined = Uncertain
	
	
	// IMPLICIT ------------------------------------
	
	implicit def autoConvertToOption(boolean: UncertainBoolean): Option[Boolean] = boolean.value
	
	implicit def autoConvertFromOption(value: Option[Boolean]): UncertainBoolean = value match {
		case Some(known) => Certain(known)
		case None => Uncertain
	}
	
	implicit def autoConvertFromBoolean(boolean: Boolean): UncertainBoolean = Certain(boolean)
	
	
	// NESTED   ------------------------------------
	
	object Certain
	{
		// IMPLICIT --------------------------------
		
		implicit def autoConvertToBoolean(certain: Certain): Boolean = certain.knownValue
		implicit def autoConvertFromBoolean(boolean: Boolean): Certain = Certain(boolean)
	}
	/**
	 * Used when the boolean value is known
	 * @param knownValue The known boolean value
	 */
	case class Certain(knownValue: Boolean) extends UncertainBoolean with Reversible[Certain]
	{
		override def value = Some(knownValue)
		
		override def self = this
		
		override def unary_! = Certain(!knownValue)
		override def unary_- = !this
	}
	
	/**
	 * Used when the boolean value is unknown
	 */
	case object Uncertain extends UncertainBoolean
	{
		override def value = None
	}
}