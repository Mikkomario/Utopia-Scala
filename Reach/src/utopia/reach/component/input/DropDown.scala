package utopia.reach.component.input

import utopia.flow.datastructure.immutable.View
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.{ChangingLike, Fixed}
import utopia.genesis.event.{MouseButtonStateEvent, MouseEvent}
import utopia.genesis.handling.MouseButtonStateListener
import utopia.inception.handling.HandlerType
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.{MutableViewTextLabel, ViewTextLabel}
import utopia.reach.component.template.Focusable.FocusWrapper
import utopia.reach.component.template.{CursorDefining, Focusable, ReachComponentLike}
import utopia.reach.component.wrapper.OpenComponent
import utopia.reach.cursor.CursorType.Interactive
import utopia.reflection.color.{ColorRole, ColorShadeVariant}
import utopia.reflection.color.ColorRole.Secondary
import utopia.reflection.component.context.{ScrollingContextLike, TextContextLike}
import utopia.reflection.component.template.display.Refreshable
import utopia.reflection.container.stack.StackLayout
import utopia.reflection.container.stack.StackLayout.Fit
import utopia.reflection.image.SingleColorIcon
import utopia.reflection.localization.{DisplayFunction, LocalizedString}
import utopia.reflection.shape.stack.StackLength
import utopia.reflection.util.ComponentCreationDefaults
import utopia.reflection.shape.LengthExtensions._

/**
  * A field used for selecting a value from a predefined list of options
  * @author Mikko Hilpinen
  * @since 23.12.2020, v1
  */
object DropDown extends ContextInsertableComponentFactoryFactory[TextContextLike, DropDownFactory,
	ContextualDropDownFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new DropDownFactory(hierarchy)
}

class DropDownFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[TextContextLike, ContextualDropDownFactory]
{
	override def withContext[N <: TextContextLike](context: N) =
		ContextualDropDownFactory(parentHierarchy, context)
}

case class ContextualDropDownFactory[+N <: TextContextLike](parentHierarchy: ComponentHierarchy, context: N)
	extends ContextualComponentFactory[N, TextContextLike, ContextualDropDownFactory]
{
	override def withContext[N2 <: TextContextLike](newContext: N2) = copy(context = newContext)
	
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
	  * @tparam A Type of selectable item
	  * @tparam C Type of component inside the field
	  * @tparam P Type of content pointer used
	  * @return A new field
	  */
	def apply[A, C <: ReachComponentLike with Refreshable[A], P <: ChangingLike[Vector[A]]]
	(contentPointer: P, valuePointer: PointerWithEvents[Option[A]] = new PointerWithEvents[Option[A]](None),
	 rightExpandIcon: Option[SingleColorIcon] = None,
	 rightCollapseIcon: Option[SingleColorIcon] = None,
	 displayFunction: DisplayFunction[Option[A]] = DisplayFunction.rawOption,
	 fieldNamePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
	 promptPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
	 hintPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
	 errorMessagePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
	 leftIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
	 listLayout: StackLayout = Fit, listCap: StackLength = StackLength.fixedZero,
	 noOptionsView: Option[OpenComponent[ReachComponentLike, Any]] = None,
	 highlightStylePointer: ChangingLike[Option[ColorRole]] = Fixed(None),
	 focusColorRole: ColorRole = Secondary, sameItemCheck: Option[(A, A) => Boolean] = None,
	 fillBackground: Boolean = ComponentCreationDefaults.useFillStyleFields)
	(makeDisplay: (ComponentHierarchy, A) => C)(implicit scrollingContext: ScrollingContextLike) =
	{
		val isEmptyPointer = valuePointer.map { _.isEmpty }
		val actualPromptPointer = promptPointer.notFixedWhere { _.isEmpty }
			.map { _.mergeWith(isEmptyPointer) { (prompt, isEmpty) => if (isEmpty) prompt else LocalizedString.empty } }
			.getOrElse(promptPointer)
		val field = FieldWithSelectionPopup(parentHierarchy).withContext(context)
			.apply[A, FocusWrapper[ViewTextLabel[Option[A]]], C, P](isEmptyPointer, contentPointer, valuePointer,
				rightExpandIcon, rightCollapseIcon, fieldNamePointer, actualPromptPointer, hintPointer,
				errorMessagePointer, leftIconPointer, listLayout, listCap, noOptionsView, highlightStylePointer,
				focusColorRole, sameItemCheck, fillBackground)
				{ (fieldContext, context) =>
					val actualStylePointer = fieldContext.textStylePointer.map { _.expandingHorizontally }
					val label = ViewTextLabel(fieldContext.parentHierarchy).apply(valuePointer, actualStylePointer,
						displayFunction, additionalDrawers = fieldContext.promptDrawers,
						allowLineBreaks = context.allowLineBreaks, allowTextShrink = context.allowTextShrink)
					// Makes sure the label doesn't have to resize itself when displaying various options
					val maxContentWidthPointer = contentPointer.lazyMap {
						_.view.map { c => label.calculatedStackSizeWith(displayFunction(Some(c))) }.reduceOption { _ max _ }
					}
					label.addConstraint { original =>
						maxContentWidthPointer.value match
						{
							case Some(maxContentSize) => original max maxContentSize
							case None => original
						}
					}
					// Wraps the label as a focusable component
					Focusable.wrap(label, Vector(fieldContext.focusListener))
				}(makeDisplay) { (_, _) => None }
		// Adds mouse interaction to the field
		field.addMouseButtonListener(new FieldFocusMouseListener(field.field))
		CursorDefining.defineCursorFor(field, View(Interactive), field.field.innerBackgroundPointer.lazyMap { c =>
			ColorShadeVariant.forLuminosity(c.luminosity) })
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
	  * @tparam A Type of selectable item
	  * @tparam P Type of content pointer used
	  * @return A new field
	  */
	def simple[A, P <: ChangingLike[Vector[A]]]
	(contentPointer: P, valuePointer: PointerWithEvents[Option[A]] = new PointerWithEvents[Option[A]](None),
	 rightExpandIcon: Option[SingleColorIcon] = None, rightCollapseIcon: Option[SingleColorIcon] = None,
	 displayFunction: DisplayFunction[A] = DisplayFunction.raw,
	 fieldNamePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
	 promptPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
	 hintPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
	 errorMessagePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
	 noOptionsView: Option[OpenComponent[ReachComponentLike, Any]] = None,
	 highlightStylePointer: ChangingLike[Option[ColorRole]] = Fixed(None), focusColorRole: ColorRole = Secondary,
	 sameItemCheck: Option[(A, A) => Boolean] = None,
	 fillBackground: Boolean = ComponentCreationDefaults.useFillStyleFields)
	(implicit scrollingContext: ScrollingContextLike) =
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
				MutableViewTextLabel(hierarchy).withContext(context).apply(firstItem, displayFunction)
			}
	}
	
	// TODO: Add a variant that also displays an icon
}

private class FieldFocusMouseListener(field: Field[_]) extends MouseButtonStateListener
{
	// ATTRIBUTES	-------------------
	
	override val mouseButtonStateEventFilter = MouseButtonStateEvent.leftPressedFilter &&
		MouseEvent.isOverAreaFilter(field.bounds)
	
	
	// IMPLEMENTED	-------------------
	
	override def onMouseButtonState(event: MouseButtonStateEvent) =
	{
		field.requestFocus()
		None
	}
	
	override def allowsHandlingFrom(handlerType: HandlerType) = !field.hasFocus
}

