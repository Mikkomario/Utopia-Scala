package utopia.reflection.component.reach.input

import utopia.flow.datastructure.immutable.Value
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.{ChangeListener, Changing}
import utopia.flow.util.StringExtensions._
import utopia.flow.generic.ValueConversions._
import utopia.genesis.color.Color
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.Axis.X
import utopia.genesis.shape.shape2D.Insets
import utopia.reflection.color.ColorRole.{Error, Secondary}
import utopia.reflection.color.ColorShade.Light
import utopia.reflection.color.{ColorRole, ColorScheme, ComponentColor}
import utopia.reflection.component.context.TextContextLike
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.view.{BackgroundViewDrawer, BorderViewDrawer, SelectableTextViewDrawer}
import utopia.reflection.component.reach.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory, Mixed}
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.label.{ViewTextLabel, ViewTextLabelFactory}
import utopia.reflection.component.reach.template.{MutableFocusableWrapper, ReachComponent, ReachComponentWrapper}
import utopia.reflection.component.reach.wrapper.ComponentCreationResult
import utopia.reflection.component.template.input.InputWithPointer
import utopia.reflection.container.reach.{ViewStack, ViewStackFactory}
import utopia.reflection.event.FocusEvent.FocusLost
import utopia.reflection.event.{FocusChangeEvent, FocusChangeListener}
import utopia.reflection.localization.{DisplayFunction, LocalizedString, Localizer}
import utopia.reflection.shape.{Alignment, Border}
import utopia.reflection.shape.stack.{StackInsets, StackLength}
import utopia.reflection.text.{Font, FontMetricsContext, MeasuredText, Regex}
import utopia.reflection.localization.LocalString._
import utopia.reflection.shape.stack.modifier.MaxBetweenModifier
import utopia.reflection.util.ComponentCreationDefaults

import scala.concurrent.duration.Duration

object TextField extends ContextInsertableComponentFactoryFactory[TextContextLike, TextFieldFactory,
	ContextualTextFieldFactory]
{
	/**
	  * Default factor used for scaling the hint elements
	  */
	val defaultHintScaleFactor = 0.7
	
	override def apply(hierarchy: ComponentHierarchy) = new TextFieldFactory(hierarchy)
}

class TextFieldFactory(val parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[TextContextLike, ContextualTextFieldFactory]
{
	// IMPLEMENTED	----------------------------
	
	override def withContext[N <: TextContextLike](context: N) =
		ContextualTextFieldFactory(this, context)
	
	
	// OTHER	--------------------------------
	
	/**
	  * Creates a new text field
	  * @param actorHandler Actor handler that will deliver action events for this component (for caret-blinking)
	  * @param colorScheme Color scheme used in this component
	  * @param contextBackgroundPointer A pointer to the container component's background color
	  * @param defaultWidth Standard width used for this component
	  * @param font Font used for the main text area
	  * @param alignment Text alignment used (default = Left)
	  * @param textInsets Insets placed around the main text area (default = any, preferring 0)
	  * @param fieldNamePointer A pointer to this field's name (optional)
	  * @param promptPointer A pointer to the prompt displayed on this field (optional)
	  * @param hintPointer A pointer to a hint displayed on this field (optional)
	  * @param errorMessagePointer A pointer to an error message displayed on this field (optional)
	  * @param textPointer A mutable pointer for this field's text (default = new empty pointer)
	  * @param selectedTextColorPointer A pointer to the color used in the selected text
	  *                                 (default = always use standard black text)
	  * @param selectionBackgroundPointer A pointer to the selected text area's background color, if any
	  *                                   (default = never any background)
	  * @param highlightStylePointer A pointer to a highlighting applied to this component (default = always None)
	  * @param focusColorRole Color role used when this field has focus (default = Secondary)
	  * @param defaultBorderWidth Border width used by default (default = 1 px)
	  * @param focusBorderWidth Border width used when this field has focus (default = 3 px)
	  * @param hintScaleFactor A scaling factor applied for hint texts (default = 0.5 = 50%)
	  * @param caretWidth Width of the drawn caret (default = 1 px)
	  * @param caretBlinkFrequency Frequency how often the caret changes visibility when idle (default = global default)
	  * @param betweenLinesMargin Vertical margin placed between two text lines (default = 0.0)
	  * @param inputFilter A filter applied for all input characters (optional)
	  * @param resultFilter A filter applied for resulting text (optional). Applied to field results and shown when
	  *                     this field loses focus.
	  * @param maxLength Maximum text length in number of characters (optional)
	  * @param enabledPointer A pointer to this field's enabled status (default = always enabled)
	  * @param inputValidation A function for producing an error message based on current value (optional)
	  * @param fillBackground Whether input area should be filled with color (default = true)
	  * @param allowLineBreaks Whether line breaks should be allowed in text (default = false)
	  * @param allowTextShrink Whether text should be allowed to shrink to preserve space (default = false)
	  * @param showCharacterCount Whether the current character count should be displayed in this component
	  *                           (default = false)
	  * @param parseResult A function for parsing a result from a non-empty string
	  * @tparam A Type of parsed result
	  * @return A new field
	  */
	def apply[A](actorHandler: ActorHandler, colorScheme: ColorScheme,
				 contextBackgroundPointer: Changing[ComponentColor], defaultWidth: StackLength,
				 font: Font, alignment: Alignment = Alignment.Left, textInsets: StackInsets = StackInsets.any,
				 fieldNamePointer: Option[Changing[LocalizedString]] = None,
				 promptPointer: Option[Changing[LocalizedString]] = None,
				 hintPointer: Option[Changing[LocalizedString]] = None,
				 errorMessagePointer: Option[Changing[LocalizedString]] = None,
				 textPointer: PointerWithEvents[String] = new PointerWithEvents(""),
				 selectedTextColorPointer: Changing[Color] = Changing.wrap(Color.textBlack),
				 selectionBackgroundPointer: Changing[Option[Color]] = Changing.wrap(None),
				 highlightStylePointer: Changing[Option[ColorRole]] = Changing.wrap(None),
				 focusColorRole: ColorRole = Secondary, defaultBorderWidth: Double = 1, focusBorderWidth: Double = 3,
				 hintScaleFactor: Double = TextField.defaultHintScaleFactor, caretWidth: Double = 1.0,
				 caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
				 betweenLinesMargin: Double = 0.0, inputFilter: Option[Regex] = None,
				 resultFilter: Option[Regex] = None, maxLength: Option[Int] = None,
				 enabledPointer: Changing[Boolean] = Changing.wrap(true),
				 inputValidation: Option[A => LocalizedString] = None, fillBackground: Boolean = true,
				 allowLineBreaks: Boolean = false, allowTextShrink: Boolean = false,
				 showCharacterCount: Boolean = false)
				(parseResult: Option[String] => A) =
		new TextField(parentHierarchy, actorHandler, colorScheme, contextBackgroundPointer, defaultWidth, font,
			alignment, textInsets, fieldNamePointer, promptPointer, hintPointer, errorMessagePointer, textPointer,
			selectedTextColorPointer, selectionBackgroundPointer, highlightStylePointer, focusColorRole,
			defaultBorderWidth, focusBorderWidth, hintScaleFactor, caretWidth, caretBlinkFrequency, betweenLinesMargin,
			inputFilter, resultFilter, maxLength, enabledPointer, inputValidation, fillBackground, allowLineBreaks,
			allowTextShrink, showCharacterCount)(parseResult)
}

case class ContextualTextFieldFactory[+N <: TextContextLike](factory: TextFieldFactory, context: N)
	extends ContextualComponentFactory[N, TextContextLike, ContextualTextFieldFactory]
{
	// ATTRIBUTES	--------------------------------
	
	private lazy val textMeasureContext = FontMetricsContext(factory.parentHierarchy.fontMetrics(context.font),
		context.betweenLinesMargin.optimal)
	
	
	// IMPLICIT	------------------------------------
	
	private implicit def localizer: Localizer = context.localizer
	private implicit def languageCode: String = "en"
	
	
	// IMPLEMENTED	--------------------------------
	
	override def withContext[N2 <: TextContextLike](newContext: N2) = copy(context = newContext)
	
	
	// OTHER	------------------------------------
	
	/**
	  * Creates a new text field
	  * @param defaultWidth Standard width used for this component
	  * @param fieldNamePointer A pointer to this field's name (optional)
	  * @param promptPointer A pointer to the prompt displayed on this field (optional)
	  * @param hintPointer A pointer to a hint displayed on this field (optional)
	  * @param errorMessagePointer A pointer to an error message displayed on this field (optional)
	  * @param textPointer A mutable pointer for this field's text (default = new empty pointer)
	  * @param highlightStylePointer A pointer to a highlighting applied to this component (default = always None)
	  * @param hintScaleFactor A scaling factor applied for hint texts (default = 0.5 = 50%)
	  * @param caretBlinkFrequency Frequency how often the caret changes visibility when idle (default = global default)
	  * @param inputFilter A filter applied for all input characters (optional)
	  * @param resultFilter A filter applied for resulting text (optional). Applied to field results and shown when
	  *                     this field loses focus.
	  * @param maxLength Maximum text length in number of characters (optional)
	  * @param enabledPointer A pointer to this field's enabled status (default = always enabled)
	  * @param inputValidation A function for producing an error message based on current value (optional)
	  * @param fillBackground Whether input area should be filled with color (default = true)
	  * @param showCharacterCount Whether the current character count should be displayed in this component
	  *                           (default = false)
	  * @param allowLineBreaks Whether line breaks should be allowed in text (default = context-defined)
	  * @param parseResult A function for parsing a result from a non-empty string
	  * @tparam A Type of parsed result
	  * @return A new field
	  */
	def apply[A](defaultWidth: StackLength, fieldNamePointer: Option[Changing[LocalizedString]] = None,
				 promptPointer: Option[Changing[LocalizedString]] = None,
				 hintPointer: Option[Changing[LocalizedString]] = None,
				 errorMessagePointer: Option[Changing[LocalizedString]] = None,
				 textPointer: PointerWithEvents[String] = new PointerWithEvents(""),
				 highlightStylePointer: Changing[Option[ColorRole]] = Changing.wrap(None),
				 hintScaleFactor: Double = TextField.defaultHintScaleFactor,
				 caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
				 inputFilter: Option[Regex] = None, resultFilter: Option[Regex] = None, maxLength: Option[Int] = None,
				 enabledPointer: Changing[Boolean] = Changing.wrap(true),
				 inputValidation: Option[A => LocalizedString] = None, fillBackground: Boolean = true,
				 showCharacterCount: Boolean = false, allowLineBreaks: Boolean = context.allowLineBreaks)
				(parseResult: Option[String] => A) =
	{
		val selectionBackground = context.color(Secondary, Light)
		val selectedTextColor = selectionBackground.defaultTextColor
		
		val focusBorderWidth = (context.margins.verySmall / 2) max 3
		val defaultBorderWidth = focusBorderWidth / 3
		val caretWidth = (context.margins.verySmall / 2) max 1
		
		factory[A](context.actorHandler, context.colorScheme, Changing.wrap(context.containerBackground), defaultWidth,
			context.font, context.textAlignment, context.textInsets, fieldNamePointer, promptPointer, hintPointer,
			errorMessagePointer, textPointer, Changing.wrap(selectedTextColor), Changing.wrap(Some(selectionBackground)),
			highlightStylePointer, Secondary, defaultBorderWidth, focusBorderWidth, hintScaleFactor, caretWidth,
			caretBlinkFrequency, context.betweenLinesMargin.optimal, inputFilter, resultFilter, maxLength,
			enabledPointer, inputValidation, fillBackground, context.allowLineBreaks, context.allowTextShrink,
			showCharacterCount)(parseResult)
	}
	
	/**
	  * Creates a new text field
	  * @param defaultWidth Standard width used for this component
	  * @param fieldNamePointer A pointer to this field's name (optional)
	  * @param promptPointer A pointer to the prompt displayed on this field (optional)
	  * @param hintPointer A pointer to a hint displayed on this field (optional)
	  * @param errorMessagePointer A pointer to an error message displayed on this field (optional)
	  * @param textPointer A mutable pointer for this field's text (default = new empty pointer)
	  * @param highlightStylePointer A pointer to a highlighting applied to this component (default = always None)
	  * @param hintScaleFactor A scaling factor applied for hint texts (default = 0.5 = 50%)
	  * @param caretBlinkFrequency Frequency how often the caret changes visibility when idle (default = global default)
	  * @param inputFilter A filter applied for all input characters (optional)
	  * @param resultFilter A filter applied for resulting text (optional). Applied to field results and shown when
	  *                     this field loses focus.
	  * @param maxLength Maximum text length in number of characters (optional)
	  * @param enabledPointer A pointer to this field's enabled status (default = always enabled)
	  * @param fillBackground Whether input area should be filled with color (default = true)
	  * @param showCharacterCount Whether the current character count should be displayed in this component
	  *                           (default = false)
	  * @return A new field
	  */
	def forString(defaultWidth: StackLength, fieldNamePointer: Option[Changing[LocalizedString]] = None,
				  promptPointer: Option[Changing[LocalizedString]] = None,
				  hintPointer: Option[Changing[LocalizedString]] = None,
				  errorMessagePointer: Option[Changing[LocalizedString]] = None,
				  textPointer: PointerWithEvents[String] = new PointerWithEvents(""),
				  highlightStylePointer: Changing[Option[ColorRole]] = Changing.wrap(None),
				  hintScaleFactor: Double = TextField.defaultHintScaleFactor,
				  caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
				  inputFilter: Option[Regex] = None, resultFilter: Option[Regex] = None, maxLength: Option[Int] = None,
				  enabledPointer: Changing[Boolean] = Changing.wrap(true), fillBackground: Boolean = true,
				  showCharacterCount: Boolean = false) =
	{
		apply[String](defaultWidth, fieldNamePointer, promptPointer, hintPointer, errorMessagePointer, textPointer,
			highlightStylePointer, hintScaleFactor, caretBlinkFrequency, inputFilter, resultFilter, maxLength,
			enabledPointer, None, fillBackground, showCharacterCount) { _ getOrElse "" }
	}
	
	/**
	  * Creates a new text field
	  * @param fieldNamePointer A pointer to this field's name (optional)
	  * @param promptPointer A pointer to the prompt displayed on this field (optional)
	  * @param hintPointer A pointer to a hint displayed on this field (optional)
	  * @param errorMessagePointer A pointer to an error message displayed on this field (optional)
	  * @param highlightStylePointer A pointer to a highlighting applied to this component (default = always None)
	  * @param enabledPointer A pointer to this field's enabled status (default = always enabled)
	  * @param initialValue Initially displayed value (optional)
	  * @param minValue Smallest allowed value (default = smallest possible integer value)
	  * @param maxValue Largest allowed value (default = largest possible integer value)
	  * @param hintScaleFactor A scaling factor applied for hint texts (default = 0.5 = 50%)
	  * @param caretBlinkFrequency Frequency how often the caret changes visibility when idle (default = global default)
	  * @param fillBackground Whether input area should be filled with color (default = true)
	  * @param allowAutoHint Whether automatic input value hint should be allowed (default = true)
	  * @return A new field
	  */
	def forInt(fieldNamePointer: Option[Changing[LocalizedString]] = None,
			   promptPointer: Option[Changing[LocalizedString]] = None,
			   hintPointer: Option[Changing[LocalizedString]] = None,
			   errorMessagePointer: Option[Changing[LocalizedString]] = None,
			   highlightStylePointer: Changing[Option[ColorRole]] = Changing.wrap(None),
			   enabledPointer: Changing[Boolean] = Changing.wrap(true), initialValue: Option[Int] = None,
			   minValue: Int = Int.MinValue, maxValue: Int = Int.MaxValue, hintScaleFactor: Double = TextField.defaultHintScaleFactor,
			   caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
			   fillBackground: Boolean = true, allowAutoHint: Boolean = true) =
	{
		// Only accepts integer numbers
		val inputFilter = if (minValue < 0) Regex.numericParts else Regex.digit
		val resultFilter = if (minValue < 0) Regex.numeric else Regex.numericPositive
		
		forNumbers(minValue, maxValue, inputFilter, resultFilter, fieldNamePointer, promptPointer, hintPointer,
			errorMessagePointer, highlightStylePointer, enabledPointer, initialValue, hintScaleFactor,
			caretBlinkFrequency, allowAutoHint && minValue > Int.MinValue && minValue != 0,
			allowAutoHint && maxValue < Int.MaxValue, fillBackground) { _.int }
	}
	
	/**
	  * Creates a new text field
	  * @param minValue Smallest allowed value
	  * @param maxValue Largest allowed value
	  * @param fieldNamePointer A pointer to this field's name (optional)
	  * @param promptPointer A pointer to the prompt displayed on this field (optional)
	  * @param hintPointer A pointer to a hint displayed on this field (optional)
	  * @param errorMessagePointer A pointer to an error message displayed on this field (optional)
	  * @param highlightStylePointer A pointer to a highlighting applied to this component (default = always None)
	  * @param enabledPointer A pointer to this field's enabled status (default = always enabled)
	  * @param initialValue Initially displayed value (optional)
	  * @param hintScaleFactor A scaling factor applied for hint texts (default = 0.5 = 50%)
	  * @param caretBlinkFrequency Frequency how often the caret changes visibility when idle (default = global default)
	  * @param fillBackground Whether input area should be filled with color (default = true)
	  * @param allowAutoHint Whether automatic input value hint should be allowed (default = true)
	  * @return A new field
	  */
	def forDouble(minValue: Double, maxValue: Double, fieldNamePointer: Option[Changing[LocalizedString]] = None,
				  promptPointer: Option[Changing[LocalizedString]] = None,
				  hintPointer: Option[Changing[LocalizedString]] = None,
				  errorMessagePointer: Option[Changing[LocalizedString]] = None,
				  highlightStylePointer: Changing[Option[ColorRole]] = Changing.wrap(None),
				  enabledPointer: Changing[Boolean] = Changing.wrap(true), initialValue: Option[Double] = None,
				  hintScaleFactor: Double = TextField.defaultHintScaleFactor,
				  caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
				  fillBackground: Boolean = true, allowAutoHint: Boolean = true) =
	{
		// Only accepts integer numbers
		val inputFilter = if (minValue < 0) Regex.decimalParts else Regex.decimalPositiveParts
		val resultFilter = if (minValue < 0) Regex.decimal else Regex.decimalPositive
		
		forNumbers(minValue, maxValue, inputFilter, resultFilter, fieldNamePointer, promptPointer, hintPointer,
			errorMessagePointer, highlightStylePointer, enabledPointer, initialValue, hintScaleFactor,
			caretBlinkFrequency, allowAutoHint && minValue != 0, allowAutoHint, fillBackground) { _.double }
	}
	
	private def forNumbers[A](minValue: A, maxValue: A, inputRegex: Regex, resultRegex: Regex,
							  fieldNamePointer: Option[Changing[LocalizedString]] = None,
							  promptPointer: Option[Changing[LocalizedString]] = None,
							  hintPointer: Option[Changing[LocalizedString]] = None,
							  errorMessagePointer: Option[Changing[LocalizedString]] = None,
							  highlightStylePointer: Changing[Option[ColorRole]] = Changing.wrap(None),
							  enabledPointer: Changing[Boolean] = Changing.wrap(true),
							  initialValue: Option[A] = None, hintScaleFactor: Double = TextField.defaultHintScaleFactor,
							  caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
							  markMinimumValue: Boolean = false, markMaximumValue: Boolean = false,
							  fillBackground: Boolean = true)
							 (parse: Value => Option[A])(implicit ordering: Ordering[A]) =
	{
		// Field width is based on minimum and maximum values and their lengths
		val minString = minValue.toString
		val maxString = maxValue.toString
		val maxLength = minString.length max maxString.length
		val minStringWidth = widthOf(minString)
		val maxStringWidth = widthOf(maxString)
		val defaultWidth = StackLength(minStringWidth min maxStringWidth, minStringWidth max maxStringWidth)
		
		// May display min / max values as hints
		val effectiveHintPointer =
		{
			if (markMinimumValue || markMaximumValue)
			{
				val autoHint =
				{
					if (markMaximumValue)
					{
						if (markMinimumValue)
							Some(s"$minValue - $maxValue".noLanguageLocalizationSkipped)
						else
							Some(s"Up to %s".autoLocalized.interpolated(Vector(maxValue)))
					}
					else if (markMinimumValue)
						Some(s"$minValue+".noLanguageLocalizationSkipped)
					else
						None
				}
				autoHint match
				{
					case Some(autoHint) =>
						hintPointer match
						{
							case Some(hint) => Some(hint.map { _.notEmpty.getOrElse(autoHint) })
							case None => Some(Changing.wrap(autoHint))
						}
					case None => hintPointer
				}
			}
			else
				hintPointer
		}
		val initialText = initialValue.map { _.toString }.getOrElse("")
		
		// Displays an error if the value is outside of accepted range
		def validateInput(input: Option[A]) = input match
		{
			case Some(input) =>
				if (ordering.compare(input, minValue) < 0)
					"Minimum value is %i".autoLocalized.interpolated(Vector(minValue))
				else if (ordering.compare(input, maxValue) > 0)
					"Maximum value is %i".autoLocalized.interpolated(Vector(maxValue))
				else
					LocalizedString.empty
			case None => LocalizedString.empty
		}
		
		apply[Option[A]](defaultWidth, fieldNamePointer, promptPointer, effectiveHintPointer, errorMessagePointer,
			new PointerWithEvents(initialText), highlightStylePointer, hintScaleFactor, caretBlinkFrequency,
			Some(inputRegex), Some(resultRegex), Some(maxLength), enabledPointer, Some(validateInput),
			fillBackground, allowLineBreaks = false) { parse(_) }
	}
	
	private def widthOf(text: String) = textMeasureContext.lineWidthOf(text)
}

/**
  * Used for requesting text input from the user
  * @author Mikko Hilpinen
  * @since 14.11.2020, v2
  */
class TextField[A](parentHierarchy: ComponentHierarchy, actorHandler: ActorHandler, colorScheme: ColorScheme,
				   contextBackgroundPointer: Changing[ComponentColor], defaultWidth: StackLength,
				   font: Font, alignment: Alignment = Alignment.Left, textInsets: StackInsets = StackInsets.any,
				   fieldNamePointer: Option[Changing[LocalizedString]] = None,
				   promptPointer: Option[Changing[LocalizedString]] = None,
				   hintPointer: Option[Changing[LocalizedString]] = None,
				   errorMessagePointer: Option[Changing[LocalizedString]] = None,
				   textPointer: PointerWithEvents[String] = new PointerWithEvents(""),
				   selectedTextColorPointer: Changing[Color] = Changing.wrap(Color.textBlack),
				   selectionBackgroundPointer: Changing[Option[Color]] = Changing.wrap(None),
				   highlightStylePointer: Changing[Option[ColorRole]] = Changing.wrap(None),
				   focusColorRole: ColorRole = Secondary, defaultBorderWidth: Double = 1, focusBorderWidth: Double = 3,
				   hintScaleFactor: Double = TextField.defaultHintScaleFactor, caretWidth: Double = 1.0,
				   caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
				   betweenLinesMargin: Double = 0.0, inputFilter: Option[Regex] = None,
				   resultFilter: Option[Regex] = None, maxLength: Option[Int] = None,
				   enabledPointer: Changing[Boolean] = Changing.wrap(true),
				   inputValidation: Option[A => LocalizedString] = None, fillBackground: Boolean = true,
				   allowLineBreaks: Boolean = false, allowTextShrink: Boolean = false,
				   showCharacterCount: Boolean = false)
				  (parseResult: Option[String] => A)
	extends ReachComponentWrapper with InputWithPointer[A, Changing[A]] with MutableFocusableWrapper
{
	// ATTRIBUTES	------------------------------------------
	
	override val valuePointer = resultFilter match
	{
		case Some(filter) => textPointer.map { text => parseResult(filter.filter(text).notEmpty) }
		case None => textPointer.map { text => parseResult(text.notEmpty) }
	}
	
	private lazy val defaultHintInsets = textInsets.expandingHorizontallyAccordingTo(alignment)
		.mapVertical { _ * hintScaleFactor }
	
	private val _focusPointer = new PointerWithEvents(false)
	
	// Uses either the outside error message, an input validator, both or neither as the error message pointer
	private val actualErrorPointer = inputValidation match
	{
		case Some(validation) =>
			val validationErrorPointer = valuePointer.map(validation)
			errorMessagePointer match
			{
				case Some(outsideError) => Some(outsideError.mergeWith(validationErrorPointer) { (default, validation) =>
					default.notEmpty.getOrElse(validation) })
				case None => Some(validationErrorPointer)
			}
		case None => errorMessagePointer
	}
	// Displays an error if there is one, otherwise displays the hint (provided there is one). None if neither is used.
	private lazy val actualHintTextPointer = hintPointer match
	{
		case Some(hint) =>
			actualErrorPointer match
			{
				case Some(error) => Some(hint.mergeWith(error) { (hint, error) => error.notEmpty getOrElse hint })
				case None => Some(hint)
			}
		case None => actualErrorPointer
	}
	private lazy val hintVisibilityPointer = actualHintTextPointer.map { _.map { _.nonEmpty } }
	
	// A pointer to whether this field currently highlights an error
	private val errorStatePointer = actualErrorPointer.map { _.map { _.nonEmpty } }
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
	private val contentColorPointer: Changing[Color] = highlightColorPointer
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
			Changing.wrap(Border.zero)
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
	
	private val (_wrapped, label) =
	{
		// Checks whether a separate hint area is required
		if (hintPointer.nonEmpty || actualErrorPointer.nonEmpty || (showCharacterCount && maxLength.isDefined))
		{
			ViewStack(parentHierarchy).builder(Mixed).withFixedStyle(margin = StackLength.fixedZero) { factories =>
				// Input part may contain a name label, if enabled
				val (inputPart, editLabel) = makeInputArea(factories.next())
				val hintPartAndPointer = makeHintArea(factories.next())
				// Input part is above and below it is hint part, which may sometimes be hidden
				(Vector(inputPart -> None) ++ hintPartAndPointer) -> editLabel
			}.parentAndResult
		}
		else
			makeInputArea(Mixed(parentHierarchy))
	}
	private val repaintListener = ChangeListener.onAnyChange { repaint() }
	
	
	// INITIAL CODE	------------------------------------------
	
	// Will not shrink below the default width
	wrapped.addConstraintOver(X)(MaxBetweenModifier(defaultWidth))
	
	_focusPointer.addListener(repaintListener)
	innerBackgroundPointer.addListener(repaintListener)
	borderPointer.addListener(repaintListener)
	
	
	// COMPUTED	----------------------------------------------
	
	def hasFocus = _focusPointer.value
	
	def focusPointer = _focusPointer.view
	
	
	// IMPLEMENTED	------------------------------------------
	
	override protected def wrapped: ReachComponent = _wrapped
	
	override protected def focusable = label
	
	
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
		
		// May draw a prompt while the field is empty (or starting with the prompt text)
		promptPointer.foreach { promptPointer =>
			val emptyText = measureText(LocalizedString.empty)
			val promptStylePointer = textStylePointer.map { _.mapColor { _.timesAlpha(0.66) } }
			val displayedPromptPointer = promptPointer.mergeWith(textPointer) { (prompt, text) =>
				if (text.isEmpty || prompt.string.startsWith(text)) measureText(prompt) else emptyText }
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
			val nameShouldBeSeparatePointer = _focusPointer.mergeWith(textPointer) { _ || _.nonEmpty }
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
			
			// May also display another prompt while the field has focus and is empty / starting with the prompt
			// (not blocked by name or text)
			promptPointer.foreach { promptPointer =>
				val promptContentPointer = promptPointer.mergeWith(textPointer) { (prompt, text) =>
					if (text.isEmpty || prompt.string.startsWith(text)) measureText(prompt) else emptyText
				}
				val displayedPromptPointer = promptContentPointer.mergeWith(_focusPointer) { (prompt, focus) =>
					if (focus) prompt else emptyText }
				textLabel.addCustomDrawer(SelectableTextViewDrawer(displayedPromptPointer, promptStylePointer))
			}
			
			// Displays one or both of the items
			Vector(nameLabel -> Some(nameVisibilityPointer), textLabel -> None) -> textLabel
		}
	}
	
	// Returns input area + editable label
	private def makeInputArea(factories: Mixed) =
	{
		// Input part may contain a name label, if enabled
		fieldNamePointer match
		{
			case Some(fieldNamePointer) => makeTextAndNameArea(factories(ViewStack), fieldNamePointer).parentAndResult
			case None =>
				val label = makeTextLabelOnly(factories(EditableTextLabel))
				label -> label
		}
	}
	
	// Returns the generated component (if any), along with its visibility pointer (if applicable)
	private def makeHintArea(factories: => Mixed) =
	{
		// In some cases, displays both message field and character count label
		// In other cases only the message field (which is hidden while empty)
		(if (showCharacterCount) maxLength else None) match
		{
			// Case: Character count should be displayed => Always displays at least the counter
			case Some(maxLength) =>
				actualHintTextPointer match
				{
					// Case: Hints are sometimes displayed
					case Some(hintTextPointer) =>
						// Places caps to stack equal to horizontal content margin
						val cap = textInsets.horizontal / 2 + (if (fillBackground) 0 else defaultBorderWidth)
						val stack = factories(ViewStack).builder(ViewTextLabel).withFixedStyle(X,
							margin = StackLength.any, cap = cap) { labelFactories =>
							val hintLabel = makeHintLabel(labelFactories.next(), hintTextPointer)
							val countLabel = makeCharacterCountLabel(labelFactories.next(), maxLength)
							
							// Hint label is only displayed while there is a hint to display,
							// Count label is always displayed
							ComponentCreationResult(Vector(hintLabel -> hintVisibilityPointer, countLabel -> None))
						}.parent
						Some(stack -> None)
					// Case: Only the character count element should be displayed
					case None => Some(makeCharacterCountLabel(factories(ViewTextLabel), maxLength) -> None)
				}
			case None =>
				// Case: No character count should be displayed => May display a hint label still (occasionally)
				actualHintTextPointer.map { hintTextPointer =>
					makeHintLabel(factories(ViewTextLabel), hintTextPointer) -> hintVisibilityPointer
				}
		}
	}
	
	private def makeMainTextLabel(factory: EditableTextLabelFactory, stylePointer: Changing[TextDrawContext]) =
	{
		val label = factory.apply(actorHandler, stylePointer, selectedTextColorPointer, selectionBackgroundPointer,
		contentColorPointer, caretWidth, caretBlinkFrequency, textPointer, inputFilter, maxLength, enabledPointer,
		allowSelectionWhileDisabled = false, allowLineBreaks, allowTextShrink)
		
		label.addFocusListener(FocusTracker)
		label
	}
	
	private def makeHintLabel(factory: ViewTextLabelFactory, textPointer: Changing[LocalizedString]) =
		factory.forText(textPointer, hintTextStylePointer, allowLineBreaks = false, allowTextShrink = true)
	
	private def makeCharacterCountLabel(factory: ViewTextLabelFactory, maxLength: Int) =
	{
		val countStylePointer = defaultHintColorPointer.map { color =>
			TextDrawContext(font * hintScaleFactor, color, Alignment.Right, textInsets * hintScaleFactor) }
		val textLengthPointer = textPointer.map { _.length }
		factory(textLengthPointer, countStylePointer,
			DisplayFunction.noLocalization[Int] { length => s"$length / $maxLength".noLanguage })
	}
	
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
	
	private def measureText(text: LocalizedString, isHint: Boolean = false) =
	{
		MeasuredText(text,
			FontMetricsContext(parentHierarchy.fontMetrics(font), betweenLinesMargin), alignment, allowLineBreaks)
	}
	
	
	// NESTED	-----------------------------------
	
	private object FocusTracker extends FocusChangeListener
	{
		override def onFocusChangeEvent(event: FocusChangeEvent) =
		{
			// Updates focus status
			_focusPointer.value = event.hasFocus
			// May format text contents as well
			if (event == FocusLost)
				resultFilter.foreach { filter => textPointer.update(filter.filter) }
		}
	}
}
