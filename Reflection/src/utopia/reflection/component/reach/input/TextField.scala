package utopia.reflection.component.reach.input

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.Changing
import utopia.flow.util.StringExtensions._
import utopia.genesis.color.Color
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.Axis.X
import utopia.genesis.shape.shape2D.Insets
import utopia.reflection.color.ColorRole.{Error, Secondary}
import utopia.reflection.color.{ColorRole, ColorScheme, ComponentColor}
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.view.{BackgroundViewDrawer, BorderViewDrawer, SelectableTextViewDrawer}
import utopia.reflection.component.reach.factory.Mixed
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.label.{ViewTextLabel, ViewTextLabelFactory}
import utopia.reflection.component.reach.template.ReachComponentWrapper
import utopia.reflection.component.template.input.InputWithPointer
import utopia.reflection.container.reach.{ViewStack, ViewStackFactory}
import utopia.reflection.event.FocusStateTracker
import utopia.reflection.localization.{DisplayFunction, LocalizedString}
import utopia.reflection.shape.{Alignment, Border}
import utopia.reflection.shape.stack.{StackInsets, StackLength}
import utopia.reflection.text.{Font, FontMetricsContext, MeasuredText, Regex}
import utopia.reflection.localization.LocalString._
import utopia.reflection.shape.stack.modifier.MaxBetweenModifier
import utopia.reflection.util.ComponentCreationDefaults

import scala.concurrent.duration.Duration

/**
  * Used for requesting text input from the user
  * @author Mikko Hilpinen
  * @since 14.11.2020, v2
  */
// TODO: Hint pointer and error pointers should be optional
class TextField[A](parentHierarchy: ComponentHierarchy, actorHandler: ActorHandler, colorScheme: ColorScheme,
				   contextBackgroundPointer: Changing[ComponentColor], defaultWidth: StackLength,
				   font: Font, alignment: Alignment = Alignment.Left, textInsets: StackInsets = StackInsets.any,
				   fieldNamePointer: Option[Changing[LocalizedString]] = None,
				   promptPointer: Option[Changing[LocalizedString]] = None,
				   hintPointer: Changing[LocalizedString] = Changing.wrap(LocalizedString.empty),
				   errorMessagePointer: Changing[LocalizedString] = Changing.wrap(LocalizedString.empty),
				   textPointer: PointerWithEvents[String] = new PointerWithEvents(""),
				   selectedTextColorPointer: Changing[Color] = Changing.wrap(Color.textBlack),
				   selectionBackgroundPointer: Changing[Option[Color]] = Changing.wrap(None),
				   highlightStylePointer: Changing[Option[ColorRole]] = Changing.wrap(None),
				   focusColorRole: ColorRole = Secondary, defaultBorderWidth: Double = 1, focusBorderWidth: Double = 3,
				   hintScaleFactor: Double = 0.5, caretWidth: Double = 1.0,
				   caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
				   betweenLinesMargin: Double = 0.0, inputFilter: Option[Regex] = None,
				   resultFilter: Option[Regex] = None, maxLength: Option[Int] = None,
				   enabledPointer: Changing[Boolean] = Changing.wrap(true), fillBackground: Boolean = true,
				   allowLineBreaks: Boolean = true, allowTextShrink: Boolean = false,
				   showCharacterCount: Boolean = false)
				  (parseResult: Option[String] => A)
	extends ReachComponentWrapper with InputWithPointer[A, Changing[A]]
{
	// ATTRIBUTES	------------------------------------------
	
	private val defaultHintInsets = textInsets.expandingHorizontallyAccordingTo(alignment)
		.mapVertical { _ * hintScaleFactor }
	
	private val focusTracker = new FocusStateTracker(false)
	
	// Displays an error if there is one, otherwise displays the hint (provided there is one)
	private val actualHintTextPointer = hintPointer.mergeWith(errorMessagePointer) { (hint, error) =>
		error.notEmpty getOrElse hint }
	private val hintVisibilityPointer = actualHintTextPointer.map { _.nonEmpty }
	
	// A pointer to whether this field currently highlights an error
	private val errorStatePointer = errorMessagePointer.map { _.nonEmpty }
	private val externalHighlightStatePointer =
		highlightStylePointer.mergeWith(errorStatePointer) { (custom, isError) => if (isError) Some(Error) else custom }
	private val highlightStatePointer = focusPointer.mergeWith(externalHighlightStatePointer) { (focus, custom) =>
		custom.orElse { if (focus) Some(focusColorRole) else None }
	}
	private val highlightColorPointer = highlightStatePointer
		.mergeWith(contextBackgroundPointer) { (state, background) =>
			state.map { s => colorScheme(s).forBackground(background) } }
	// If a separate background color is used for this component, it depends from this component's state
	private val innerBackgroundPointer =
	{
		// TODO: Handle mouse over state (highlights one more time)
		if (fillBackground)
			contextBackgroundPointer.mergeWith(focusPointer) { (context, focus) =>
				val base = context.highlighted
				if (focus) base.highlighted else base
			}
		else
			contextBackgroundPointer
	}
	
	private val editTextColorPointer = innerBackgroundPointer.map { _.defaultTextColor }
	private val contentColorPointer: Changing[Color] = highlightColorPointer
		.mergeWith(editTextColorPointer) { (highlight, default) =>
			highlight match
			{
				case Some(color) => color: Color
				case None => default
			}
		}
	private val defaultHintColorPointer = contextBackgroundPointer.map { _.textColorStandard.hintTextColor }
	private val errorHintColorPointer = contextBackgroundPointer.mergeWith(errorStatePointer) { (background, isError) =>
		if (isError) Some(colorScheme.error.forBackground(background)) else None }
	private val hintColorPointer = defaultHintColorPointer.mergeWith(errorHintColorPointer) { (default, error) =>
		error match
		{
			case Some(color) => color: Color
			case None => default
		}
	}
	private val hintTextStylePointer = hintColorPointer.map { makeHintStyle(_) }
	
	private val borderPointer =
	{
		// Border widths at 0 => No border is drawn
		if (defaultBorderWidth <= 0 && focusBorderWidth <= 0)
			Changing.wrap(Border.zero)
		// When using filled background style, only draws the bottom border which varies in style based state
		else if (fillBackground)
		{
			// In case both focus and default borders share the same width, doesn't listen to the focus state
			if (defaultBorderWidth == focusBorderWidth)
				contentColorPointer.map { Border.bottom(defaultBorderWidth, _) }
			// Otherwise uses a different height border when focused
			else
				contentColorPointer.mergeWith(focusPointer) { (color, focus) =>
					Border.bottom(if (focus) focusBorderWidth else defaultBorderWidth, color)
				}
		}
		else if (defaultBorderWidth == focusBorderWidth)
			contentColorPointer.map { color => Border.symmetric(defaultBorderWidth, color) }
		else
			contentColorPointer.mergeWith(focusPointer) { (color, focus) =>
				Border.symmetric(if (focus) focusBorderWidth else defaultBorderWidth, color)
			}
	}
	private val borderDrawer = BorderViewDrawer(borderPointer)
	
	override protected val wrapped = ViewStack(parentHierarchy).builder(Mixed)
		.withFixedStyle(margin = StackLength.fixedZero) { factories =>
			// Input part may contain a name label, if enabled
			val inputPart = fieldNamePointer match
			{
				case Some(fieldNamePointer) => makeTextAndNameArea(factories.next()(ViewStack), fieldNamePointer)
				case None => makeTextLabelOnly(factories.next()(EditableTextLabel))
			}
			val hintPartAndPointer = makeHintArea(factories.next())
			// Input part is above and below it is hint part, which may sometimes be hidden
			Vector(inputPart -> None, hintPartAndPointer)
		}.parent
	
	override val valuePointer = resultFilter match
	{
		case Some(filter) => textPointer.map { text => parseResult(filter.filter(text).notEmpty) }
		case None => textPointer.map { text => parseResult(text.notEmpty) }
	}
	
	
	// INITIAL CODE	------------------------------------------
	
	// Will not shrink below the default width
	wrapped.addConstraintOver(X)(MaxBetweenModifier(defaultWidth))
	
	
	// COMPUTED	----------------------------------------------
	
	def hasFocus = focusTracker.hasFocus
	
	def focusPointer = focusTracker.focusPointer
	
	
	// OTHER	----------------------------------------------
	
	// Creates the main label in situations where there is no field name label to handle
	private def makeTextLabelOnly(factory: EditableTextLabelFactory) =
	{
		// Text insets always expand horizontally
		val baseInsets = textInsets.expandingHorizontallyAccordingTo(alignment)
		// Top and side inset are increased if border is drawn on all sides
		val borderInsets = if (fillBackground) Insets.bottom(focusBorderWidth) else Insets.symmetric(focusBorderWidth)
		val insets = baseInsets + borderInsets
		
		val textStylePointer = editTextColorPointer.map { makeTextStyle(_, insets) }
		val label = makeMainTextLabel(factory, textStylePointer)
		
		// Draws background (optional) and border
		if (fillBackground)
			label.addCustomDrawer(makeBackgroundDrawer())
		label.addCustomDrawer(borderDrawer)
		
		// May draw a prompt while the field is empty
		promptPointer.foreach { promptPointer =>
			val emptyText = measureText(LocalizedString.empty)
			val promptStylePointer = textStylePointer.map { _.mapColor { _.timesAlpha(0.66) } }
			val displayedPromptPointer = promptPointer.mergeWith(textPointer) { (prompt, text) =>
				if (text.isEmpty) measureText(prompt) else emptyText }
			label.addCustomDrawer(SelectableTextViewDrawer(displayedPromptPointer, promptStylePointer))
		}
		
		label
	}
	
	private def makeTextAndNameArea(factories: ViewStackFactory, fieldNamePointer: Changing[LocalizedString]) =
	{
		val drawers = if (fillBackground) Vector(makeBackgroundDrawer(), borderDrawer) else Vector(borderDrawer)
		factories.builder(Mixed).withFixedStyle(margin = StackLength.fixedZero,
			cap = StackLength.fixed(focusBorderWidth), customDrawers = drawers) { factories =>
			// Creates the field name label first
			// Field name is displayed when
			// a) it is available AND
			// b) The edit label has focus OR c) The edit label is empty
			val nameShouldBeSeparatePointer = focusPointer.mergeWith(textPointer) { _ || _.nonEmpty }
			val nameVisibilityPointer = fieldNamePointer.mergeWith(nameShouldBeSeparatePointer) { _.nonEmpty && _ }
			val nameStylePointer = contentColorPointer.map { makeHintStyle(_, !fillBackground) }
			val nameLabel = factories.next()(ViewTextLabel).forText(fieldNamePointer, nameStylePointer,
				allowLineBreaks = false, allowTextShrink = true)
			
			// When displaying only the input label, accommodates name label size increase into the vertical insets
			// While displaying both, applies only half of the main text insets at top
			val baseTextInsets = textInsets.expandingHorizontallyAccordingTo(alignment)
			val defaultTextInsets =
			{
				if (fillBackground)
					baseTextInsets
				else
					baseTextInsets + Insets.horizontal(focusBorderWidth)
			}
			val comboTextInsets = defaultTextInsets.mapTop { _ / 2 }
			val requiredIncrease = comboTextInsets.vertical + nameLabel.stackSize.height - defaultTextInsets.vertical
			val individualTextInsets = defaultTextInsets.mapVertical { _ + requiredIncrease / 2 }
			val textStylePointer = editTextColorPointer.mergeWith(nameVisibilityPointer) { (color, nameIsVisible) =>
				makeTextStyle(color, if (nameIsVisible) comboTextInsets else individualTextInsets)
			}
			val textLabel = makeMainTextLabel(factories.next()(EditableTextLabel), textStylePointer)
			
			// While only the text label is being displayed, shows the field name as a prompt. Otherwise may show
			// the other specified prompt (if defined)
			val promptStylePointer = textStylePointer.map { _.mapColor { _.timesAlpha(0.66) } }
			val emptyText = measureText(LocalizedString.empty)
			// Only draws the name while it is not displayed elsewhere
			val namePromptPointer = fieldNamePointer.mergeWith(nameShouldBeSeparatePointer) { (name, isSeparate) =>
				if (isSeparate) emptyText else measureText(name) }
			textLabel.addCustomDrawer(SelectableTextViewDrawer(namePromptPointer, promptStylePointer))
			
			// May also display another prompt while the field has focus and is empty (not blocked by name or text)
			promptPointer.foreach { promptPointer =>
				val shouldDisplayPromptPointer = textPointer.mergeWith(focusPointer) { _.isEmpty && _ }
				val additionalPromptPointer = shouldDisplayPromptPointer.mergeWith(promptPointer) { (display, content) =>
					if (display) measureText(content) else emptyText }
				textLabel.addCustomDrawer(SelectableTextViewDrawer(additionalPromptPointer, promptStylePointer))
			}
			
			// Displays one or both of the items
			Vector(nameLabel -> Some(nameVisibilityPointer), textLabel -> None)
		}.parent
	}
	
	// Returns the generated component, along with its visibility pointer (if applicable)
	private def makeHintArea(factories: Mixed) =
	{
		// In some cases, displays both message field and character count label
		// In other cases only the message field (which is hidden while empty)
		(if (showCharacterCount) None else maxLength) match
		{
			// Case: Character count should be displayed => Always displays at least the counter
			case Some(maxLength) =>
				// Places caps to stack equal to horizontal content margin
				val cap = textInsets.horizontal / 2 + (if (fillBackground) 0 else defaultBorderWidth)
				val stack = factories(ViewStack).builder(ViewTextLabel).withFixedStyle(X,
					margin = StackLength.any, cap = cap) { labelFactories =>
					val hintLabel = makeHintLabel(labelFactories.next())
					
					val countStylePointer = defaultHintColorPointer.map { color =>
						TextDrawContext(font * hintScaleFactor, color, Alignment.Right, textInsets * hintScaleFactor) }
					val textLengthPointer = textPointer.map { _.length }
					val countLabel = labelFactories.next()(textLengthPointer, countStylePointer,
						DisplayFunction.noLocalization[Int] { length => s"$length / $maxLength".noLanguage })
					
					// Hint label is only displayed while there is a hint to display,
					// Count label is always displayed
					Vector(hintLabel -> Some(hintVisibilityPointer), countLabel -> None)
				}.parent
				stack -> None
			// Case: Only hint label should be displayed (only when there's a message to show)
			case None => makeHintLabel(factories(ViewTextLabel)) -> Some(hintVisibilityPointer)
		}
	}
	
	private def makeMainTextLabel(factory: EditableTextLabelFactory, stylePointer: Changing[TextDrawContext]) =
	{
		val label = factory.apply(actorHandler, stylePointer, selectedTextColorPointer, selectionBackgroundPointer,
		contentColorPointer, caretWidth, caretBlinkFrequency, textPointer, inputFilter, maxLength, enabledPointer,
		allowSelectionWhileDisabled = false, allowLineBreaks, allowTextShrink)
		
		label.addFocusListener(focusTracker)
		label
	}
	
	private def makeHintLabel(factory: ViewTextLabelFactory) =
		factory.forText(actualHintTextPointer, hintTextStylePointer, allowLineBreaks = false, allowTextShrink = true)
	
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
	
	private def measureText(text: LocalizedString) = MeasuredText(text, FontMetricsContext(fontMetrics(font),
		betweenLinesMargin), alignment, allowLineBreaks)
}
