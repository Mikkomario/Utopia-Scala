package utopia.reach.component.input

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.{AlwaysFalse, AlwaysTrue, ChangeListener, ChangingLike, Fixed}
import utopia.genesis.color.Color
import utopia.genesis.image.Image
import utopia.genesis.shape.Axis.X
import utopia.genesis.shape.shape2D.{Direction2D, Insets}
import utopia.reflection.color.ColorRole.{Error, Secondary}
import utopia.reflection.color.{ColorRole, ColorScheme, ComponentColor}
import utopia.reflection.component.context.TextContextLike
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.view.{BackgroundViewDrawer, BorderViewDrawer, TextViewDrawer2}
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory, Mixed}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.text.{ViewTextLabel, ViewTextLabelFactory}
import utopia.reach.component.label.image.{ImageLabel, ViewImageLabel}
import utopia.reach.component.template.focus.{Focusable, FocusableWithPointer, FocusableWrapper}
import utopia.reach.component.template.{ReachComponent, ReachComponentLike, ReachComponentWrapper}
import utopia.reach.component.wrapper.{ComponentCreationResult, Open, OpenComponent}
import utopia.reach.container.multi.stack.{Stack, ViewStack}
import utopia.reach.container.ReachCanvas
import utopia.reach.container.wrapper.{Framing, FramingFactory}
import utopia.reach.focus.{FocusChangeEvent, FocusChangeListener}
import utopia.reach.util.Priority.High
import utopia.reflection.container.stack.StackLayout.Center
import utopia.reflection.image.SingleColorIcon
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.stack.{StackInsets, StackLength, StackSize}
import utopia.reflection.shape.{Alignment, Border}
import utopia.reflection.text.{Font, FontMetricsContext, MeasuredText}
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.util.ComponentCreationDefaults

/**
  * A set of context variables provided when creating field contents
  * @param parentHierarchy Component hierarchy to use
  * @param focusListener Focus listener to assign to the created component
  * @param textStylePointer Proposed text style to use
  * @param promptDrawers Custom drawers to assign for prompt drawing
  * @param backgroundPointer A pointer to the contextual background color
  */
case class FieldCreationContext(parentHierarchy: ComponentHierarchy, focusListener: FocusChangeListener,
								textStylePointer: ChangingLike[TextDrawContext], promptDrawers: Vector[CustomDrawer],
								backgroundPointer: ChangingLike[ComponentColor])

/**
  * A set of context variables provided when creating an additional right side label
  * @param content Field content
  * @param font Proposed font
  * @param backgroundPointer Pointer to contextual background color
  * @tparam C Type of field contents
  */
case class ExtraFieldCreationContext[C](content: C, font: Font, backgroundPointer: ChangingLike[ComponentColor])

object Field extends ContextInsertableComponentFactoryFactory[TextContextLike, FieldFactory, ContextualFieldFactory]
{
	// ATTRIBUTES	--------------------------
	
	/**
	  * Default factor used for scaling the hint elements
	  */
	val defaultHintScaleFactor = 0.7
	
	
	// IMPLEMENTED	--------------------------
	
	override def apply(hierarchy: ComponentHierarchy) = new FieldFactory(hierarchy)
}

class FieldFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[TextContextLike, ContextualFieldFactory]
{
	override def withContext[N <: TextContextLike](context: N) =
		ContextualFieldFactory(this, context)
	
	/**
	  * Creates a new field
	  * @param colorScheme Color scheme to use
	  * @param isEmptyPointer A pointer that contains true when the content of this field is considered empty
	  *                       (of text / selection etc.)
	  * @param contextBackgroundPointer A pointer that contains the background color of the parent component
	  * @param font Font used in this field and the wrapped component
	  * @param alignment Text alignment to use (default = Left)
	  * @param textInsets Insets to place around the content and other text by default (default = any, preferring zero)
	  * @param fieldNamePointer A pointer to this field's name (default = always empty)
	  * @param promptPointer A pointer to a prompt text shown on this field (default = always empty)
	  * @param hintPointer A pointer to an additional hint shown under this field (default = always empty)
	  * @param errorMessagePointer A pointer to an error message shown under this field (default = always empty)
	  * @param leftIconPointer A pointer to the icon to display on the left side of content, if any (default = always empty)
	  * @param rightIconPointer A pointer to the icon to display on the right side of content, if any (default = always empty)
	  * @param iconOutsideMargins Margins to place around the icon(s) on the outer field edges (default = any, preferring zero)
	  * @param highlightStylePointer A pointer to an extra highlight color style to apply to this field (default = always None)
	  * @param focusColorRole Color role to use for the focused state on this field (default = secondary)
	  * @param defaultBorderWidth Default width of field border(s) (default = 1)
	  * @param focusBorderWidth Width of field border(s) when focused (default = 3)
	  * @param hintScaleFactor A scaling factor to apply on displayed hint text (default = 70%)
	  * @param fillBackground Whether a filled style should be used (true) or whether an outlined style should be
	  *                       used (false) (default = true)
	  * @param makeField A function for creating field contents (accepts contextual data)
	  * @param makeRightHintLabel A function for producing an additional right edge hint field (accepts created main
	  *                           field, returns an open component or None if no label should be placed)
	  * @tparam C Type of wrapped component
	  * @return A new field
	  */
	def apply[C <: ReachComponentLike with Focusable]
	(colorScheme: ColorScheme, isEmptyPointer: ChangingLike[Boolean],
	 contextBackgroundPointer: ChangingLike[ComponentColor], font: Font, alignment: Alignment = Alignment.Left,
	 textInsets: StackInsets = StackInsets.any,
	 fieldNamePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
	 promptPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
	 hintPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
	 errorMessagePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
	 leftIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
	 rightIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
	 iconOutsideMargins: StackSize = StackSize.any, highlightStylePointer: ChangingLike[Option[ColorRole]] = Fixed(None),
	 focusColorRole: ColorRole = Secondary, defaultBorderWidth: Double = 1, focusBorderWidth: Double = 3,
	 hintScaleFactor: Double = Field.defaultHintScaleFactor,
	 fillBackground: Boolean = ComponentCreationDefaults.useFillStyleFields)
	(makeField: FieldCreationContext => C)
	(makeRightHintLabel: ExtraFieldCreationContext[C] => Option[OpenComponent[ReachComponentLike, Any]]) =
		new Field[C](parentHierarchy, colorScheme, isEmptyPointer, contextBackgroundPointer, font, alignment, textInsets,
			fieldNamePointer, promptPointer, hintPointer, errorMessagePointer, leftIconPointer, rightIconPointer,
			iconOutsideMargins, highlightStylePointer, focusColorRole, defaultBorderWidth, focusBorderWidth,
			hintScaleFactor, fillBackground)(makeField)(makeRightHintLabel)
	
	/**
	  * Creates a new field
	  * @param colorScheme Color scheme to use
	  * @param isEmptyPointer A pointer that contains true when the content of this field is considered empty
	  *                       (of text / selection etc.)
	  * @param contextBackgroundPointer A pointer that contains the background color of the parent component
	  * @param font Font used in this field and the wrapped component
	  * @param alignment Text alignment to use (default = Left)
	  * @param textInsets Insets to place around the content and other text by default (default = any, preferring zero)
	  * @param fieldNamePointer A pointer to this field's name (default = always empty)
	  * @param promptPointer A pointer to a prompt text shown on this field (default = always empty)
	  * @param hintPointer A pointer to an additional hint shown under this field (default = always empty)
	  * @param errorMessagePointer A pointer to an error message shown under this field (default = always empty)
	  * @param leftIconPointer A pointer to the icon to display on the left side of content, if any (default = always empty)
	  * @param rightIconPointer A pointer to the icon to display on the right side of content, if any (default = always empty)
	  * @param iconOutsideMargins Margins to place around the icon(s) on the outer field edges (default = any, preferring zero)
	  * @param highlightStylePointer A pointer to an extra highlight color style to apply to this field (default = always None)
	  * @param focusColorRole Color role to use for the focused state on this field (default = secondary)
	  * @param defaultBorderWidth Default width of field border(s) (default = 1)
	  * @param focusBorderWidth Width of field border(s) when focused (default = 3)
	  * @param hintScaleFactor A scaling factor to apply on displayed hint text (default = 70%)
	  * @param fillBackground Whether a filled style should be used (true) or whether an outlined style should be
	  *                       used (false) (default = true)
	  * @param makeField A function for creating field contents (accepts contextual data)
	  * @tparam C Type of wrapped component
	  * @return A new field
	  */
	def withoutExtraLabel[C <: ReachComponentLike with Focusable]
	(colorScheme: ColorScheme, isEmptyPointer: ChangingLike[Boolean],
	 contextBackgroundPointer: ChangingLike[ComponentColor], font: Font, alignment: Alignment = Alignment.Left,
	 textInsets: StackInsets = StackInsets.any,
	 fieldNamePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
	 promptPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
	 hintPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
	 errorMessagePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
	 leftIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
	 rightIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
	 iconOutsideMargins: StackSize = StackSize.any, highlightStylePointer: ChangingLike[Option[ColorRole]] = Fixed(None),
	 focusColorRole: ColorRole = Secondary, defaultBorderWidth: Double = 1, focusBorderWidth: Double = 3,
	 hintScaleFactor: Double = Field.defaultHintScaleFactor,
	 fillBackground: Boolean = ComponentCreationDefaults.useFillStyleFields)
	(makeField: FieldCreationContext => C) =
		apply[C](colorScheme, isEmptyPointer, contextBackgroundPointer, font, alignment, textInsets,
			fieldNamePointer, promptPointer, hintPointer, errorMessagePointer, leftIconPointer, rightIconPointer,
			iconOutsideMargins, highlightStylePointer, focusColorRole, defaultBorderWidth, focusBorderWidth,
			hintScaleFactor, fillBackground)(makeField) { _ => None }
}

case class ContextualFieldFactory[+N <: TextContextLike](factory: FieldFactory, context: N)
	extends ContextualComponentFactory[N, TextContextLike, ContextualFieldFactory]
{
	override def withContext[N2 <: TextContextLike](newContext: N2) = copy(context = newContext)
	
	/**
	  * Creates a new field
	  * @param isEmptyPointer A pointer that contains true when the content of this field is considered empty
	  *                       (of text / selection etc.)
	  * @param fieldNamePointer A pointer to this field's name (default = always empty)
	  * @param promptPointer A pointer to a prompt text shown on this field (default = always empty)
	  * @param hintPointer A pointer to an additional hint shown under this field (default = always empty)
	  * @param errorMessagePointer A pointer to an error message shown under this field (default = always empty)
	  * @param leftIconPointer A pointer to the icon to display on the left side of content, if any (default = always empty)
	  * @param rightIconPointer A pointer to the icon to display on the right side of content, if any (default = always empty)
	  * @param iconOutsideMargins Margins to place around the icon(s) on the outer field edges (default = determined by context)
	  * @param highlightStylePointer A pointer to an extra highlight color style to apply to this field (default = always None)
	  * @param focusColorRole Color role to use for the focused state on this field (default = secondary)
	  * @param hintScaleFactor A scaling factor to apply on displayed hint text (default = 70%)
	  * @param fillBackground Whether a filled style should be used (true) or whether an outlined style should be
	  *                       used (false) (default = true)
	  * @param makeField A function for creating field contents
	  *                  (accepts contextual data (specific context and context of this factory))
	  * @param makeRightHintLabel A function for producing an additional right edge hint field (accepts created main
	  *                           field and component creation context, returns an open component or None
	  *                           if no label should be placed)
	  * @tparam C Type of wrapped component
	  * @return A new field
	  */
	def apply[C <: ReachComponentLike with Focusable]
	(isEmptyPointer: ChangingLike[Boolean],
	 fieldNamePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
	 promptPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
	 hintPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
	 errorMessagePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
	 leftIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
	 rightIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
	 iconOutsideMargins: StackSize = context.textInsets.total / 2,
	 highlightStylePointer: ChangingLike[Option[ColorRole]] = Fixed(None),
	 focusColorRole: ColorRole = Secondary, hintScaleFactor: Double = Field.defaultHintScaleFactor,
	 fillBackground: Boolean = ComponentCreationDefaults.useFillStyleFields)
	(makeField: (FieldCreationContext, N) => C)
	(makeRightHintLabel: (ExtraFieldCreationContext[C], N) => Option[OpenComponent[ReachComponentLike, Any]]) =
	{
		val focusBorderWidth = (context.margins.verySmall / 2) max 3
		val defaultBorderWidth = focusBorderWidth / 3
		
		factory[C](context.colorScheme, isEmptyPointer, Fixed(context.containerBackground), context.font,
			context.textAlignment, context.textInsets, fieldNamePointer, promptPointer, hintPointer,
			errorMessagePointer, leftIconPointer, rightIconPointer, iconOutsideMargins, highlightStylePointer,
			focusColorRole, defaultBorderWidth, focusBorderWidth, hintScaleFactor, fillBackground) {
			makeField(_, context) } { makeRightHintLabel(_, context) }
	}
	
	/**
	  * Creates a new field
	  * @param isEmptyPointer A pointer that contains true when the content of this field is considered empty
	  *                       (of text / selection etc.)
	  * @param fieldNamePointer A pointer to this field's name (default = always empty)
	  * @param promptPointer A pointer to a prompt text shown on this field (default = always empty)
	  * @param hintPointer A pointer to an additional hint shown under this field (default = always empty)
	  * @param errorMessagePointer A pointer to an error message shown under this field (default = always empty)
	  * @param leftIconPointer A pointer to the icon to display on the left side of content, if any (default = always empty)
	  * @param rightIconPointer A pointer to the icon to display on the right side of content, if any (default = always empty)
	  * @param iconOutsideMargins Margins to place around the icon(s) on the outer field edges (default = determined by context)
	  * @param highlightStylePointer A pointer to an extra highlight color style to apply to this field (default = always None)
	  * @param focusColorRole Color role to use for the focused state on this field (default = secondary)
	  * @param hintScaleFactor A scaling factor to apply on displayed hint text (default = 70%)
	  * @param fillBackground Whether a filled style should be used (true) or whether an outlined style should be
	  *                       used (false) (default = true)
	  * @param makeField A function for creating field contents
	  *                  (accepts contextual data (specific context and context of this factory))
	  * @tparam C Type of wrapped component
	  * @return A new field
	  */
	def withoutExtraLabel[C <: ReachComponentLike with Focusable]
	(isEmptyPointer: ChangingLike[Boolean],
	 fieldNamePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
	 promptPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
	 hintPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
	 errorMessagePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
	 leftIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
	 rightIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
	 iconOutsideMargins: StackSize = context.textInsets.total / 2,
	 highlightStylePointer: ChangingLike[Option[ColorRole]] = Fixed(None),
	 focusColorRole: ColorRole = Secondary, hintScaleFactor: Double = Field.defaultHintScaleFactor,
	 fillBackground: Boolean = ComponentCreationDefaults.useFillStyleFields)
	(makeField: (FieldCreationContext, N) => C) =
		apply(isEmptyPointer, fieldNamePointer, promptPointer, hintPointer, errorMessagePointer, leftIconPointer,
			rightIconPointer, iconOutsideMargins, highlightStylePointer, focusColorRole, hintScaleFactor,
			fillBackground)(makeField) { (_, _) => None }
}

/**
  * Wraps another component in an interactive container that indicates an input field
  * @author Mikko Hilpinen
  * @since 14.11.2020, v0.1
  * @tparam C Type of wrapped field
  */
class Field[C <: ReachComponentLike with Focusable]
(parentHierarchy: ComponentHierarchy, colorScheme: ColorScheme, isEmptyPointer: ChangingLike[Boolean],
 contextBackgroundPointer: ChangingLike[ComponentColor], font: Font, alignment: Alignment = Alignment.Left,
 textInsets: StackInsets = StackInsets.any, fieldNamePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
 promptPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
 hintPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
 errorMessagePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
 leftIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
 rightIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None), iconOutsideMargins: StackSize = StackSize.any,
 highlightStylePointer: ChangingLike[Option[ColorRole]] = Fixed(None), focusColorRole: ColorRole = Secondary,
 defaultBorderWidth: Double = 1, focusBorderWidth: Double = 3,
 hintScaleFactor: Double = Field.defaultHintScaleFactor,
 fillBackground: Boolean = ComponentCreationDefaults.useFillStyleFields)
(makeField: FieldCreationContext => C)
(makeRightHintLabel: ExtraFieldCreationContext[C] => Option[OpenComponent[ReachComponentLike, Any]])
	extends ReachComponentWrapper with FocusableWrapper with FocusableWithPointer
{
	// ATTRIBUTES	------------------------------------------
	
	private implicit def c: ReachCanvas = parentHierarchy.top
	
	private lazy val defaultHintInsets = textInsets.expandingHorizontallyAccordingTo(alignment)
		.mapVertical { _ * hintScaleFactor }
	
	private val _focusPointer = new PointerWithEvents(false)
	
	// Displays an error if there is one, otherwise displays the hint (provided there is one). None if neither is used.
	private lazy val actualHintTextPointer = hintPointer.notFixedWhere { _.isEmpty } match
	{
		case Some(hint) =>
			errorMessagePointer.notFixedWhere { _.isEmpty } match
			{
				case Some(error) => Some(hint.mergeWith(error) { (hint, error) => error.notEmpty getOrElse hint })
				case None => Some(hint)
			}
		case None => errorMessagePointer.notFixedWhere { _.isEmpty }
	}
	private lazy val hintVisibilityPointer = actualHintTextPointer match
	{
		case Some(hintTextPointer) => hintTextPointer.map { _.nonEmpty }
		case None => AlwaysFalse
	}
	
	// A pointer to whether this field currently highlights an error
	private val errorStatePointer = errorMessagePointer.map { _.nonEmpty }
	private val externalHighlightStatePointer = highlightStylePointer.mergeWith(errorStatePointer) { (custom, isError) =>
		if (isError) Some(Error) else custom }
	private val highlightStatePointer = _focusPointer.mergeWith(externalHighlightStatePointer) { (focus, custom) =>
		custom.orElse { if (focus) Some(focusColorRole) else None }
	}
	// If a separate background color is used for this component, it depends from this component's state
	/**
	  * A pointer to this field's current inner background color. May vary based on state.
	  */
	val innerBackgroundPointer =
	{
		// TODO: Handle mouse over state (highlights one more time)
		if (fillBackground)
			contextBackgroundPointer.mergeWith(_focusPointer) { (context, focus) =>
				context.highlightedBy(if (focus) 0.15 else 0.075)
			}
		else
			contextBackgroundPointer
	}
	private val highlightColorPointer = highlightStatePointer
		.mergeWith(innerBackgroundPointer) { (state, background) =>
			state.map { s => colorScheme(s).forBackground(background) }
		}
	
	private val editTextColorPointer = innerBackgroundPointer.map { _.defaultTextColor }
	private val contentColorPointer: ChangingLike[Color] = highlightColorPointer
		.mergeWith(editTextColorPointer) { (highlight, default) =>
			highlight match
			{
				case Some(color) => color: Color
				case None => default.timesAlpha(0.66)
			}
		}
	private val defaultHintColorPointer = contextBackgroundPointer.map { _.textColorStandard.hintTextColor }
	private val errorHintColorPointer = contextBackgroundPointer.mergeWith(errorStatePointer) { (background, isError) =>
			if (isError) Some(colorScheme.error.forBackground(background)) else None
	}
	private lazy val hintColorPointer = errorHintColorPointer.notFixedWhere { _.isEmpty } match
	{
		case Some(errorColorPointer) =>
			defaultHintColorPointer.mergeWith(errorColorPointer) { (default, error) =>
				error match {
					case Some(color) => color: Color
					case None => default
				}
			}
		case None => defaultHintColorPointer
	}
	
	private lazy val hintTextStylePointer = hintColorPointer.map { makeHintStyle(_) }
	
	private val borderPointer =
	{
		// Border widths at 0 => No border is drawn
		if (defaultBorderWidth <= 0 && focusBorderWidth <= 0)
			Fixed(Border.zero)
		// When using filled background style, only draws the bottom border which varies in style based state
		else if (fillBackground)
		{
			// In case both focus and default borders share the same width, doesn't listen to the focus state
			if (defaultBorderWidth == focusBorderWidth)
				contentColorPointer.map { Border.bottom(defaultBorderWidth, _) }
			// Otherwise uses a different height border when focused
			else
				contentColorPointer.mergeWith(_focusPointer) { (color, focus) =>
					Border.bottom(if (focus) focusBorderWidth else defaultBorderWidth, color)
				}
		}
		else if (defaultBorderWidth == focusBorderWidth)
			contentColorPointer.map { color => Border.symmetric(defaultBorderWidth, color) }
		else
			contentColorPointer.mergeWith(_focusPointer) { (color, focus) =>
				Border.symmetric(if (focus) focusBorderWidth else defaultBorderWidth, color)
			}
	}
	private val borderDrawer = BorderViewDrawer(borderPointer)
	
	private val (_wrapped, field) =
	{
		// Creates the main area first
		val openMainArea = makeInputArea()
		// Checks whether a separate hint area is required
		val component = makeHintArea(openMainArea.result) match
		{
			// Case: Both main and hint area used => uses a view stack
			case Some(openHintArea) =>
				val framedMainArea = Open.using(Framing) { makeContentFraming(_, openMainArea) }
				ViewStack(parentHierarchy).withFixedStyle(Vector(framedMainArea.withResult(AlwaysTrue), openHintArea),
					margin = StackLength.fixedZero).parent
			// Case: Only main area used => uses framing only
			case None => makeContentFraming(Framing(parentHierarchy), openMainArea).parent
		}
		component -> openMainArea.result
	}
	private val repaintListener = ChangeListener.onAnyChange { repaint(High) }
	
	
	// INITIAL CODE	------------------------------------------
	
	_focusPointer.addListener(repaintListener)
	innerBackgroundPointer.addListener(repaintListener)
	borderPointer.addListener(repaintListener)
	
	
	// COMPUTED	----------------------------------------------
	
	/**
	  * @return The main component inside this wrapper
	  */
	def wrappedField = field
	
	
	// IMPLEMENTED	------------------------------------------
	
	/**
	  * @return A pointer to this component's focus state
	  */
	override def focusPointer = _focusPointer.view
	
	override protected def focusable = field
	
	override protected def wrapped: ReachComponent = _wrapped
	
	
	// OTHER	----------------------------------------------
	
	private def makeContentFraming[C2 <: ReachComponentLike](factory: FramingFactory, content: OpenComponent[C2, C]) =
	{
		// If extra icons are used, places them with the main content in a stack view
		val framingContent =
		{
			if (leftIconPointer.isChanging || rightIconPointer.isChanging)
				Open.using(ViewStack) { stackF =>
					stackF.withFixedStyle(Vector(
						makeOpenViewImageLabel(leftIconPointer, Direction2D.Right),
						content.withResult(AlwaysTrue),
						makeOpenViewImageLabel(rightIconPointer, Direction2D.Left)), X, Center,
						StackLength.fixedZero)
				}
			else if (leftIconPointer.value.isDefined || rightIconPointer.value.isDefined)
				Open.using(Stack) { stackF =>
					val leftLabel = leftIconPointer.value.map { makeImageLabel(content.hierarchy, _, Direction2D.Right) }
					val rightLabel = rightIconPointer.value.map { makeImageLabel(content.hierarchy, _, Direction2D.Left) }
					stackF.withoutMargin(content.mapComponent { c => Vector(leftLabel, Some(c), rightLabel).flatten },
						X, Center)
				}
			else
				content
		}
		// Wraps the field component in a Framing (that applies border)
		// Top and side inset are increased if border is drawn on all sides
		val borderInsets = if (fillBackground) Insets.bottom(focusBorderWidth) else Insets.symmetric(focusBorderWidth)
		// Draws background (optional) and border
		val drawers =
		{
			if (fillBackground)
				Vector(makeBackgroundDrawer(), borderDrawer)
			else
				Vector(borderDrawer)
		}
		factory(framingContent, borderInsets.fixed, drawers).withResult(content.result)
	}
	
	private def makeViewImageLabel(hierarchy: ComponentHierarchy, pointer: ChangingLike[Option[SingleColorIcon]],
							   noMarginSide: Direction2D) =
	{
		ViewImageLabel(hierarchy).withStaticLayout(pointer.mergeWith(innerBackgroundPointer) { (icon, bg) =>
			icon match
			{
				case Some(icon) => icon.singleColorImageAgainst(bg)
				case None => Image.empty
			}
		}, iconOutsideMargins.toInsets - noMarginSide, useLowPrioritySize = true)
	}
	
	private def makeOpenViewImageLabel(pointer: ChangingLike[Option[SingleColorIcon]], noMarginSide: Direction2D) =
		Open { makeViewImageLabel(_, pointer, noMarginSide) }.withResult(pointer.map { _.isDefined })
	
	private def makeImageLabel(hierarchy: ComponentHierarchy, icon: SingleColorIcon, noMarginSide: Direction2D) =
	{
		if (contextBackgroundPointer.isChanging)
			ViewImageLabel(hierarchy).withStaticLayout(innerBackgroundPointer.map(icon.singleColorImageAgainst),
				iconOutsideMargins.toInsets - noMarginSide, useLowPrioritySize = true)
		else
			ImageLabel(hierarchy).apply(icon.singleColorImageAgainst(contextBackgroundPointer.value),
				iconOutsideMargins.toInsets - noMarginSide, useLowPrioritySize = true)
	}
	
	private def makeContentAndNameArea(fieldNamePointer: ChangingLike[LocalizedString]) =
	{
		Open.using(ViewStack) { stackFactory =>
			stackFactory.builder(Mixed).withFixedStyle(margin = StackLength.fixedZero) { factories =>
				// Creates the field name label first
				// Field name is displayed when
				// a) it is available AND
				// b) The edit label has focus OR c) The edit label is empty
				val nameShouldBeSeparatePointer = _focusPointer.mergeWith(isEmptyPointer) { _ || !_ }
				val nameVisibilityPointer = fieldNamePointer.mergeWith(nameShouldBeSeparatePointer) { _.nonEmpty && _ }
				val nameStylePointer = contentColorPointer.map { makeHintStyle(_, !fillBackground) }
				val nameLabel = factories.next()(ViewTextLabel).forText(fieldNamePointer, nameStylePointer,
					allowLineBreaks = false, allowTextShrink = true)
				
				// When displaying only the input, accommodates name label size increase into the vertical insets
				// While displaying both, applies only half of the main text insets at top
				val comboTextInsets = textInsets.mapTop { _ / 2 }
				val requiredIncrease = comboTextInsets.vertical + nameLabel.stackSize.height - textInsets.vertical
				val individualTextInsets = textInsets.mapVertical { _ + requiredIncrease / 2 }
				val textStylePointer = editTextColorPointer.mergeWith(nameVisibilityPointer) { (color, nameIsVisible) =>
					makeTextStyle(color, if (nameIsVisible) comboTextInsets else individualTextInsets)
				}
				
				// While only the text label is being displayed, shows the field name as a prompt. Otherwise may show
				// the other specified prompt (if defined)
				val promptStylePointer = textStylePointer.map { _.mapColor { _.timesAlpha(0.66) } }
				val emptyText = measureText(LocalizedString.empty)
				// Only draws the name while it is not displayed elsewhere
				val namePromptPointer = fieldNamePointer.mergeWith(nameShouldBeSeparatePointer) { (name, isSeparate) =>
					if (isSeparate) emptyText else measureText(name) }
				val namePromptDrawer = TextViewDrawer2(namePromptPointer, promptStylePointer)
				
				// May also display another prompt while the field has focus and is empty / starting with the prompt
				// (not blocked by name or text)
				val promptDrawer = promptPointer.notFixedWhere { _.isEmpty }.map { promptPointer =>
					val promptContentPointer = promptPointer.map { measureText(_, allowLineBreaks = true) }
					val displayedPromptPointer = promptContentPointer.mergeWith(_focusPointer) { (prompt, focus) =>
						if (focus) prompt else emptyText }
					TextViewDrawer2(displayedPromptPointer, promptStylePointer)
				}
				
				val wrappedField = makeField(FieldCreationContext(factories.next().parentHierarchy, FocusTracker,
					textStylePointer, Vector(namePromptDrawer) ++ promptDrawer, innerBackgroundPointer))
				
				// Displays one or both of the items
				Vector(ComponentCreationResult(nameLabel,nameVisibilityPointer),
					ComponentCreationResult(wrappedField, AlwaysTrue)) -> wrappedField
			}
		}
	}
	
	// Returns input area + editable label
	private def makeInputArea() =
	{
		// Input part may contain a name label, if enabled
		if (fieldNamePointer.existsFixed { _.isEmpty })
		{
			val textStylePointer = editTextColorPointer.map { makeTextStyle(_, textInsets) }
			// May draw a prompt while the field is empty (or starting with the prompt text)
			val promptDrawer = promptPointer.notFixedWhere { _.isEmpty }.map { promptPointer =>
				val promptStylePointer = textStylePointer.map { _.mapColor { _.timesAlpha(0.66) } }
				val displayedPromptPointer = promptPointer.map { measureText(_, allowLineBreaks = true) }
				TextViewDrawer2(displayedPromptPointer, promptStylePointer)
			}
			Open { hierarchy =>
				val field = makeField(FieldCreationContext(hierarchy, FocusTracker, textStylePointer,
					promptDrawer.toVector, innerBackgroundPointer))
				field -> field
			}
		}
		else
			makeContentAndNameArea(fieldNamePointer)
	}
	
	// Returns the generated open component (if any), along with its visibility pointer (if applicable)
	private def makeHintArea(wrappedField: C): Option[OpenComponent[ReachComponentLike, ChangingLike[Boolean]]] =
	{
		// In some cases, displays both message field and extra right side label
		// In other cases only the message field (which is hidden while empty)
		makeRightHintLabel(ExtraFieldCreationContext(wrappedField, font * hintScaleFactor, contextBackgroundPointer)) match
		{
			case Some(rightComponent) =>
				actualHintTextPointer match
				{
					// Case: Hints are sometimes displayed
					case Some(hintTextPointer) =>
						// Places caps to stack equal to horizontal content margin
						val cap = textInsets.horizontal / 2 + (if (fillBackground) 0 else defaultBorderWidth)
						val hintLabel = Open.using(ViewTextLabel) { makeHintLabel(_, hintTextPointer) }
						val stack = Open.using(ViewStack) { _.withFixedStyle(
							Vector(hintLabel.withResult(hintVisibilityPointer), rightComponent.withResult(AlwaysTrue)), X,
							margin = StackLength.any, cap = cap).parent
						}
						Some(stack.withResult(AlwaysTrue))
					// Case: Only the right hint element should be displayed
					case None => Some(rightComponent.withResult(AlwaysTrue))
				}
			case None =>
				// Case: No additional hint should be displayed => May display a hint label still (occasionally)
				actualHintTextPointer.map { hintTextPointer =>
					Open.using(ViewTextLabel) { makeHintLabel(_, hintTextPointer) }.withResult(hintVisibilityPointer)
				}
		}
	}
	
	private def makeHintLabel(factory: ViewTextLabelFactory, textPointer: ChangingLike[LocalizedString]) =
		factory.forText(textPointer, hintTextStylePointer, allowLineBreaks = false, allowTextShrink = true)
	
	private def makeHintStyle(textColor: Color, includeHorizontalBorder: Boolean = false) =
	{
		val insets =
		{
			if (includeHorizontalBorder)
				defaultHintInsets + Insets.horizontal(focusBorderWidth)
			else
				defaultHintInsets
		}
		TextDrawContext(font * hintScaleFactor, textColor, alignment, insets)
	}
	
	private def makeTextStyle(color: Color, insets: StackInsets) = TextDrawContext(font, color, alignment, insets)
	
	private def makeBackgroundDrawer() = BackgroundViewDrawer(innerBackgroundPointer.lazyMap { c => c })
	
	private def measureText(text: LocalizedString, allowLineBreaks: Boolean = false) = MeasuredText(text,
		FontMetricsContext(parentHierarchy.fontMetrics(font), 0.0), alignment)
	
	
	// NESTED	-----------------------------------
	
	private object FocusTracker extends FocusChangeListener
	{
		// Updates focus status
		override def onFocusChangeEvent(event: FocusChangeEvent) = _focusPointer.value = event.hasFocus
	}
}
