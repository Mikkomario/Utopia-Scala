package utopia.reach.window

import utopia.flow.generic.model.immutable.Value
import utopia.reach.component.template.{ReachComponentLike, ReachComponentWrapper}
import utopia.reach.focus.FocusRequestable
import utopia.firmament.component.input.Input
import utopia.firmament.localization.LocalizedString

import scala.language.implicitConversions

object InputField
{
	// IMPLICIT -------------------------
	
	/**
	  * Implicitly converts a field into an input field
	  * @param field A field that supports focus-requesting and provides input
	  * @param f A function for implicitly converting the field input into a value
	  * @tparam A Type of field input
	  * @return A new input field that wraps the specified field
	  */
	implicit def autoConvert[A](field: ReachComponentLike with FocusRequestable with Input[A])
	                           (implicit f: A => Value): InputField =
		wrap(field) { Right(f(field.value)) }
	
	
	// OTHER    -------------------------
	
	/**
	  * Converts a component into an input field
	  * @param field A field to wrap
	  * @param value A function for generating the input value, which may be either
	  *                 Right: Success (as a Value), or
	  *                 Left: Failure, as an error message to display
	  * @return A new input field that wraps the specified field and uses the specified input function
	  */
	def wrap(field: ReachComponentLike with FocusRequestable)(value: => Either[LocalizedString, Value]): InputField =
		new InputFieldWrapper(field, value)
	
	/**
	  * Converts a component into an input field
	  * @param field A field to wrap
	  * @param value A function that generates the input value (as a [[Value]])
	  * @return A new input field that wraps the specified field and uses the specified input function
	  */
	def apply(field: ReachComponentLike with FocusRequestable)(value: => Value) = wrap(field)(Right(value))
	
	/**
	  * Adds input validation / conversion for an input field
	  * @param field An input field
	  * @param test A function that accepts the field's current value and returns either
	  *                 Right: The input value converted into a [[Value]], or
	  *                 Left: A failure message, in case the input was not acceptable
	  * @tparam A Type of original input value
	  * @return A new input field
	  */
	def test[A](field: ReachComponentLike with FocusRequestable with Input[A])(test: A => Either[LocalizedString, Value]) =
		wrap(field)(test(field.value))
	
	/**
	  * Manually adds value conversion to an existing input field
	  * @param field An input field
	  * @param convert A function for converting the field's input into a [[Value]]
	  * @tparam A Type of fields input
	  * @return A new input field
	  */
	def convert[A](field: ReachComponentLike with FocusRequestable with Input[A])(convert: A => Value) =
		wrap(field)(Right(convert(field.value)))
	
	/**
	  * Adds validation to an input field's value
	  * @param field An input field
	  * @param validate A function for validating the field's current input.
	  *                 Returns an empty (localized) string on a success and a non-empty (localized) string on a failure
	  * @param f A function for implicitly converting the input into a [[Value]]
	  * @tparam A Field's input type
	  * @return A new input field
	  */
	def validate[A](field: ReachComponentLike with FocusRequestable with Input[A])
	               (validate: A => LocalizedString)(implicit f: A => Value) =
		wrap(field) {
			val input = field.value
			validate(input).notEmpty.toLeft { f(input) }
		}
	
	
	// EXTENSIONS   ---------------------
	
	implicit class InputFieldConvertible[A](val field: ReachComponentLike with FocusRequestable with Input[A]) extends AnyVal
	{
		/**
		  * Adds value conversion & input validation to this field
		  * @param f A function for testing this field's input.
		  *          Returns either
		  *             Right: [[Value]] on success, or
		  *             Left: A localized error message on failure
		  * @return A new input field
		  */
		def testWith(f: A => Either[LocalizedString, Value]) = InputField.test(field)(f)
		
		/**
		  * Adds value conversion to this field
		  * @param f A function that converts this field's input into a [[Value]]
		  * @return A new input field
		  */
		def convertWith(f: A => Value) = InputField.convert(field)(f)
		
		/**
		  * Adds input validation to this field
		  * @param f A function that validates this field's current input.
		  *          Returns an empty (localized) string on success and a non-empty message on failure.
		  * @param c Implicit value conversion for valid input values
		  * @return A new input field
		  */
		def validateWith(f: A => LocalizedString)(implicit c: A => Value) = InputField.validate(field)(f)
	}
	
	implicit class FieldWithoutInput(val field: ReachComponentLike with FocusRequestable) extends AnyVal
	{
		/**
		  * Adds input generation into this field
		  * @param f A function that generates input for this field
		  * @return A new input field
		  */
		def withInput(f: => Value) = InputField(field)(f)
		
		/**
		  * Adds input generation and -validation for this field
		  * @param f A function that generates either
		  *             Right: Valid value of this input, or
		  *             Left: Error message about invalid input
		  * @return A new input field
		  */
		def withInputOrFailure(f: => Either[LocalizedString, Value]) = InputField.wrap(field)(f)
	}
	
	
	// NESTED   -------------------------
	
	private class InputFieldWrapper(field: ReachComponentLike with FocusRequestable,
	                                f: => Either[LocalizedString, Value])
		extends InputField with ReachComponentWrapper
	{
		override protected def wrapped = field
		override def value = f
		
		override def requestFocus(forceFocusLeave: Boolean, forceFocusEnter: Boolean) =
			field.requestFocus(forceFocusLeave, forceFocusEnter)
	}
}

/**
  * Common trait for fields which may be used as window inputs
  * @author Mikko Hilpinen
  * @since 6.4.2023, v0.6
  */
trait InputField extends ReachComponentLike with FocusRequestable with Input[Either[LocalizedString, Value]]
