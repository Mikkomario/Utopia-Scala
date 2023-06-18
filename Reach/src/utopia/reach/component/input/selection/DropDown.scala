package utopia.reach.component.input.selection

import utopia.firmament.component.display.Refreshable
import utopia.firmament.context.{ScrollingContext, TextContext}
import utopia.firmament.localization.{DisplayFunction, LocalizedString}
import utopia.flow.operator.EqualsFunction
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.event.{MouseButtonStateEvent, MouseEvent}
import utopia.genesis.handling.MouseButtonStateListener
import utopia.inception.handling.HandlerType
import utopia.paradigm.color.ColorShade
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.contextual.VariableContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.input.{FieldWithSelectionPopup, FieldWithSelectionPopupSettings, FieldWithSelectionPopupSettingsWrapper}
import utopia.reach.component.label.text.{MutableViewTextLabel, ViewTextLabel}
import utopia.reach.component.template.focus.Focusable
import utopia.reach.component.template.focus.Focusable.FocusWrapper
import utopia.reach.component.template.{CursorDefining, ReachComponentLike}
import utopia.reach.context.ReachContentWindowContext
import utopia.reach.cursor.CursorType.Interactive

import java.awt.event.KeyEvent
import scala.concurrent.ExecutionContext

case class DropDownSetup(settings: FieldWithSelectionPopupSettings = FieldWithSelectionPopupSettings.default)
	extends FieldWithSelectionPopupSettingsWrapper[DropDownSetup]
		with Ccff[ReachContentWindowContext, ContextualDropDownFactory]
{
	override def withSettings(settings: FieldWithSelectionPopupSettings): DropDownSetup = copy(settings = settings)
	
	override def withContext(hierarchy: ComponentHierarchy, context: ReachContentWindowContext) =
		ContextualDropDownFactory(hierarchy, Fixed(context))
	
	def withContext(hierarchy: ComponentHierarchy, contextPointer: Changing[ReachContentWindowContext]) =
		ContextualDropDownFactory(hierarchy, contextPointer)
}

/**
  * A field used for selecting a value from a predefined list of options
  * @author Mikko Hilpinen
  * @since 23.12.2020, v0.1
  */
object DropDown extends DropDownSetup()

case class ContextualDropDownFactory(parentHierarchy: ComponentHierarchy,
                                     contextPointer: Changing[ReachContentWindowContext],
                                     settings: FieldWithSelectionPopupSettings = FieldWithSelectionPopupSettings.default)
	extends VariableContextualFactory[ReachContentWindowContext, ContextualDropDownFactory]
		with FieldWithSelectionPopupSettingsWrapper[ContextualDropDownFactory]
{
	override def withContextPointer(p: Changing[ReachContentWindowContext]): ContextualDropDownFactory =
		copy(contextPointer = p)
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
	def apply[A, C <: ReachComponentLike with Refreshable[A], P <: Changing[Vector[A]]]
	(contentPointer: P, valuePointer: PointerWithEvents[Option[A]] = new PointerWithEvents[Option[A]](None),
	 displayFunction: DisplayFunction[Option[A]] = DisplayFunction.rawOption,
	 sameItemCheck: Option[EqualsFunction[A]] = None)
	(makeDisplay: (ComponentHierarchy, Changing[TextContext], A) => C)
	(implicit scrollingContext: ScrollingContext, exc: ExecutionContext, log: Logger) =
	{
		val isEmptyPointer = valuePointer.map { _.isEmpty }
		val actualPromptPointer = promptPointer.notFixedWhere { _.isEmpty } match {
			case Some(pointer) =>
				pointer.mergeWith(isEmptyPointer) { (prompt, isEmpty) => if (isEmpty) prompt else LocalizedString.empty }
			case None => LocalizedString.alwaysEmpty
		}
		val appliedSettings = settings.withPromptPointer(actualPromptPointer)
			.withAdditionalActivationKeys(Set(KeyEvent.VK_SPACE, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN))
		val field = FieldWithSelectionPopup.withContext(parentHierarchy, contextPointer).withSettings(appliedSettings)
			.apply[A, FocusWrapper[ViewTextLabel[Option[A]]], C, P](isEmptyPointer, contentPointer, valuePointer,
				sameItemCheck)
				{ fieldContext =>
					val label = ViewTextLabel
						.withContextPointer(fieldContext.parentHierarchy, fieldContext.contextPointer)
						.mapContext { _.withHorizontallyExpandingText }
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
					Focusable.wrap(label, Vector(fieldContext.focusListener))
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
	def simple[A, P <: Changing[Vector[A]]](contentPointer: P,
	                                        valuePointer: PointerWithEvents[Option[A]] = PointerWithEvents.empty(),
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
			sameItemCheck) { (hierarchy, context, firstItem) =>
			// TODO: At this time, uses static context here (modify when possible)
			MutableViewTextLabel(hierarchy).withContext(context.value.withTextExpandingToRight)
				.apply(firstItem, displayFunction)
		}
	}
	
	// TODO: Add a variant that also displays an icon
}

private class FieldFocusMouseListener(field: FieldWithSelectionPopup[_, _, _, _]) extends MouseButtonStateListener
{
	// ATTRIBUTES	-------------------
	
	override val mouseButtonStateEventFilter = MouseButtonStateEvent.leftPressedFilter &&
		MouseEvent.isOverAreaFilter(field.bounds)
	
	
	// IMPLEMENTED	-------------------
	
	override def onMouseButtonState(event: MouseButtonStateEvent) =
	{
		// Requests focus or opens the field
		if (field.field.hasFocus)
			field.openPopup()
		else
			field.requestFocus()
		None
	}
	
	override def allowsHandlingFrom(handlerType: HandlerType) = true
}

