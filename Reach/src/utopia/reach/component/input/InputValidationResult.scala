package utopia.reach.component.input

import utopia.reflection.color.ColorRole
import utopia.reflection.color.ColorRole.Error
import utopia.reflection.localization.LocalizedString

import scala.language.implicitConversions

/**
  * A common trait for results of input validation processes
  * @author Mikko Hilpinen
  * @since 18.9.2022, v0.4
  */
trait InputValidationResult
{
	/**
	  * @return A message to display based on the input validation.
	  *         Empty if no message is appropriate or needed.
	  */
	def message: LocalizedString
	/**
	  * @return Color highlighting to apply on the input field or around the message.
	  *         None if no highlighting is necessary.
	  */
	def highlighting: Option[ColorRole]
}

object InputValidationResult
{
	// IMPLICIT --------------------
	
	// Implicitly converts unit to the default result
	implicit def unitToResult(u: Unit): InputValidationResult = Default
	
	
	// OTHER    --------------------
	
	/**
	  * Creates a new input validation result
	  * @param message Message to display
	  * @param highlighting Highlighting color to apply (optional)
	  * @return A new input validation result
	  */
	def apply(message: LocalizedString, highlighting: Option[ColorRole] = None): InputValidationResult =
		Custom(message, highlighting)
	/**
	  * Creates a new input validation result
	  * @param message Message to display
	  * @param highlighting Highlighting color to apply
	  * @return A new input validation result
	  */
	def apply(message: LocalizedString, highlighting: ColorRole): InputValidationResult =
		apply(message, Some(highlighting))
	
	
	// NESTED   -------------------
	
	/**
	  * The default result that doesn't display anything
	  */
	case object Default extends InputValidationResult {
		override def message = LocalizedString.empty
		override def highlighting = None
	}
	/**
	  * An empty successful input validation result (applies highlighting but no message)
	  */
	case object Success extends InputValidationResult {
		override def message = LocalizedString.empty
		override def highlighting = Some(ColorRole.Success)
	}
	/**
	  * A failure result
	  * @param message Message to display. Empty if no message should be displayed
	  */
	case class Failure(message: LocalizedString) extends InputValidationResult
	{
		override def highlighting = Some(Error)
	}
	/**
	  * A warning result
	  * @param message Message to display. Empty if no message should be displayed
	  */
	case class Warning(message: LocalizedString) extends InputValidationResult
	{
		override def highlighting = Some(ColorRole.Warning)
	}
	
	private case class Custom(message: LocalizedString, highlighting: Option[ColorRole]) extends InputValidationResult
}
