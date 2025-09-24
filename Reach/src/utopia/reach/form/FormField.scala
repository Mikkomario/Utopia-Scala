package utopia.reach.form

import utopia.flow.generic.model.immutable.Value
import utopia.flow.view.immutable.View
import utopia.reach.component.template.ReachComponent
import utopia.reach.focus.FocusRequestable
import utopia.reach.form.FormFieldOut.Success

import scala.language.implicitConversions

object FormField
{
	// IMPLICIT -------------------------
	
	/**
	 * Implicitly wraps a name + field -tuple
	 * @param nameAndField Name + field to convert into a form field.
	 *                     The field is expected to be a [[FocusRequestable]] [[ReachComponent]]
	 *                     that provides some kind of output value.
	 * @param outputToValue An implicitly available conversion from the field's output type to [[Value]]
	 * @tparam A Type of the field's natural output
	 * @return A form field based on that name + field
	 */
	implicit def wrap[A](nameAndField: (String, ReachComponent with FocusRequestable with View[A]))
	                    (implicit outputToValue: A => Value): FormField =
		apply(nameAndField._1) { outputToValue(nameAndField._2.value) }
	
	
	// OTHER    -------------------------
	
	/**
	 * Creates a form field with no input validation
	 * @param name Name of this field
	 * @param value A function that yields this field's value
	 * @return A new form field
	 */
	def apply(name: String)(value: => Value) = new FormField(name, None, None)(Success(value))
	
	/**
	 * Creates a new form field
	 * @param name Name of this field
	 * @param field The wrapped component
	 * @param out A function that yields an output value for this field
	 * @return A new form field
	 */
	def apply(name: String, field: ReachComponent with FocusRequestable)(out: => FormFieldOut) =
		new FormField(name, Some(field), Some(field))(out)
	/**
	 * Creates a new form field with separate components for notification-display and focus.
	 * Useful in situations where the focusable component is very specific,
	 * e.g. when the field consists of multiple components.
	 * @param name Name of this field
	 * @param component The wrapped [[ReachComponent]], for the purposes of locating notifications
	 * @param focusable The wrapped [[FocusRequestable]], which receives focus if field value is rejected
	 * @param out A function that produces field output
	 * @return A new form field
	 */
	def separate(name: String, component: ReachComponent, focusable: FocusRequestable)(out: => FormFieldOut) =
		new FormField(name, Some(component), Some(focusable))(out)
}

/**
 * Represents an input component in a form
 * @param name Name of this field / field's variable
 * @param component The wrapped [[ReachComponent]], over which potential notifications are displayed (optional)
 * @param focusable The wrapped [[FocusRequestable]] that receives focus when/if the input value is rejected (optional)
 * @param getValue A function that yields a result / output value for this field
 * @author Mikko Hilpinen
 * @since 07.09.2025, v1.7
 */
class FormField(val name: String, val component: Option[ReachComponent], val focusable: Option[FocusRequestable])
               (getValue: => FormFieldOut)
	extends View[FormFieldOut]
{
	// COMPUTED -----------------------------
	
	/**
	 * @return Component associated with this form field.
	 *         None if this field doesn't have an associated component,
	 *         or if that component is not linked to the main component hierarchy.
	 */
	def visibleComponent = component.filter { _.isLinked }
	
	
	// IMPLEMENTED  -------------------------
	
	override def value: FormFieldOut = getValue
}