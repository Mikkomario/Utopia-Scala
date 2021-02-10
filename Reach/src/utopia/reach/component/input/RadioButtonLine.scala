package utopia.reach.component.input

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.{AlwaysTrue, ChangingLike, Fixed}
import utopia.genesis.event.ConsumeEvent
import utopia.genesis.handling.MouseButtonStateListener
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory, Mixed}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.ViewTextLabel
import utopia.reach.component.template.CursorDefining
import utopia.reach.container.Stack
import utopia.reach.cursor.CursorType.{Default, Interactive}
import utopia.reach.focus.FocusListener
import utopia.reflection.color.{ColorRole, ComponentColor}
import utopia.reflection.color.ColorRole.Secondary
import utopia.reflection.component.context.TextContextLike
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.container.stack.StackLayout.Center
import utopia.reflection.localization.LocalizedString

/**
 * Used for constructing radio buttons with labels
 * @author Mikko Hilpinen
 * @since 30.1.2021, v1
 */
object RadioButtonLine extends ContextInsertableComponentFactoryFactory[TextContextLike, RadioButtonLineFactory,
	ContextualRadioButtonLineFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new RadioButtonLineFactory(hierarchy)
}

class RadioButtonLineFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[TextContextLike, ContextualRadioButtonLineFactory]
{
	override def withContext[N <: TextContextLike](context: N) =
		ContextualRadioButtonLineFactory(parentHierarchy, context)
}

case class ContextualRadioButtonLineFactory[+N <: TextContextLike](parentHierarchy: ComponentHierarchy, context: N)
	extends ContextualComponentFactory[N, TextContextLike, ContextualRadioButtonLineFactory]
{
	// IMPLEMENTED  ------------------------------------
	
	override def withContext[N2 <: TextContextLike](newContext: N2) =
		copy(context = newContext)
	
	
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
	 * @return A new radio button and associated label
	 */
	def apply[A](selectedValuePointer: PointerWithEvents[A], value: A, labelText: LocalizedString,
				 selectedColorRole: ColorRole = Secondary, enabledPointer: ChangingLike[Boolean] = AlwaysTrue,
				 backgroundColorPointer: ChangingLike[ComponentColor] = Fixed(context.containerBackground),
				 customDrawers: Vector[CustomDrawer] = Vector(), focusListeners: Seq[FocusListener] = Vector()) =
	{
		Stack(parentHierarchy).withContext(context).build(Mixed)
			.row(Center, customDrawers = customDrawers, areRelated = true) { factories =>
				val radioButton = factories(RadioButton).apply(selectedValuePointer, value, selectedColorRole,
					enabledPointer, backgroundColorPointer, focusListeners = focusListeners)
				// Text color may vary
				val textColorPointer = backgroundColorPointer.mergeWith(enabledPointer) { (background, enabled) =>
					if (enabled) background.defaultTextColor else background.textColorStandard.hintTextColor
				}
				val label = factories(ViewTextLabel).withoutContext.forText(Fixed(labelText),
					textColorPointer.map { color => context.textDrawContext.copy(color = color) })
				label.addMouseButtonListener(MouseButtonStateListener.onLeftPressedInside { label.bounds } { _ =>
					radioButton.select()
					Some(ConsumeEvent("Radio button selected via label"))
				})
				// Adds mouse functionality to the label
				CursorDefining.defineCursorFor(label,
					enabledPointer.map { enabled => if (enabled) Interactive else Default },
					backgroundColorPointer.map { _.shade })
				
				// Places the radio button on the left and the text field on the right
				Vector(radioButton, label)
			}
	}
}