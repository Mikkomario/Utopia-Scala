package utopia.reach.component.input.selection

import utopia.firmament.component.display.Refreshable
import utopia.firmament.context.{ComponentCreationDefaults, ScrollingContext}
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.{DisplayFunction, LocalizedString}
import utopia.firmament.model.enumeration.StackLayout
import utopia.firmament.model.enumeration.StackLayout.Fit
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackLength
import utopia.flow.operator.EqualsFunction
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.event.{MouseButtonStateEvent, MouseEvent}
import utopia.genesis.handling.MouseButtonStateListener
import utopia.inception.handling.HandlerType
import utopia.paradigm.color.ColorRole.Secondary
import utopia.paradigm.color.{ColorRole, ColorShade}
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.ReachContentWindowContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.input.FieldWithSelectionPopup
import utopia.reach.component.label.text.{MutableViewTextLabel, ViewTextLabel}
import utopia.reach.component.template.focus.Focusable
import utopia.reach.component.template.focus.Focusable.FocusWrapper
import utopia.reach.component.template.{CursorDefining, ReachComponentLike}
import utopia.reach.component.wrapper.OpenComponent
import utopia.reach.context.ReachContentWindowContext
import utopia.reach.cursor.CursorType.Interactive

import java.awt.event.KeyEvent
import scala.concurrent.ExecutionContext

/**
  * A field used for selecting a value from a predefined list of options
  * @author Mikko Hilpinen
  * @since 23.12.2020, v0.1
  */
object DropDown extends Ccff[ReachContentWindowContext, ContextualDropDownFactory]
{
	override def withContext(hierarchy: ComponentHierarchy, context: ReachContentWindowContext) =
		ContextualDropDownFactory(hierarchy, context)
}

case class ContextualDropDownFactory(parentHierarchy: ComponentHierarchy, context: ReachContentWindowContext)
	extends ReachContentWindowContextualFactory[ContextualDropDownFactory]
{
	private implicit val c: ReachContentWindowContext = context
	
	override def self: ContextualDropDownFactory = this
	
	override def withContext(newContext: ReachContentWindowContext) = copy(context = newContext)
	
	// TODO: Add enabled pointer parameter
	
	/**
	  * Creates a new field that utilizes a selection pop-up
	  * @param contentPointer Pointer to the available options in this field
	  * @param valuePointer Pointer to the currently selected option, if any (default = new empty pointer)
	  * @param rightExpandIcon Icon indicating that this selection may be expanded (optional)
	  * @param rightCollapseIcon Icon indicating that this selection may be collapsed (optional)
	  * @param displayFunction Display function to use for converting selectable values to text (default = use toString)
	  * @param fieldNamePointer A pointer to the displayed name of this field (default = always empty)
	  * @param promptPointer A pointer to the prompt displayed on this field (default = always empty)
	  * @param hintPointer A pointer to the hint displayed under this field (default = always empty)
	  * @param errorMessagePointer A pointer to the error message displayed on this field (default = always empty)
	  * @param leftIconPointer A pointer to the icon displayed on the left side of this component (default = always None)
	  * @param listLayout Stack layout used in the selection list (default = Fit)
	  * @param listCap Cap placed at each end of the selection list (default = always 0)
	  * @param noOptionsView A view to display when there are no options to choose from (optional, in open form)
	  * @param highlightStylePointer A pointer to an additional highlighting style applied to this field (default = always None)
	  * @param focusColorRole Color role used when this field has focus (default = Secondary)
	  * @param sameItemCheck A function for checking whether two options represent the same instance (optional).
	  *                      Should only be specified when equality function (==) shouldn't be used.
	  * @param fillBackground Whether filled field style should be used (default = global default)
	  * @param makeDisplay        A function for constructing new item option fields in the pop-up selection list.
	  *                           Accepts a component hierarcy, and the item to display initially.
	  *                           Returns a field that can display such a value.
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
	 rightExpandIcon: Option[SingleColorIcon] = None,
	 rightCollapseIcon: Option[SingleColorIcon] = None,
	 displayFunction: DisplayFunction[Option[A]] = DisplayFunction.rawOption,
	 fieldNamePointer: Changing[LocalizedString] = Fixed(LocalizedString.empty),
	 promptPointer: Changing[LocalizedString] = Fixed(LocalizedString.empty),
	 hintPointer: Changing[LocalizedString] = Fixed(LocalizedString.empty),
	 errorMessagePointer: Changing[LocalizedString] = Fixed(LocalizedString.empty),
	 leftIconPointer: Changing[Option[SingleColorIcon]] = Fixed(None),
	 listLayout: StackLayout = Fit, listCap: StackLength = StackLength.fixedZero,
	 noOptionsView: Option[OpenComponent[ReachComponentLike, Any]] = None,
	 highlightStylePointer: Changing[Option[ColorRole]] = Fixed(None),
	 focusColorRole: ColorRole = Secondary, sameItemCheck: Option[EqualsFunction[A]] = None,
	 fillBackground: Boolean = ComponentCreationDefaults.useFillStyleFields)
	(makeDisplay: (ComponentHierarchy, A) => C)
	(implicit scrollingContext: ScrollingContext, exc: ExecutionContext, log: Logger) =
	{
		val isEmptyPointer = valuePointer.map { _.isEmpty }
		val actualPromptPointer = promptPointer.notFixedWhere { _.isEmpty }
			.map { _.mergeWith(isEmptyPointer) { (prompt, isEmpty) => if (isEmpty) prompt else LocalizedString.empty } }
			.getOrElse(promptPointer)
		val field = FieldWithSelectionPopup.withContext(parentHierarchy, context)
			.apply[A, FocusWrapper[ViewTextLabel[Option[A]]], C, P](isEmptyPointer, contentPointer, valuePointer,
				rightExpandIcon, rightCollapseIcon, fieldNamePointer, actualPromptPointer, hintPointer,
				errorMessagePointer, leftIconPointer, listLayout, listCap, noOptionsView, highlightStylePointer,
				focusColorRole, Set(KeyEvent.VK_SPACE, KeyEvent.VK_RIGHT), sameItemCheck, fillBackground)
				{ (fieldContext, context) =>
					val actualStylePointer = fieldContext.textStylePointer.map { _.expandingHorizontally }
					val label = ViewTextLabel(fieldContext.parentHierarchy).apply(valuePointer, actualStylePointer,
						displayFunction, customDrawers = fieldContext.promptDrawers,
						allowTextShrink = context.allowTextShrink)
					// Makes sure the label doesn't have to resize itself when displaying various options
					val maxContentWidthPointer = contentPointer.lazyMap {
						_.view.map { c => label.calculatedStackSizeWith(displayFunction(Some(c))) }.reduceOption { _ max _ }
					}
					label.addConstraint { original =>
						maxContentWidthPointer.value match {
							case Some(maxContentSize) => original max maxContentSize
							case None => original
						}
					}
					// Wraps the label as a focusable component
					Focusable.wrap(label, Vector(fieldContext.focusListener))
				}(makeDisplay) { (_, _) => None }
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
	  * @param rightExpandIcon Icon indicating that this selection may be expanded (optional)
	  * @param rightCollapseIcon Icon indicating that this selection may be collapsed (optional)
	  * @param displayFunction Display function to use for converting selectable values to text (default = use toString)
	  * @param fieldNamePointer A pointer to the displayed name of this field (default = always empty)
	  * @param promptPointer A pointer to the prompt displayed on this field (default = always empty)
	  * @param hintPointer A pointer to the hint displayed under this field (default = always empty)
	  * @param errorMessagePointer A pointer to the error message displayed on this field (default = always empty)
	  * @param noOptionsView A view to display when there are no options to choose from (optional, in open form)
	  * @param highlightStylePointer A pointer to an additional highlighting style applied to this field (default = always None)
	  * @param focusColorRole Color role used when this field has focus (default = Secondary)
	  * @param sameItemCheck A function for checking whether two options represent the same instance (optional).
	  *                      Should only be specified when equality function (==) shouldn't be used.
	  * @param fillBackground Whether filled field style should be used (default = global default)
	  * @param scrollingContext Context used for the created scroll view
	  * @param exc              Context used for parallel operations
	  * @param log              Logger for various errors
	  * @tparam A Type of selectable item
	  * @tparam P Type of content pointer used
	  * @return A new field
	  */
	def simple[A, P <: Changing[Vector[A]]]
	(contentPointer: P, valuePointer: PointerWithEvents[Option[A]] = new PointerWithEvents[Option[A]](None),
	 rightExpandIcon: Option[SingleColorIcon] = None, rightCollapseIcon: Option[SingleColorIcon] = None,
	 displayFunction: DisplayFunction[A] = DisplayFunction.raw,
	 fieldNamePointer: Changing[LocalizedString] = Fixed(LocalizedString.empty),
	 promptPointer: Changing[LocalizedString] = Fixed(LocalizedString.empty),
	 hintPointer: Changing[LocalizedString] = Fixed(LocalizedString.empty),
	 errorMessagePointer: Changing[LocalizedString] = Fixed(LocalizedString.empty),
	 noOptionsView: Option[OpenComponent[ReachComponentLike, Any]] = None,
	 highlightStylePointer: Changing[Option[ColorRole]] = Fixed(None), focusColorRole: ColorRole = Secondary,
	 sameItemCheck: Option[EqualsFunction[A]] = None,
	 fillBackground: Boolean = ComponentCreationDefaults.useFillStyleFields)
	(implicit scrollingContext: ScrollingContext, exc: ExecutionContext, log: Logger) =
	{
		val mainDisplayFunction = DisplayFunction.wrap[Option[A]] {
			case Some(item) => displayFunction(item)
			case None => LocalizedString.empty
		}
		// TODO: Display constructor assumes wrong background color
		apply[A, MutableViewTextLabel[A], P](contentPointer, valuePointer, rightExpandIcon, rightCollapseIcon,
			mainDisplayFunction, fieldNamePointer, promptPointer, hintPointer, errorMessagePointer, Fixed(None), Fit,
			context.margins.small.any, noOptionsView, highlightStylePointer, focusColorRole, sameItemCheck,
			fillBackground)
			{ (hierarchy, firstItem) =>
				MutableViewTextLabel(hierarchy).withContext(context.withTextExpandingToRight)
					.apply(firstItem, displayFunction)
			}
	}
	
	// TODO: Add a variant that also displays an icon
}

private class FieldFocusMouseListener(field: FieldWithSelectionPopup[_, _, _, _, _]) extends MouseButtonStateListener
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

