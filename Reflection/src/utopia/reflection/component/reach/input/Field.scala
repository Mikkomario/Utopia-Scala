package utopia.reflection.component.reach.input

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.{ChangeListener, ChangingLike, Fixed}
import utopia.genesis.color.Color
import utopia.genesis.shape.Axis.X
import utopia.genesis.shape.shape2D.Insets
import utopia.reflection.color.ColorRole.{Error, Secondary}
import utopia.reflection.color.{ColorRole, ColorScheme, ComponentColor}
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.view.{BackgroundViewDrawer, BorderViewDrawer, TextViewDrawer2}
import utopia.reflection.component.reach.factory.Mixed
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.label.{ViewTextLabel, ViewTextLabelFactory}
import utopia.reflection.component.reach.template.{Focusable, ReachComponent, ReachComponentLike, ReachComponentWrapper}
import utopia.reflection.component.reach.wrapper.{ComponentCreationResult, Open, OpenComponent}
import utopia.reflection.container.reach.{Framing, FramingFactory, ViewStack}
import utopia.reflection.container.swing.ReachCanvas
import utopia.reflection.event.{FocusChangeEvent, FocusChangeListener}
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.stack.{StackInsets, StackLength}
import utopia.reflection.shape.{Alignment, Border}
import utopia.reflection.text.{Font, FontMetricsContext, MeasuredText}
import utopia.reflection.util.Priority.High
import utopia.reflection.shape.LengthExtensions._

case class FieldCreationContext(parentHierarchy: ComponentHierarchy, focusListener: FocusChangeListener,
								textStylePointer: ChangingLike[TextDrawContext], promptDrawers: Vector[CustomDrawer])

/**
  * Wraps another component in an interactive container that indicates an input field
  * @author Mikko Hilpinen
  * @since 14.11.2020, v2
  */
class Field[C <: ReachComponentLike with Focusable, R <: ReachComponentLike]
(parentHierarchy: ComponentHierarchy, colorScheme: ColorScheme, isEmptyPointer: ChangingLike[Boolean],
 contextBackgroundPointer: ChangingLike[ComponentColor], font: Font, alignment: Alignment = Alignment.Left,
 textInsets: StackInsets = StackInsets.any, fieldNamePointer: Option[ChangingLike[LocalizedString]] = None,
 promptPointer: Option[ChangingLike[LocalizedString]] = None, hintPointer: Option[ChangingLike[LocalizedString]] = None,
 errorMessagePointer: Option[ChangingLike[LocalizedString]] = None,
 highlightStylePointer: ChangingLike[Option[ColorRole]] = Fixed(None), focusColorRole: ColorRole = Secondary,
 defaultBorderWidth: Double = 1, focusBorderWidth: Double = 3,
 hintScaleFactor: Double = TextField.defaultHintScaleFactor, betweenLinesMargin: Double = 0.0,
 fillBackground: Boolean = true, allowLineBreaks: Boolean = false)
(makeField: FieldCreationContext => C)
(makeRightHintLabel: C => Option[OpenComponent[R, Any]])
	extends ReachComponentWrapper with Focusable
{
	// ATTRIBUTES	------------------------------------------
	
	private implicit def c: ReachCanvas = parentHierarchy.top
	
	private lazy val defaultHintInsets = textInsets.expandingHorizontallyAccordingTo(alignment)
		.mapVertical { _ * hintScaleFactor }
	
	private val _focusPointer = new PointerWithEvents(false)
	
	// Displays an error if there is one, otherwise displays the hint (provided there is one). None if neither is used.
	private lazy val actualHintTextPointer = hintPointer match
	{
		case Some(hint) =>
			errorMessagePointer match
			{
				case Some(error) => Some(hint.mergeWith(error) { (hint, error) => error.notEmpty getOrElse hint })
				case None => Some(hint)
			}
		case None => errorMessagePointer
	}
	private lazy val hintVisibilityPointer = actualHintTextPointer.map { _.map { _.nonEmpty } }
	
	// A pointer to whether this field currently highlights an error
	private val errorStatePointer = errorMessagePointer.map { _.map { _.nonEmpty } }
	private val externalHighlightStatePointer = errorStatePointer match
	{
		case Some(errorPointer) =>
			highlightStylePointer.mergeWith(errorPointer) { (custom, isError) => if (isError) Some(Error) else custom }
		case None => highlightStylePointer
	}
	private val highlightStatePointer = _focusPointer.mergeWith(externalHighlightStatePointer) { (focus, custom) =>
		custom.orElse { if (focus) Some(focusColorRole) else None }
	}
	// If a separate background color is used for this component, it depends from this component's state
	private val innerBackgroundPointer =
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
	private val errorHintColorPointer = errorStatePointer.map { errorPointer =>
		contextBackgroundPointer.mergeWith(errorPointer) { (background, isError) =>
			if (isError) Some(colorScheme.error.forBackground(background)) else None }
	}
	private lazy val hintColorPointer = errorHintColorPointer match
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
				ViewStack(parentHierarchy).withFixedStyle(Vector(framedMainArea.withResult(None), openHintArea),
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
	
	override def focusId = field.focusId
	
	def wrappedField = field
	
	def hasFocus = _focusPointer.value
	
	def focusPointer = _focusPointer.view
	
	
	// IMPLEMENTED	------------------------------------------
	
	override protected def wrapped: ReachComponent = _wrapped
	
	override def focusListeners = field.focusListeners
	
	override def allowsFocusEnter = field.allowsFocusEnter
	
	override def allowsFocusLeave = field.allowsFocusLeave
	
	
	// OTHER	----------------------------------------------
	
	private def makeContentFraming[C2 <: ReachComponentLike](factory: FramingFactory, content: OpenComponent[C2, C]) =
	{
		// Wraps the field component in a Framing (that applies border)
		// Top and side inset are increased if border is drawn on all sides
		val borderInsets = if (fillBackground) Insets.bottom(focusBorderWidth) else Insets.symmetric(focusBorderWidth)
		// val textStylePointer = editTextColorPointer.mergeWith(textInsetsPointer)(makeTextStyle)
		// Draws background (optional) and border
		val drawers =
		{
			if (fillBackground)
				Vector(makeBackgroundDrawer(), borderDrawer)
			else
				Vector(borderDrawer)
		}
		factory(content, borderInsets.fixed, drawers)
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
				val promptDrawer = promptPointer.map { promptPointer =>
					val promptContentPointer = promptPointer.map(measureText)
					val displayedPromptPointer = promptContentPointer.mergeWith(_focusPointer) { (prompt, focus) =>
						if (focus) prompt else emptyText }
					TextViewDrawer2(displayedPromptPointer, promptStylePointer)
				}
				
				val wrappedField = makeField(FieldCreationContext(factories.next().parentHierarchy, FocusTracker,
					textStylePointer, Vector(namePromptDrawer) ++ promptDrawer))
				
				// Displays one or both of the items
				Vector(ComponentCreationResult(nameLabel, Some(nameVisibilityPointer)),
					ComponentCreationResult(wrappedField, None)) -> wrappedField
			}
		}
	}
	
	// Returns input area + editable label
	private def makeInputArea() =
	{
		// Input part may contain a name label, if enabled
		fieldNamePointer match
		{
			case Some(fieldNamePointer) => makeContentAndNameArea(fieldNamePointer)
			case None =>
				val textStylePointer = editTextColorPointer.map { makeTextStyle(_, textInsets) }
				// May draw a prompt while the field is empty (or starting with the prompt text)
				val promptDrawer = promptPointer.map { promptPointer =>
					val promptStylePointer = textStylePointer.map { _.mapColor { _.timesAlpha(0.66) } }
					val displayedPromptPointer = promptPointer.map(measureText)
					TextViewDrawer2(displayedPromptPointer, promptStylePointer)
				}
				Open { hierarchy =>
					val field = makeField(FieldCreationContext(hierarchy, FocusTracker, textStylePointer,
						promptDrawer.toVector))
					field -> field
				}
		}
	}
	
	// Returns the generated open component (if any), along with its visibility pointer (if applicable)
	private def makeHintArea(wrappedField: C): Option[OpenComponent[ReachComponentLike, Option[ChangingLike[Boolean]]]] =
	{
		// In some cases, displays both message field and extra right side label
		// In other cases only the message field (which is hidden while empty)
		makeRightHintLabel(wrappedField) match
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
							Vector(hintLabel.withResult(hintVisibilityPointer), rightComponent.withResult(None)), X,
							margin = StackLength.any, cap = cap).parent
						}
						Some(stack.withResult(None))
					// Case: Only the right hint element should be displayed
					case None => Some(rightComponent.withResult(None))
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
		TextDrawContext(font * hintScaleFactor, textColor, alignment, insets, betweenLinesMargin * hintScaleFactor)
	}
	
	private def makeTextStyle(color: Color, insets: StackInsets) = TextDrawContext(font, color, alignment,
		insets, betweenLinesMargin)
	
	private def makeBackgroundDrawer() = BackgroundViewDrawer(innerBackgroundPointer.lazyMap { c => c })
	
	private def measureText(text: LocalizedString) = MeasuredText(text,
		FontMetricsContext(parentHierarchy.fontMetrics(font), betweenLinesMargin), alignment, allowLineBreaks)
	
	
	// NESTED	-----------------------------------
	
	private object FocusTracker extends FocusChangeListener
	{
		// Updates focus status
		override def onFocusChangeEvent(event: FocusChangeEvent) = _focusPointer.value = event.hasFocus
	}
}
