package utopia.reach.component.interactive.input.selection

import utopia.firmament.context.ScrollingContext
import utopia.firmament.context.text.VariableTextContext
import utopia.firmament.localization.{Display, LocalizedString}
import utopia.firmament.model.enumeration.StackLayout.Leading
import utopia.firmament.model.enumeration.WindowResizePolicy.Program
import utopia.flow.collection.immutable.Single
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.immutable.View
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.handling.event.consume.ConsumeChoice.Preserve
import utopia.genesis.handling.event.keyboard.Key.{DownArrow, RightArrow, Space}
import utopia.genesis.handling.event.mouse.{MouseButtonStateEvent, MouseButtonStateListener, MouseEvent}
import utopia.reach.component.factory.ContextualMixed
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.contextual.VariableTextContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.interactive.input.FieldWithPopup
import utopia.reach.component.interactive.input.selection.DropDownSetup.defaultSettings
import utopia.reach.component.interactive.input.selection.FieldFocusMouseListener.visibilityChangeThreshold
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.component.template.focus.Focusable
import utopia.reach.component.template.focus.Focusable.FocusWrapper
import utopia.reach.component.template.{CursorDefining, PartOfComponentHierarchy, ReachComponent}
import utopia.reach.context.{ReachWindowContext, VariableReachContentWindowContext}
import utopia.reach.cursor.CursorType.{Default, Interactive}

import scala.concurrent.ExecutionContext

object DropDownSetup
{
	lazy val defaultSettings = FieldWithSelectionPopupSettings.default
		.withAdditionalActivationKeys(Set(Space, RightArrow, DownArrow))
}
case class DropDownSetup(settings: FieldWithSelectionPopupSettings = defaultSettings)
	extends FieldWithSelectionPopupSettingsWrapper[DropDownSetup]
		with Ccff[VariableReachContentWindowContext, DropDownFactory]
{
	override def withSettings(settings: FieldWithSelectionPopupSettings): DropDownSetup = copy(settings = settings)
	
	override def withContext(hierarchy: ComponentHierarchy,
	                         context: VariableReachContentWindowContext): DropDownFactory =
		DropDownFactory(hierarchy, context, settings)
}

/**
  * A field used for selecting a value from a predefined list of options
  * @author Mikko Hilpinen
  * @since 23.12.2020, v0.1
  */
object DropDown extends DropDownSetup()

case class DropDownFactory(hierarchy: ComponentHierarchy, context: VariableTextContext,
                           settings: FieldWithSelectionPopupSettings = defaultSettings)
	extends VariableTextContextualFactory[DropDownFactory]
		with FieldWithSelectionPopupSettingsWrapper[DropDownFactory] with PartOfComponentHierarchy
{
	// IMPLEMENTED  --------------------------------
	
	override def self: DropDownFactory = this
	
	override def withContext(p: VariableTextContext): DropDownFactory = copy(context = p)
	override def withSettings(settings: FieldWithSelectionPopupSettings): DropDownFactory =
		copy(settings = settings)
		
	
	// OTHER    -------------------------------------
	
	/**
	  * Creates a new field that utilizes a selection pop-up
	  * @param contentPointer Pointer to the available options in this field
	  * @param valuePointer Pointer to the currently selected option, if any (default = new empty pointer)
	  * @param display Display function to use for converting selectable values to text (default = use toString)
	 * @param makeItemView   A function that constructs an individual view for a selectable item.
	 *                       Receives:
	 *                          1. Component factories (with a variable text context)
	 *                          1. Content pointer to display
	 *                          1. A flag that contains true while this item is selected
	 *                          1. Index of this item (0-based)
	  * @param scrollingContext   Context used for the created scroll view
	  * @param exc                Context used for parallel operations
	  * @param windowContext Context used as the basis for constructing the pop-up window.
	 *                      Will be adjusted to be borderless and not resizable.
	 * @param equals Implicit equals function for the selected items. Defaults to ==.
	  * @tparam A Type of selectable item
	  * @return A new drop-down field
	  */
	def apply[A](contentPointer: Changing[Seq[A]], valuePointer: EventfulPointer[Option[A]] = Pointer.eventful.empty,
	             display: Display[Option[A]] = Display.identity.optional)
	            (makeItemView: (ContextualMixed[VariableTextContext], Changing[A], Flag, Int) => ReachComponent)
	            (implicit scrollingContext: ScrollingContext, exc: ExecutionContext, windowContext: ReachWindowContext,
	             equals: EqualsFunction[A] = EqualsFunction.default) =
	{
		// Prepares the settings & properties
		val emptyFlag: Flag = valuePointer.lightMap { _.isEmpty }
		val appliedPromptPointer = promptPointer.notFixedWhere { _.isEmpty } match {
			case Some(pointer) =>
				pointer.mergeWith(emptyFlag) { (prompt, isEmpty) => if (isEmpty) prompt else LocalizedString.empty }
			case None => LocalizedString.alwaysEmpty
		}
		val appliedSettings = settings.withPromptPointer(appliedPromptPointer)
		
		// Creates the wrapped field
		val field = FieldWithSelectionPopup.withContext(hierarchy, context).withSettings(appliedSettings)
			.withPopupContext(windowContext.borderless.withResizeLogic(Program))
			.apply[A, FocusWrapper[ViewTextLabel[Option[A]]]](emptyFlag, contentPointer, valuePointer) {
				fieldContext =>
					// The wrapped field is a simple label
					val label = fieldContext(ViewTextLabel)
						.mapContext { _.withTextExpandingToRight.withoutVerticalTextInsets }
						.withAdditionalCustomDrawers(fieldContext.promptDrawers)
						.apply(valuePointer, display)
					// Wraps the label as a focusable component
					Focusable.wrap(label, Single(fieldContext.focusListener))
				}(makeItemView) { _ => None }
		
		// Adds mouse interaction to the field
		field.addMouseButtonListener(new FieldFocusMouseListener(field.wrapped, enabledFlag))
		CursorDefining.defineCursorFor(field, View { if (enabledFlag.value) Interactive else Default },
			field.field.innerBackgroundPointer.map { _.shade })
		
		field
	}
	
	/**
	  * Creates a new field that utilizes a selection pop-up and uses text labels for displaying options
	  * @param contentPointer Pointer to the available options in this field
	  * @param valuePointer Pointer to the currently selected option, if any (default = new empty pointer)
	  * @param display Display function to use for converting selectable values to text (default = use toString)
	  * @param scrollingContext Context used for the created scroll view
	  * @param exc              Context used for parallel operations
	 * @param windowContext     Context used as the basis for constructing the pop-up window.
	 *                          Will be adjusted to be borderless and not resizable.
	 * @param equals Implicit equals function for the selected items. Defaults to ==.
	 * @tparam A Type of selectable item
	  * @return A new field
	  */
	def labels[A](contentPointer: Changing[Seq[A]], valuePointer: EventfulPointer[Option[A]] = EventfulPointer.empty,
	              display: Display[A] = Display.identity)
	             (implicit scrollingContext: ScrollingContext, exc: ExecutionContext, windowContext: ReachWindowContext,
	              equals: EqualsFunction[A] = EqualsFunction.default) =
	{
		// Makes sure some selection-drawing is applied
		val appliedSettings = {
			if (settings.selectionDrawer.isDefined)
				settings
			else
				settings.withSelectionDrawer(SelectionDrawer.highlight(context))
		}
		withSettings(appliedSettings.withSelectionLayout(Leading))
			.apply[A](contentPointer, valuePointer, display.optional) {
				(factories, contentP, _, _) => factories(ViewTextLabel).apply(contentP, display)
			}
	}
}

private object FieldFocusMouseListener
{
	// Time before pop-up visibility may be swapped
	private val visibilityChangeThreshold = 0.2.seconds
}
private class FieldFocusMouseListener(field: FieldWithPopup[_], enabledFlag: Flag)
	extends MouseButtonStateListener
{
	// ATTRIBUTES	-------------------
	
	override val mouseButtonStateEventFilter =
		MouseButtonStateEvent.filter.leftPressed && MouseEvent.filter.over(field.bounds)
	
	
	// IMPLEMENTED	-------------------
	
	override def handleCondition: Flag = enabledFlag
	
	override def onMouseButtonStateEvent(event: MouseButtonStateEvent) = {
		// Requests focus or opens the field, except when the pop-up was just closed
		if (field.field.hasFocus && field.lastPopupCloseTime < Now - visibilityChangeThreshold)
			field.showPopup()
		else
			field.requestFocus()
		Preserve
	}
}

