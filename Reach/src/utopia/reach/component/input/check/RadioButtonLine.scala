package utopia.reach.component.input.check

import utopia.firmament.context.text.VariableTextContext
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.localization.LocalizedString
import utopia.flow.collection.immutable.Pair
import utopia.flow.view.immutable.View
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.genesis.handling.event.consume.ConsumeChoice.Consume
import utopia.genesis.handling.event.mouse.MouseButtonStateListener
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.Mixed
import utopia.reach.component.factory.contextual.{ContextualFactory, VariableBackgroundRoleAssignableFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.component.template.CursorDefining
import utopia.reach.container.multi.Stack
import utopia.reach.cursor.CursorType.{Default, Interactive}

case class RadioButtonLineSetup(settings: RadioButtonSettings = RadioButtonSettings.default)
	extends RadioButtonSettingsWrapper[RadioButtonLineSetup]
		with Ccff[VariableTextContext, ContextualRadioButtonLineFactory]
{
	// IMPLEMENTED  --------------------
	
	override def withContext(hierarchy: ComponentHierarchy, context: VariableTextContext): ContextualRadioButtonLineFactory =
		ContextualRadioButtonLineFactory(hierarchy, context, settings)
	
	override def withSettings(settings: RadioButtonSettings): RadioButtonLineSetup = copy(settings = settings)
}

/**
 * Used for constructing radio buttons with labels
 * @author Mikko Hilpinen
 * @since 30.1.2021, v0.1
 */
object RadioButtonLine extends RadioButtonLineSetup()
{
	def apply(settings: RadioButtonSettings) = withSettings(settings)
}

case class ContextualRadioButtonLineFactory(parentHierarchy: ComponentHierarchy, context: VariableTextContext,
                                            settings: RadioButtonSettings = RadioButtonSettings.default,
                                            drawsBackground: Boolean = false)
	extends RadioButtonSettingsWrapper[ContextualRadioButtonLineFactory]
		with ContextualFactory[VariableTextContext, ContextualRadioButtonLineFactory]
		with VariableBackgroundRoleAssignableFactory[VariableTextContext, ContextualRadioButtonLineFactory]
{
	// IMPLEMENTED  ------------------------------------
	
	override def withContext(c: VariableTextContext): ContextualRadioButtonLineFactory =
		copy(context = c)
	override def withSettings(settings: RadioButtonSettings): ContextualRadioButtonLineFactory =
		copy(settings = settings)
	
	override protected def withVariableBackgroundContext(newContext: VariableTextContext,
	                                                     backgroundDrawer: CustomDrawer): ContextualRadioButtonLineFactory =
		copy(context = newContext, settings = settings.withCustomBackgroundDrawer(backgroundDrawer),
			drawsBackground = true)
	
	
	// OTHER    ----------------------------------------
	
	/**
	 * Creates a new radio button & label
	 * @param selectedValuePointer A mutable pointer to the selected value
	 * @param value Value represented by this radio button
	 * @param labelText Text displayed on the label
	 * @tparam A Type of selected value
	 * @return A new radio button and associated label in a stack. The created radio button is added
	  *         as an additional result.
	 */
	def apply[A](selectedValuePointer: EventfulPointer[A], value: A, labelText: LocalizedString) =
	{
		val stack = Stack(parentHierarchy).withContext(context).centeredRow
			// The custom drawers are assigned to this whole component
			.withCustomDrawers(customDrawers).withoutMargin
			.build(Mixed) { factories =>
				val radioButton = factories(RadioButton).withoutCustomDrawers.apply(selectedValuePointer, value)
				val label = factories(ViewTextLabel)
					.withIsHintPointer(settings.enabledPointer.lightMap { !_ })
					.text(labelText)
				// Clicking the label triggers the button
				label.addMouseButtonListener(MouseButtonStateListener.leftPressed.over { label.bounds } { _ =>
					radioButton.select()
					radioButton.requestFocus()
					Consume("Radio button selected via label")
				})
				// Adds mouse functionality to the label
				CursorDefining.defineCursorFor(label, View { if (enabledPointer.value) Interactive else Default },
					View { context.backgroundPointer.value.shade })
				
				// Places the radio button on the left and the text field on the right
				Pair(radioButton, label) -> radioButton
			}
		// Repaints the component when drawn background color changes (if applicable)
		if (drawsBackground)
			context.backgroundPointer.addListenerWhile(parentHierarchy.linkPointer) { _ => stack.repaint() }
		stack
	}
}