package utopia.reach.component.input.check

import utopia.firmament.context.TextContext
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.enumeration.StackLayout.Center
import utopia.flow.view.immutable.eventful.{AlwaysTrue, Fixed}
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.event.ConsumeEvent
import utopia.genesis.handling.MouseButtonStateListener
import utopia.paradigm.color.ColorRole.Secondary
import utopia.paradigm.color.{Color, ColorRole}
import utopia.paradigm.enumeration.Axis.X
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.{Mixed, TextContextualFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.component.template.CursorDefining
import utopia.reach.container.multi.Stack
import utopia.reach.cursor.CursorType.{Default, Interactive}
import utopia.reach.focus.FocusListener

/**
 * Used for constructing radio buttons with labels
 * @author Mikko Hilpinen
 * @since 30.1.2021, v0.1
 */
object RadioButtonLine extends Ccff[TextContext, ContextualRadioButtonLineFactory]
{
	override def withContext(hierarchy: ComponentHierarchy, context: TextContext) =
		ContextualRadioButtonLineFactory(hierarchy, context)
}

case class ContextualRadioButtonLineFactory(parentHierarchy: ComponentHierarchy, context: TextContext)
	extends TextContextualFactory[ContextualRadioButtonLineFactory]
{
	// IMPLEMENTED  ------------------------------------
	
	override def self: ContextualRadioButtonLineFactory = this
	
	override def withContext(newContext: TextContext) = copy(context = newContext)
	
	
	// OTHER    ----------------------------------------
	
	/**
	 * Creates a new radio button & label
	 * @param selectedValuePointer A mutable pointer to the selected value
	 * @param value Value represented by this radio button
	 * @param labelText Text displayed on the label
	 * @param selectedColorRole Color role that represents the selected state (default = Secondary)
	 * @param enabledPointer A pointer to the enabled state of this button (default = always enabled)
	 * @param backgroundColorPointer A pointer to the context container background color
	 *                               (default = determined by context (fixed))
	 * @param customDrawers Custom drawers to assign (default = empty)
	 * @param focusListeners Focus listeners to assign (default = empty)
	 * @tparam A Type of selected value
	 * @return A new radio button and associated label in a stack. The created radio button is added
	  *         as an additional result.
	 */
	def apply[A](selectedValuePointer: PointerWithEvents[A], value: A, labelText: LocalizedString,
	             selectedColorRole: ColorRole = Secondary, enabledPointer: Changing[Boolean] = AlwaysTrue,
	             backgroundColorPointer: Changing[Color] = Fixed(context.background),
	             customDrawers: Vector[CustomDrawer] = Vector(), focusListeners: Seq[FocusListener] = Vector()) =
	{
		Stack(parentHierarchy).withContext(context).copy(axis = X, layout = Center, customDrawers = customDrawers)
			.build(Mixed) { factories =>
				val radioButton = factories(RadioButton).apply(selectedValuePointer, value, selectedColorRole,
					enabledPointer, backgroundColorPointer, focusListeners = focusListeners)
				// Text color may vary
				val textColorPointer = backgroundColorPointer.mergeWith(enabledPointer) { (background, enabled) =>
					if (enabled) background.shade.defaultTextColor else background.shade.defaultHintTextColor
				}
				val label = factories(ViewTextLabel).withoutContext.forText(Fixed(labelText),
					textColorPointer.map { color => context.textDrawContext.copy(color = color) })
				label.addMouseButtonListener(MouseButtonStateListener.onLeftPressedInside { label.bounds } { _ =>
					radioButton.select()
					radioButton.requestFocus()
					Some(ConsumeEvent("Radio button selected via label"))
				})
				// Adds mouse functionality to the label
				CursorDefining.defineCursorFor(label,
					enabledPointer.map { enabled => if (enabled) Interactive else Default },
					backgroundColorPointer.map { _.shade })
				
				// Places the radio button on the left and the text field on the right
				Vector(radioButton, label) -> radioButton
			}
	}
}