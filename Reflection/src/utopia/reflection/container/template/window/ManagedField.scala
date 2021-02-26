package utopia.reflection.container.template.window

import scala.language.implicitConversions

import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConvertible
import utopia.reflection.component.template.input.Input
import utopia.reflection.localization.LocalizedString

object ManagedField
{
	// IMPLICIT	--------------------------
	
	/**
	  * Implicitly converts an input field into a managed field (without failure state)
	  * @param field An input field
	  * @param autoConvert An implicit conversion from field result type to value
	  * @tparam V Type of intermediate ValueConvertible type
	  * @tparam C Type of wrapped field
	  * @return A new managed field
	  */
	implicit def autoConvert[V, C <: Input[V]](field: C)(implicit autoConvert: V => ValueConvertible): ManagedField[C] =
		alwaysSucceed(field) { autoConvert(field.value).toValue }
	
	
	// OTHER	--------------------------
	
	/**
	  * Creates a new managed field by combining a field and a value function
	  * @param field A field to wrap
	  * @param value A function that returns either Left: Error message or Right: processed field value
	  * @tparam C Type of wrapped field
	  * @return A new managed field
	  */
	def apply[C](field: C)(value: => Either[LocalizedString, Value]): ManagedField[C] =
		new ManagedFieldWrapper[C](field)(value)
	
	/**
	  * Creates a new managed field without a failure state
	  * @param field A field to wrap
	  * @param value A function for generating the field value
	  * @tparam C Type of wrapped field
	  * @return A new managed field
	  */
	def alwaysSucceed[C](field: C)(value: => Value): ManagedField[C] = apply(field) { Right(value) }
	
	/**
	  * Creates a new managed field by manual value conversion
	  * @param field A field to wrap
	  * @param convert A function for converting field value to Value
	  * @tparam A Type of field value
	  * @tparam C Type of wrapped field
	  * @return A new managed field
	  */
	def convert[A, C <: Input[A]](field: C)(convert: A => Value): ManagedField[C] =
		alwaysSucceed(field) { convert(field.value) }
	
	/**
	  * Creates a new managed field by value testing and manual conversion
	  * @param field A field to wrap
	  * @param process A function that accepts a field value and returns either
	  *                Left: error message or Right: converted value
	  * @tparam A Type of field value
	  * @tparam C Type of wrapped field
	  * @return A new managed field
	  */
	def testAndConvert[A, C <: Input[A]](field: C)(process: A => Either[LocalizedString, Value]) =
		apply(field) { process(field.value) }
	
	/**
	  * Creates a new managed field using implicit value conversion and a test function
	  * @param field A field to wrap
	  * @param test A function for testing current field value. Returns error message on failure and None on success.
	  * @param autoConvert Implicit conversion from the field value type to a value convertible data type
	  * @tparam V Type of intermediate field value type
	  * @tparam C Type of wrapped field
	  * @return A new managed field
	  */
	def test[V, C <: Input[V]](field: C)(test: V => Option[LocalizedString])
											(implicit autoConvert: V => ValueConvertible): ManagedField[C] =
		apply(field) {
			val raw = field.value
			test(raw) match
			{
				case Some(error) => Left(error)
				case None => Right(autoConvert(raw).toValue)
			}
		}
	
	
	// NESTED	--------------------------
	
	private class ManagedFieldWrapper[+C](override val field: C)(value: => Either[LocalizedString, Value])
		extends ManagedField[C]
	{
		override def currentValue = value
	}
}

/**
  * A common trait for created field components that are managed with value conversion and value checking
  * @author Mikko Hilpinen
  * @since 26.2.2021, v1
  */
trait ManagedField[+C]
{
	// ABSTRACT	--------------------------
	
	/**
	  * @return The managed input field
	  */
	def field: C
	
	/**
	  * @return The current managed field value. Either<br>
	  *         Left: a message on invalid value failure or<br>
	  *         Right: a proper value
	  */
	def currentValue: Either[LocalizedString, Value]
}
