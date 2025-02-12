package utopia.reach.component.input.selection

import utopia.firmament.component.display.Refreshable
import utopia.firmament.context.ScrollingContext
import utopia.firmament.context.text.VariableTextContext
import utopia.firmament.localization.{DisplayFunction, LocalizedString}
import utopia.flow.collection.immutable.Single
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.handling.event.consume.ConsumeChoice.Preserve
import utopia.genesis.handling.event.keyboard.Key.{DownArrow, RightArrow, Space}
import utopia.genesis.handling.event.mouse.{MouseButtonStateEvent, MouseButtonStateListener, MouseEvent}
import utopia.paradigm.color.ColorShade
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.contextual.ContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.input.selection.FieldFocusMouseListener.visibilityChangeThreshold
import utopia.reach.component.input.{FieldWithSelectionPopup, FieldWithSelectionPopupSettings, FieldWithSelectionPopupSettingsWrapper}
import utopia.reach.component.label.text.{MutableViewTextLabel, ViewTextLabel}
import utopia.reach.component.template.focus.Focusable
import utopia.reach.component.template.focus.Focusable.FocusWrapper
import utopia.reach.component.template.{CursorDefining, ReachComponentLike}
import utopia.reach.context.VariableReachContentWindowContext
import utopia.reach.cursor.CursorType.Interactive

import scala.concurrent.ExecutionContext

case class DropDownSetup(settings: FieldWithSelectionPopupSettings = FieldWithSelectionPopupSettings.default)
	extends FieldWithSelectionPopupSettingsWrapper[DropDownSetup]
		with Ccff[VariableReachContentWindowContext, ContextualDropDownFactory]
{
	override def withSettings(settings: FieldWithSelectionPopupSettings): DropDownSetup = copy(settings = settings)
	
	override def withContext(hierarchy: ComponentHierarchy,
	                         context: VariableReachContentWindowContext): ContextualDropDownFactory =
		ContextualDropDownFactory(hierarchy, context, settings)
}

/**
  * A field used for selecting a value from a predefined list of options
  * @author Mikko Hilpinen
  * @since 23.12.2020, v0.1
  */
object DropDown extends DropDownSetup()

case class ContextualDropDownFactory(parentHierarchy: ComponentHierarchy,
                                     context: VariableReachContentWindowContext,
                                     settings: FieldWithSelectionPopupSettings = FieldWithSelectionPopupSettings.default)
	extends ContextualFactory[VariableReachContentWindowContext, ContextualDropDownFactory]
		with FieldWithSelectionPopupSettingsWrapper[ContextualDropDownFactory]
{
	override def withContext(p: VariableReachContentWindowContext): ContextualDropDownFactory = copy(context = p)
	override def withSettings(settings: FieldWithSelectionPopupSettings): ContextualDropDownFactory =
		copy(settings = settings)
	
	// TODO: Add enabled pointer parameter
	
	/**
	  * Creates a new field that utilizes a selection pop-up
	  * @param contentPointer Pointer to the available options in this field
	  * @param valuePointer Pointer to the currently selected option, if any (default = new empty pointer)
	  * @param displayFunction Display function to use for converting selectable values to text (default = use toString)
	  * @param sameItemCheck A function for checking whether two options represent the same instance (optional).
	  *                      Should only be specified when equality function (==) shouldn't be used.
	  * @param makeDisplay A function for constructing new item option fields in the pop-up selection list.
	 *                     Accepts four values:
	 *                     1) A component hierarchy,
	 *                     2) Component creation context,
	 *                     3) Background color pointer
	 *                     4) Item to display initially
	 *                     Returns a properly initialized display
	  * @param scrollingContext   Context used for the created scroll view
	  * @param exc                Context used for parallel operations
	  * @param log                Logger for various errors
	  * @tparam A Type of selectable item
	  * @tparam C Type of component inside the field
	  * @tparam P Type of content pointer used
	  * @return A new field
	  */
	def apply[A, C <: ReachComponentLike with Refreshable[A], P <: Changing[Seq[A]]]
	(contentPointer: P, valuePointer: EventfulPointer[Option[A]] = EventfulPointer[Option[A]](None),
	 displayFunction: DisplayFunction[Option[A]] = DisplayFunction.rawOption,
	 sameItemCheck: Option[EqualsFunction[A]] = None)
	(makeDisplay: (ComponentHierarchy, VariableTextContext, A) => C)
	(implicit scrollingContext: ScrollingContext, exc: ExecutionContext, log: Logger) =
	{
		val isEmptyPointer = valuePointer.map { _.isEmpty }
		val actualPromptPointer = promptPointer.notFixedWhere { _.isEmpty } match {
			case Some(pointer) =>
				pointer.mergeWith(isEmptyPointer) { (prompt, isEmpty) => if (isEmpty) prompt else LocalizedString.empty }
			case None => LocalizedString.alwaysEmpty
		}
		val appliedSettings = settings.withPromptPointer(actualPromptPointer)
			.withAdditionalActivationKeys(Set(Space, RightArrow, DownArrow))
		val field = FieldWithSelectionPopup.withContext(parentHierarchy, context).withSettings(appliedSettings)
			.apply[A, FocusWrapper[ViewTextLabel[Option[A]]], C, P](isEmptyPointer, contentPointer, valuePointer,
				sameItemCheck)
				{ fieldContext =>
					val label = ViewTextLabel
						.withContext(fieldContext.parentHierarchy, fieldContext.context)
						.mapContext { _.withHorizontallyExpandingText.withoutVerticalTextInsets }
						.withAdditionalCustomDrawers(fieldContext.promptDrawers)
						.apply(valuePointer, displayFunction)
					// Makes sure the label doesn't have to resize itself when displaying various options
					val maxContentWidthPointer = contentPointer.lazyMap {
						_.view.map { c => label.calculatedStackSizeWith(displayFunction(Some(c))) }
							.reduceOption { _ max _ }
					}
					label.addConstraint { original =>
						maxContentWidthPointer.value match {
							case Some(maxContentSize) => original max maxContentSize
							case None => original
						}
					}
					// Wraps the label as a focusable component
					Focusable.wrap(label, Single(fieldContext.focusListener))
				}(makeDisplay) { _ => None }
		// Adds mouse interaction to the field
		field.addMouseButtonListener(new FieldFocusMouseListener(field))
		CursorDefining.defineCursorFor(field, View(Interactive), field.field.innerBackgroundPointer.lazyMap { c =>
			ColorShade.forLuminosity(c.luminosity) })
		field
	}
	
	/**
	  * Creates a new field that utilizes a selection pop-up and uses text labels for displaying options
	  * @param contentPointer Pointer to the available options in this field
	  * @param valuePointer Pointer to the currently selected option, if any (default = new empty pointer)
	  * @param displayFunction Display function to use for converting selectable values to text (default = use toString)
	  * @param sameItemCheck A function for checking whether two options represent the same instance (optional).
	  *                      Should only be specified when equality function (==) shouldn't be used.
	  * @param scrollingContext Context used for the created scroll view
	  * @param exc              Context used for parallel operations
	  * @param log              Logger for various errors
	  * @tparam A Type of selectable item
	  * @tparam P Type of content pointer used
	  * @return A new field
	  */
	def simple[A, P <: Changing[Seq[A]]](contentPointer: P,
	                                        valuePointer: EventfulPointer[Option[A]] = EventfulPointer.empty,
	                                        displayFunction: DisplayFunction[A] = DisplayFunction.raw,
	                                        sameItemCheck: Option[EqualsFunction[A]] = None)
	                                       (implicit scrollingContext: ScrollingContext, exc: ExecutionContext,
	                                        log: Logger) =
	{
		val mainDisplayFunction = DisplayFunction.wrap[Option[A]] {
			case Some(item) => displayFunction(item)
			case None => LocalizedString.empty
		}
		apply[A, MutableViewTextLabel[A], P](contentPointer, valuePointer, mainDisplayFunction,
			sameItemCheck) {
			(hierarchy, context, firstItem) =>
				val labelContext = context.withTextExpandingToRight
				// TODO: At this time, uses static context here (modify when possible)
				val label = MutableViewTextLabel(hierarchy).withContext(labelContext.current)
					.apply(firstItem, displayFunction)
				labelContext.textDrawContextPointer
					.addListenerWhile(label.linkedFlag) { e => label.textDrawContext = e.newValue }
				label
		}
	}
	
	// TODO: Add a variant that also displays an icon
}

private object FieldFocusMouseListener
{
	// Time before pop-up visibility may be swapped
	private val visibilityChangeThreshold = 0.2.seconds
}
private class FieldFocusMouseListener(field: FieldWithSelectionPopup[_, _, _, _]) extends MouseButtonStateListener
{
	// ATTRIBUTES	-------------------
	
	override val mouseButtonStateEventFilter =
		MouseButtonStateEvent.filter.leftPressed && MouseEvent.filter.over(field.bounds)
	
	
	// IMPLEMENTED	-------------------
	
	override def handleCondition: Flag = AlwaysTrue
	
	override def onMouseButtonStateEvent(event: MouseButtonStateEvent) = {
		// Requests focus or opens the field, except when the pop-up was just closed
		if (field.field.hasFocus && field.popUpVisibilityLastChangedPointer.value < Now - visibilityChangeThreshold)
			field.openPopup()
		else
			field.requestFocus()
		Preserve
	}
}

