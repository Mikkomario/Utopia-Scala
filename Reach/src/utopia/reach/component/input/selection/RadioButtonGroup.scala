package utopia.reach.component.input.selection

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.{ChangingLike, Fixed}
import utopia.genesis.event.KeyStateEvent
import utopia.genesis.handling.KeyStateListener
import utopia.genesis.shape.Axis.Y
import utopia.genesis.shape.Axis2D
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.inception.handling.HandlerType
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.input.check.RadioButtonLine
import utopia.reach.component.template.ReachComponentWrapper
import utopia.reach.container.multi.stack.Stack
import utopia.reach.focus.ManyFocusableWrapper
import utopia.reflection.color.ColorRole.Secondary
import utopia.reflection.color.{ColorRole, ComponentColor}
import utopia.reflection.component.context.TextContext
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.template.display.Pool
import utopia.reflection.component.template.input.InteractionWithPointer
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
			new RadioButtonGroup[A](parentHierarchy, options, new PointerWithEvents(options.head._1),
				backgroundColorPointer, direction, selectedColorRole, customDrawers)
	}
	
	/**
	  * Creates a new radio button group
	  * @param options Selectable options
	  * @param valuePointer A mutable pointer to the currently selected option / value
	  * @param direction Group direction (Y = options are stacked vertically, X = options are stacked horizontally)
	  *                  (default = Y)
	  * @param selectedColorRole Color role that represents the selected state in these buttons (default = Secondary)
	  * @param backgroundColorPointer A pointer to the surrounding container background color
	  *                               (default = determined by context)
	  * @param customDrawers Custom drawers assigned to this group (default = empty)
	  * @tparam A Type of selected value
	  * @return A new radio button group
	  */
	def withPointer[A](options: Vector[(A, LocalizedString)], valuePointer: PointerWithEvents[A], direction: Axis2D = Y,
					   selectedColorRole: ColorRole = Secondary,
					   backgroundColorPointer: ChangingLike[ComponentColor] = Fixed(context.containerBackground),
					   customDrawers: Vector[CustomDrawer] = Vector()) =
	{
		new RadioButtonGroup[A](parentHierarchy, options, valuePointer, backgroundColorPointer, direction,
			selectedColorRole, customDrawers)
	}
}

/**
  * A user input based on a multitude of radio buttons
  * @author Mikko Hilpinen
  * @since 9.3.2021, v0.1
  */
class RadioButtonGroup[A](parentHierarchy: ComponentHierarchy, options: Vector[(A, LocalizedString)],
						  override val valuePointer: PointerWithEvents[A],
						  backgroundColorPointer: ChangingLike[ComponentColor], direction: Axis2D = Y,
						  selectedColorRole: ColorRole = Secondary, customDrawers: Vector[CustomDrawer] = Vector())
						 (implicit context: TextContext)
	extends ReachComponentWrapper with Pool[Vector[A]] with InteractionWithPointer[A] with ManyFocusableWrapper
{
	// ATTRIBUTES	------------------------
	
	override val content = options.map { _._1 }
	
	private val (_wrapped, buttons) = Stack(parentHierarchy)
		.withContext(if (direction == Y) context else context.expandingToRight)
		.build(RadioButtonLine)(direction, customDrawers = customDrawers, areRelated = true) { lineF =>
			// Creates a line for each option
			val lines = options.map { case (item, text) => lineF(valuePointer, item, text, selectedColorRole,
				backgroundColorPointer = backgroundColorPointer) }
			// Collects the radio buttons as additional results
			lines.map { _.parent } -> lines.map { _.result }
		}.parentAndResult
	
	
	// INITIAL CODE	------------------------
	
	// While focused, allows the user to change items with arrow keys
	if (buttons.size > 1)
	{
		addHierarchyListener { isAttached =>
			if (isAttached)
				GlobalKeyboardEventHandler += ArrowKeyListener
			else
				GlobalKeyboardEventHandler -= ArrowKeyListener
		}
	}
	
	
	// IMPLEMENTED	------------------------
	
	override protected def wrapped = _wrapped
	
	override protected def focusTargets = buttons
	
	
	// NESTED	----------------------------
	
	private object ArrowKeyListener extends KeyStateListener
	{
		override val keyStateEventFilter = KeyStateEvent.wasPressedFilter && KeyStateEvent.arrowKeysFilter
		
		override def onKeyState(event: KeyStateEvent) = event.arrowAlong(direction).foreach { direction =>
			if (moveFocusInside(direction.sign, forceFocusLeave = true))
				buttons.find { _.hasFocus }.foreach { _.select() }
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType) = hasFocus
	}
}
