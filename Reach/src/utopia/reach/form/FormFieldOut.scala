package utopia.reach.form

import utopia.firmament.localization.LocalizedString
import utopia.flow.generic.model.immutable.Value
import utopia.flow.view.immutable.caching.Lazy

import scala.concurrent.Future

/**
 * An enumeration used for wrapping / representing form field output values and/or validation results.
 *
 * @author Mikko Hilpinen
 * @since 07.09.2025, v1.7
 */
sealed trait FormFieldOut

object FormFieldOut
{
	// TODO: Add delayed functions, like showing a confirm dialog
	
	// VALUES   ------------------------
	
	/**
	 * Represents a successfully acquired & accepted form field value
	 * @param value Wrapped value
	 */
	case class Success(value: Value) extends FormFieldOut
	/**
	 * Represents a rejected form field value
	 * @param message Error message to display. May be empty.
	 */
	case class Failure(message: LocalizedString) extends FormFieldOut
	
	/**
	 * When this output is yielded, the form value input-gathering is cancelled without notice.
	 */
	case object Cancel extends FormFieldOut
	
	/**
	 * Represents a delayed / multistep output.
	 * Used, for example, when the output needs to be confirmed by the user,
	 * or when other additional input is required.
	 * @param result A future that will yield the eventual form field output to apply.
	 *               May be lazily initialized, so that the additional input / confirmation is only displayed
	 *               when actually necessary.
	 */
	case class Delayed(result: Lazy[Future[FormFieldOut]]) extends FormFieldOut
}