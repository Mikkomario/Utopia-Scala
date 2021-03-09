package utopia.reach.component.input

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.{Changing, ChangingLike, Fixed}
import utopia.flow.util.CollectionExtensions._
import utopia.genesis.event.KeyStateEvent
import utopia.genesis.shape.Axis.Y
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape1D.Direction1D
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{FocusableWithState, ReachComponentWrapper}
import utopia.reach.container.Stack
import utopia.reflection.color.{ColorRole, ComponentColor}
import utopia.reflection.color.ColorRole.Secondary
import utopia.reflection.component.context.TextContext
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.template.display.Pool
import utopia.reflection.component.template.input.InputWithPointer
import utopia.reflection.localization.LocalizedString

object RadioButtonGroup extends ContextInsertableComponentFactoryFactory[TextContext, RadioButtonGroupFactory,
	ContextualRadioButtonGroupFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new RadioButtonGroupFactory(hierarchy)
}

class RadioButtonGroupFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[TextContext, ContextualRadioButtonGroupFactory]
{
	override def withContext[N <: TextContext](context: N) =
		ContextualRadioButtonGroupFactory(parentHierarchy, context)
}

case class ContextualRadioButtonGroupFactory[+N <: TextContext](parentHierarchy: ComponentHierarchy, context: N)
	extends ContextualComponentFactory[N, TextContext, ContextualRadioButtonGroupFactory]
{
	// IMPLICIT	----------------------------
	
	private implicit def c: TextContext = context
	
	
	// IMPLEMENTED	------------------------
	
	override def withContext[N2 <: TextContext](newContext: N2) =
		copy(context = newContext)
	
	
	// OTHER	----------------------------
	
	/**
	  * Creates a new radio button group
	  * @param options Selectable options
	  * @param direction Group direction (Y = options are stacked vertically, X = options are stacked horizontally)
	  *                  (default = Y)
	  * @param selectedColorRole Color role that represents the selected state in these buttons (default = Secondary)
	  * @param backgroundColorPointer A pointer to the surrounding container background color
	  *                               (default = determined by context)
	  * @param customDrawers Custom drawers assigned to this group (default = empty)
	  * @tparam A Type of selected value
	  * @return A new radio button group
	  */
	def apply[A](options: Vector[(A, LocalizedString)], direction: Axis2D = Y,
				 selectedColorRole: ColorRole = Secondary,
				 backgroundColorPointer: ChangingLike[ComponentColor] = Fixed(context.containerBackground),
				 customDrawers: Vector[CustomDrawer] = Vector()) =
	{
		if (options.isEmpty)
			throw new IllegalArgumentException("There must be at least one available option")
		else
			new RadioButtonGroup[A](parentHierarchy, options, options.head._1, backgroundColorPointer, direction,
				selectedColorRole, customDrawers)
	}
}

/**
  * A user input based on a multitude of radio buttons
  * @author Mikko Hilpinen
  * @since 9.3.2021, v10.7
  */
class RadioButtonGroup[A](parentHierarchy: ComponentHierarchy, options: Vector[(A, LocalizedString)],
						  initialValue: A, backgroundColorPointer: ChangingLike[ComponentColor], direction: Axis2D = Y,
						  selectedColorRole: ColorRole = Secondary, customDrawers: Vector[CustomDrawer] = Vector())
						 (implicit context: TextContext)
	extends ReachComponentWrapper with Pool[Vector[A]] with InputWithPointer[A, Changing[A]] with FocusableWithState
{
	// ATTRIBUTES	------------------------
	
	override val content = options.map { _._1 }
	private val _valuePointer = new PointerWithEvents(initialValue)
	
	private val (_wrapped, buttons) = Stack(parentHierarchy)
		.withContext(if (direction == Y) context else context.expandingToRight)
		.build(RadioButtonLine)(direction, customDrawers = customDrawers) { lineF =>
			// Creates a line for each option
			val lines = options.map { case (item, text) => lineF(_valuePointer, item, text, selectedColorRole,
				backgroundColorPointer = backgroundColorPointer) }
			// Collects the radio buttons as additional results
			lines.map { _.parent } -> lines.map { _.result }
		}.parentAndResult
	
	
	// INITIAL CODE	------------------------
	
	// While focused, allows the user to change items with arrow keys
	if (buttons.size > 1)
	{
		addFilteredKeyListenerWhileFocused(KeyStateEvent.arrowKeysFilter) { event =>
			event.arrowAlong(direction).foreach { direction =>
				buttons.indexWhereOption { _.hasFocus }.foreach { currentFocusIndex =>
					// Moves the focus according to the arrow key (if possible)
					val nextFocusIndex = currentFocusIndex + direction.sign.modifier
					buttons.getOption(nextFocusIndex).foreach { _.requestFocus(forceFocusLeave = true) }
				}
			}
		}
	}
	
	
	// IMPLEMENTED	------------------------
	
	override protected def wrapped = _wrapped
	
	override def valuePointer = _valuePointer.view
	
	override def hasFocus = buttons.exists { _.hasFocus }
	
	override def focusId = hashCode()
	
	override def focusListeners = Vector()
	
	override def allowsFocusEnter = false
	
	override def allowsFocusLeave = true
	
	override def requestFocus(forceFocusLeave: Boolean, forceFocusEnter: Boolean) =
		if (!hasFocus) buttons.headOption.exists { _.requestFocus(forceFocusLeave, forceFocusEnter) } else true
	
	override def yieldFocus(direction: Direction1D, forceFocusLeave: Boolean) =
		buttons.find { _.hasFocus }.foreach { _.yieldFocus(direction, forceFocusLeave) }
}
