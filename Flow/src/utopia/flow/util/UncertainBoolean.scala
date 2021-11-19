package utopia.flow.util

import utopia.flow.util.UncertainBoolean.{Certain, Undefined}

import scala.language.implicitConversions

/**
 * An enumeration to be used in cases where a true / false value is expected, but undefined is possible as well
 * @author Mikko Hilpinen
 * @since 31.3.2021, v1.9
 */
sealed trait UncertainBoolean
{
	// ABSTRACT --------------------------
	
	/**
	 * @return The boolean representation of this value, if known
	 */
	def value: Option[Boolean]
	
	
	// COMPUTED --------------------------
	
	/**
	 * @return True if this boolean value is known
	 */
	def isDefined = value.isDefined
	/**
	 * @return True if this boolean value is unknown / undefined
	 */
	def isUndefined = value.isEmpty
	
	/**
	 * @return Whether this boolean is known to be true
	 */
	def isTrue = value.contains(true)
	/**
	 * @return Whether this boolean is known to be false
	 */
	def isFalse = value.contains(false)
	/**
	  * @return If this boolean is not known to be true
	  */
	def mayBeFalse = !isTrue
	/**
	  * @return If this boolean is not known to be false
	  */
	def mayBeTrue = !isFalse
	
	/**
	 * @return The known value of this boolean or false
	 */
	def get = getOrElse(false)
	
	
	// OTHER    --------------------------
	
	/**
	 * @param default Value returned for undefined booleans
	 * @return This boolean value (if known) or the default value (if undefined)
	 */
	def getOrElse(default: => Boolean) = value.getOrElse(default)
	
	/**
	 * @param other Another uncertain boolean value (call by name)
	 * @return This value if defined, otherwise the other value
	 */
	def orElse(other: => UncertainBoolean) = if (isDefined) this else other
	
	/**
	 * @param other Another known boolean value
	 * @return AND of these two values (known if other is false or this value is known)
	 */
	def &&(other: Boolean): UncertainBoolean =
	{
		if (other)
			value match
			{
				case Some(known) => Certain(known && other)
				case None => Undefined
			}
		else
			Certain(false)
	}
	/**
	 * @param other Another boolean value
	 * @return AND of these two values (known if either of these is known to be false or if both are known)
	 */
	def &&(other: UncertainBoolean): UncertainBoolean = other match
	{
		case Certain(known) => this && known
		case Undefined => if (isFalse) Certain(false) else Undefined
	}
	
	/**
	 * @param other Another known boolean value
	 * @return OR of these two values (known if other is true or if this value is known)
	 */
	def ||(other: Boolean) =
	{
		if (other)
			Certain(true)
		else
			this
	}
	/**
	 * @param other Another boolean value
	 * @return OR of these two values (known if either of these is known to be true or if both are known)
	 */
	def ||(other: UncertainBoolean): UncertainBoolean = other.value match
	{
		case Some(known) => this || known
		case None => if (isTrue) Certain(true) else Undefined
	}
}

object UncertainBoolean
{
	// ATTRIBUTES   --------------------------------
	
	/**
	 * All possible values of this enumeration
	 */
	lazy val values = Vector[UncertainBoolean](Certain(true), Certain(false), Undefined)
	
	
	// IMPLICIT ------------------------------------
	
	implicit def autoConvertToOption(boolean: UncertainBoolean): Option[Boolean] = boolean.value
	
	implicit def autoConvertFromOption(value: Option[Boolean]): UncertainBoolean = value match
	{
		case Some(known) => Certain(known)
		case None => Undefined
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
	case class Certain(knownValue: Boolean) extends UncertainBoolean
	{
		override def value = Some(knownValue)
	}
	
	/**
	 * Used when the boolean value is unknown
	 */
	case object Undefined extends UncertainBoolean
	{
		override def value = None
	}
}