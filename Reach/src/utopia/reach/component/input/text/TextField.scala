package utopia.reach.component.input.text

import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.DetachmentChoice
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.parse.string.Regex
import utopia.flow.util.StringExtensions._
import utopia.flow.view.immutable.eventful.{AlwaysTrue, Fixed}
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.ChangingLike
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Axis.X
import utopia.paradigm.enumeration.ColorContrastStandard.Minimum
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.input.FieldState.{AfterEdit, BeforeEdit, Editing}
import utopia.reach.component.input.InputValidationResult.Default
import utopia.reach.component.input.{Field, FieldState, InputValidationResult}
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.component.template.ReachComponentWrapper
import utopia.reach.component.template.focus.{FocusableWithState, MutableFocusableWrapper}
import utopia.reach.component.wrapper.Open
import utopia.reach.focus.FocusEvent.{FocusGained, FocusLost}
import utopia.reflection.color.ColorRole
import utopia.reflection.color.ColorRole.Secondary
import utopia.reflection.component.context.TextContextLike
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.template.input.InputWithPointer
import utopia.reflection.image.SingleColorIcon
import utopia.reflection.localization.LocalString._
import utopia.reflection.localization.{DisplayFunction, LocalizedString, Localizer}
import utopia.reflection.shape.stack.StackLength
import utopia.reflection.shape.stack.modifier.MaxBetweenLengthModifier
import utopia.reflection.util.ComponentCreationDefaults

import scala.concurrent.duration.Duration

object TextField extends ContextInsertableComponentFactoryFactory[TextContextLike,
	TextFieldFactory, ContextualTextFieldFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new TextFieldFactory(hierarchy)
}

class TextFieldFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[TextContextLike, ContextualTextFieldFactory]
{
	override def withContext[N <: TextContextLike](context: N) =
		ContextualTextFieldFactory(parentHierarchy, context)
}

case class ContextualTextFieldFactory[+N <: TextContextLike](parentHierarchy: ComponentHierarchy, context: N)
	extends ContextualComponentFactory[N, TextContextLike, ContextualTextFieldFactory]
{
	// ATTRIBUTES	--------------------------------
	
	private lazy val fontMetrics = parentHierarchy.fontMetricsWith(context.font)
	
	private implicit val c: TextContextLike = context
	
	
	// IMPLICIT	------------------------------------
	
	private implicit def languageCode: String = "en"
	
	private implicit def localizer: Localizer = context.localizer
	
	
	// IMPLEMENTED	--------------------------------
	
	override def withContext[N2 <: TextContextLike](newContext: N2) = copy(context = newContext)
	
	
	// OTHER	------------------------------------
	
	/**
	  * Creates a new text field
	  * @param defaultWidth The default stack width used for the editable portion of this text field.
	  *                     The actual width may be larger, based on input.
	  * @param fieldNamePointer A pointer to the displayed name of this field (default = always empty)
	  * @param promptPointer A pointer to a prompt to display on this field (default = always empty)
	  * @param hintPointer A pointer to a hint to display under this field (default = always empty)
	  * @param errorMessagePointer A pointer to an error message to display under this field (default = always empty)
	  * @param textPointer A mutable text pointer to use in this field (default = new empty pointer)
	  * @param leftIconPointer A pointer to the icon to show on the left side of this field (default = always None)
	  * @param rightIconPointer A pointer to the icon to show on the right side of this field (default = always None)
	  * @param enabledPointer A pointer to the enabled state of this field (default = always ebabled)
	  * @param selectionStylePointer A pointer to the color style used for the selected area in this field
	  *                              (default = always secondary)
	  * @param highlightStylePointer A pointer to an additional highlight color style to apply to this field
	  *                              (default = always None)
	  * @param focusColorRole Highlight color role to use when this field is in focus (default = secondary)
	  * @param hintScaleFactor A multiplier factor used for the hint portions of this field (default = 70%)
	  * @param caretBlinkFrequency Frequency how often the caret should change visibility (default = global default)
	  * @param inputFilter A regex / filter applied to all typed characters and copied strings (optional)
	  * @param resultFilter A regex / filter applied to the resulting string (optional)
	  * @param maxLength Maximum number of characters to type (optional)
	  * @param inputValidation A validator function for specified input. Returns a validation result.
	  *                        Called only once the user leaves this input field.
	  *                        None if no validation should be applied (default)
	  * @param fillBackground Whether filled style should be used (default = global default)
	  * @param showCharacterCount Whether character count should be displayed (default = false)
	  * @param allowLineBreaks Whether line breaks (multi-line text) should be completely enabled in this field
	  *                        (default = determined by component creation context)
	  * @param parseResult A function for parsing the field value
	  * @tparam A Type of field value
	  * @return A new text field
	  */
	def apply[A](defaultWidth: StackLength,
				 fieldNamePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
				 promptPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
				 hintPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
				 errorMessagePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
				 textPointer: PointerWithEvents[String] = new PointerWithEvents(""),
				 leftIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
				 rightIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
				 enabledPointer: ChangingLike[Boolean] = AlwaysTrue,
				 selectionStylePointer: ChangingLike[ColorRole] = Fixed(Secondary),
				 highlightStylePointer: ChangingLike[Option[ColorRole]] = Fixed(None),
				 focusColorRole: ColorRole = Secondary, hintScaleFactor: Double = Field.defaultHintScaleFactor,
				 caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
				 inputFilter: Option[Regex] = None,
				 resultFilter: Option[Regex] = None, maxLength: Option[Int] = None,
				 inputValidation: Option[A => InputValidationResult] = None, fillBackground: Boolean = true,
				 showCharacterCount: Boolean = false, allowLineBreaks: Boolean = context.allowLineBreaks)
				(parseResult: Option[String] => A) =
		new TextField[A](parentHierarchy, defaultWidth, fieldNamePointer, promptPointer, hintPointer,
			errorMessagePointer, textPointer, leftIconPointer, rightIconPointer, enabledPointer, selectionStylePointer,
			highlightStylePointer, focusColorRole, hintScaleFactor, caretBlinkFrequency, inputFilter, resultFilter,
			maxLength, inputValidation, fillBackground, showCharacterCount, allowLineBreaks)(parseResult)
	
	/**
	  * Creates a new text field
	  * @param defaultWidth The default stack width used for the editable portion of this text field.
	  *                     The actual width may be larger, based on input.
	  * @param fieldNamePointer A pointer to the displayed name of this field (default = always empty)
	  * @param promptPointer A pointer to a prompt to display on this field (default = always empty)
	  * @param hintPointer A pointer to a hint to display under this field (default = always empty)
	  * @param errorMessagePointer A pointer to an error message to display under this field (default = always empty)
	  * @param textPointer A mutable text pointer to use in this field (default = new empty pointer)
	  * @param leftIconPointer A pointer to the icon to show on the left side of this field (default = always None)
	  * @param rightIconPointer A pointer to the icon to show on the right side of this field (default = always None)
	  * @param enabledPointer A pointer to the enabled state of this field (default = always ebabled)
	  * @param selectionStylePointer A pointer to the color style used for the selected area in this field
	  *                              (default = always secondary)
	  * @param highlightStylePointer A pointer to an additional highlight color style to apply to this field
	  *                              (default = always None)
	  * @param focusColorRole Highlight color role to use when this field is in focus (default = secondary)
	  * @param hintScaleFactor A multiplier factor used for the hint portions of this field (default = 70%)
	  * @param caretBlinkFrequency Frequency how often the caret should change visibility (default = global default)
	  * @param inputFilter A regex / filter applied to all typed characters and copied strings (optional)
	  * @param resultFilter A regex / filter applied to the resulting string (optional)
	  * @param maxLength Maximum number of characters to type (optional)
	  * @param inputValidation A validator function for specified input. Returns a validation result.
	  *                        Called only once the user leaves this input field.
	  *                        None if no validation should be applied (default)
	  * @param fillBackground Whether filled style should be used (default = global default)
	  * @param showCharacterCount Whether character count should be displayed (default = false)
	  * @param allowLineBreaks Whether line breaks (multi-line text) should be completely enabled in this field
	  *                        (default = determined by component creation context)
	  * @return A new text field
	  */
	def forString(defaultWidth: StackLength,
				  fieldNamePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
				  promptPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
				  hintPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
				  errorMessagePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
				  textPointer: PointerWithEvents[String] = new PointerWithEvents(""),
				  leftIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
				  rightIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
				  enabledPointer: ChangingLike[Boolean] = AlwaysTrue,
				  selectionStylePointer: ChangingLike[ColorRole] = Fixed(Secondary),
				  highlightStylePointer: ChangingLike[Option[ColorRole]] = Fixed(None),
				  focusColorRole: ColorRole = Secondary, hintScaleFactor: Double = Field.defaultHintScaleFactor,
				  caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
				  inputFilter: Option[Regex] = None,
				  resultFilter: Option[Regex] = None, maxLength: Option[Int] = None,
				  inputValidation: Option[String => InputValidationResult] = None, fillBackground: Boolean = true,
				  showCharacterCount: Boolean = false, allowLineBreaks: Boolean = context.allowLineBreaks) =
		apply[String](defaultWidth, fieldNamePointer, promptPointer, hintPointer, errorMessagePointer, textPointer,
			leftIconPointer, rightIconPointer, enabledPointer, selectionStylePointer, highlightStylePointer,
			focusColorRole, hintScaleFactor, caretBlinkFrequency, inputFilter, resultFilter, maxLength,
			inputValidation, fillBackground, showCharacterCount, allowLineBreaks) { _ getOrElse "" }
	
	/**
	  * Creates a new field that accepts integer numbers
	  * @param fieldNamePointer A pointer to the displayed name of this field (default = always empty)
	  * @param promptPointer A pointer to a prompt to display on this field (default = always empty)
	  * @param hintPointer A pointer to a hint to display under this field (default = always empty)
	  * @param errorMessagePointer A pointer to an error message to display under this field (default = always empty)
	  * @param leftIconPointer A pointer to the icon to show on the left side of this field (default = always None)
	  * @param rightIconPointer A pointer to the icon to show on the right side of this field (default = always None)
	  * @param enabledPointer A pointer to the enabled state of this field (default = always enabled)
	  * @param initialValue Value to place in this field initially (optional)
	  * @param minValue Smallest allowed value (default = smallest possible integer)
	  * @param maxValue Largest allowed value (default = largest possible integer)
	  * @param selectionStylePointer A pointer to the color style used for the selected area in this field
	  *                              (default = always secondary)
	  * @param highlightStylePointer A pointer to an additional highlight color style to apply to this field
	  *                              (default = always None)
	  * @param focusColorRole Highlight color role to use when this field is in focus (default = secondary)
	  * @param hintScaleFactor A multiplier factor used for the hint portions of this field (default = 70%)
	  * @param caretBlinkFrequency Frequency how often the caret should change visibility (default = global default)
	  * @param fillBackground Whether filled style should be used (default = global default)
	  * @param allowAutoHint Whether use of a min / max value hints should be allowed (default = true)
	  * @return A new text field
	  */
	def forInt(fieldNamePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
			   promptPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
			   hintPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
			   errorMessagePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
			   leftIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
			   rightIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
			   enabledPointer: ChangingLike[Boolean] = AlwaysTrue, initialValue: Option[Int] = None,
			   minValue: Int = Int.MinValue, maxValue: Int = Int.MaxValue,
			   selectionStylePointer: ChangingLike[ColorRole] = Fixed(Secondary),
			   highlightStylePointer: ChangingLike[Option[ColorRole]] = Fixed(None),
			   focusColorRole: ColorRole = Secondary, hintScaleFactor: Double = Field.defaultHintScaleFactor,
			   caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
			   fillBackground: Boolean = true, allowAutoHint: Boolean = true) =
	{
		// Only accepts integer numbers
		val inputFilter = if (minValue < 0) Regex.numericParts else Regex.digit
		val resultFilter = if (minValue < 0) Regex.numeric else Regex.numericPositive
		
		forNumbers[Int](initialValue, minValue, maxValue, 0, inputFilter, resultFilter, fieldNamePointer,
			promptPointer, hintPointer, errorMessagePointer, leftIconPointer, rightIconPointer, enabledPointer,
			selectionStylePointer, highlightStylePointer, focusColorRole, hintScaleFactor,
			caretBlinkFrequency, allowAutoHint && minValue > Int.MinValue && minValue != 0,
			allowAutoHint && maxValue < Int.MaxValue, fillBackground) { _.int }
	}
	
	/**
	  * Creates a new field that accepts decimal numbers
	  * @param minValue Smallest allowed value
	  * @param maxValue Largest allowed value
	  * @param fieldNamePointer A pointer to the displayed name of this field (default = always empty)
	  * @param promptPointer A pointer to a prompt to display on this field (default = always empty)
	  * @param hintPointer A pointer to a hint to display under this field (default = always empty)
	  * @param errorMessagePointer A pointer to an error message to display under this field (default = always empty)
	  * @param leftIconPointer A pointer to the icon to show on the left side of this field (default = always None)
	  * @param rightIconPointer A pointer to the icon to show on the right side of this field (default = always None)
	  * @param enabledPointer A pointer to the enabled state of this field (default = always enabled)
	  * @param initialValue Value to place in this field initially (optional)
	  * @param selectionStylePointer A pointer to the color style used for the selected area in this field
	  *                              (default = always secondary)
	  * @param highlightStylePointer A pointer to an additional highlight color style to apply to this field
	  *                              (default = always None)
	  * @param focusColorRole Highlight color role to use when this field is in focus (default = secondary)
	  * @param hintScaleFactor A multiplier factor used for the hint portions of this field (default = 70%)
	  * @param caretBlinkFrequency Frequency how often the caret should change visibility (default = global default)
	  * @param fillBackground Whether filled style should be used (default = global default)
	  * @param allowAutoHint Whether use of a min / max value hints should be allowed (default = true)
	  * @return A new text field
	  */
	def forDouble(minValue: Double, maxValue: Double,
				  fieldNamePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
				  promptPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
				  hintPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
				  errorMessagePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
				  leftIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
				  rightIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
				  enabledPointer: ChangingLike[Boolean] = AlwaysTrue, initialValue: Option[Double] = None,
				  proposedNumberOfDecimals: Int = 4, selectionStylePointer: ChangingLike[ColorRole] = Fixed(Secondary),
				  highlightStylePointer: ChangingLike[Option[ColorRole]] = Fixed(None),
				  focusColorRole: ColorRole = Secondary, hintScaleFactor: Double = Field.defaultHintScaleFactor,
				  caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
				  fillBackground: Boolean = true, allowAutoHint: Boolean = true) =
	{
		// Only accepts decimal numbers / number parts
		val inputFilter = if (minValue < 0) Regex.decimalParts else Regex.decimalPositiveParts
		val resultFilter = if (minValue < 0) Regex.decimal else Regex.decimalPositive
		
		forNumbers(initialValue, minValue, maxValue, (proposedNumberOfDecimals - 1) max 0, inputFilter, resultFilter,
			fieldNamePointer, promptPointer, hintPointer, errorMessagePointer, leftIconPointer, rightIconPointer,
			enabledPointer, selectionStylePointer, highlightStylePointer, focusColorRole, hintScaleFactor,
			caretBlinkFrequency, allowAutoHint && minValue != 0, allowAutoHint, fillBackground) { _.double }
	}
	
	private def forNumbers[A](initialValue: Option[A], minValue: A, maxValue: A, extraMaxLength: Int, inputRegex: Regex,
							  resultRegex: Regex, fieldNamePointer: ChangingLike[LocalizedString],
							  promptPointer: ChangingLike[LocalizedString], hintPointer: ChangingLike[LocalizedString],
							  errorMessagePointer: ChangingLike[LocalizedString],
							  leftIconPointer: ChangingLike[Option[SingleColorIcon]],
							  rightIconPointer: ChangingLike[Option[SingleColorIcon]],
							  enabledPointer: ChangingLike[Boolean], selectionStylePointer: ChangingLike[ColorRole],
							  highlightStylePointer: ChangingLike[Option[ColorRole]],
							  focusColorRole: ColorRole = Secondary, hintScaleFactor: Double,
							  caretBlinkFrequency: Duration, markMinimumValue: Boolean, markMaximumValue: Boolean,
							  fillBackground: Boolean)
							 (parse: Value => Option[A])(implicit ordering: Ordering[A]) =
	{
		// Field width is based on minimum and maximum values and their lengths
		val minString = minValue.toString
		val maxString = maxValue.toString
		val extraCharsString = String.valueOf(Vector.fill(extraMaxLength)('0'))
		val maxLength = (minString.length max maxString.length) + extraMaxLength
		val minStringWidth = widthOf(minString + extraCharsString)
		val maxStringWidth = widthOf(maxString + extraCharsString)
		val defaultWidth = StackLength(minStringWidth min maxStringWidth, minStringWidth max maxStringWidth)
		
		// May display min / max values as hints
		val effectiveHintPointer = {
			if (markMinimumValue || markMaximumValue) {
				val autoHint = {
					if (markMaximumValue) {
						if (markMinimumValue)
							s"$minValue - $maxValue".noLanguageLocalizationSkipped
						else
							s"Up to %s".autoLocalized.interpolated(Vector(maxValue))
					}
					else
						s"$minValue+".noLanguageLocalizationSkipped
				}
				hintPointer.notFixedWhere { _.isEmpty } match {
					case Some(hint) => hint.map { _.notEmpty.getOrElse(autoHint) }
					case None => Fixed(autoHint)
				}
			}
			else
				hintPointer
		}
		val initialText = initialValue.map { _.toString }.getOrElse("")
		
		// Displays an error if the value is outside of accepted range
		val textPointer = new PointerWithEvents(initialText)
		def validateInput(input: Option[A]): InputValidationResult = input match
		{
			// Case: Input could be parsed => Checks for min / max values
			case Some(input) =>
				if (ordering.compare(input, minValue) < 0)
					InputValidationResult.Failure("Minimum value is %i".autoLocalized.interpolated(Vector(minValue)))
				else if (ordering.compare(input, maxValue) > 0)
					InputValidationResult.Failure("Maximum value is %i".autoLocalized.interpolated(Vector(maxValue)))
				else
					Default
			// Case: Input couldn't be parsed => May inform the user
			case None =>
				if (textPointer.value.isEmpty)
					Default
				else
					InputValidationResult.Failure("Not a valid number".autoLocalized)
		}
		
		apply[Option[A]](defaultWidth, fieldNamePointer, promptPointer, effectiveHintPointer, errorMessagePointer,
			textPointer, leftIconPointer, rightIconPointer, enabledPointer, selectionStylePointer,
			highlightStylePointer, focusColorRole, hintScaleFactor, caretBlinkFrequency,
			Some(inputRegex), Some(resultRegex), Some(maxLength), Some(validateInput),
			fillBackground, allowLineBreaks = false) { parse(_) }
	}
	
	private def widthOf(text: String) = fontMetrics.widthOf(text)
}

/**
  * Used for requesting text input from the user
  * @author Mikko Hilpinen
  * @since 14.11.2020, v0.1
  */
class TextField[A](parentHierarchy: ComponentHierarchy, defaultWidth: StackLength,
                   fieldNamePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
                   promptPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
                   hintPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
                   errorMessagePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
                   textContentPointer: PointerWithEvents[String] = new PointerWithEvents(""),
                   leftIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
                   rightIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
                   enabledPointer: ChangingLike[Boolean] = AlwaysTrue,
                   selectionStylePointer: ChangingLike[ColorRole] = Fixed(Secondary),
                   highlightStylePointer: ChangingLike[Option[ColorRole]] = Fixed(None),
                   focusColorRole: ColorRole = Secondary, hintScaleFactor: Double = Field.defaultHintScaleFactor,
                   caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
                   inputFilter: Option[Regex] = None,
                   resultFilter: Option[Regex] = None, val maxLength: Option[Int] = None,
                   inputValidation: Option[A => InputValidationResult] = None, fillBackground: Boolean = true,
                   showCharacterCount: Boolean = false, lineBreaksEnabled: Boolean = false)
				  (parseResult: Option[String] => A)(implicit context: TextContextLike)
	extends ReachComponentWrapper with InputWithPointer[A, ChangingLike[A]] with MutableFocusableWrapper
		with FocusableWithState
{
	// ATTRIBUTES	------------------------------------------
	
	private val _statePointer = new PointerWithEvents[FieldState](BeforeEdit)
	private val goToEditInputListener = ChangeListener.onAnyChange {
		if (hasFocus)
			_statePointer.value = Editing
		DetachmentChoice.detach
	}
	
	override val valuePointer = resultFilter match {
		case Some(filter) => textContentPointer.map { text => parseResult(filter.filter(text).notEmpty) }
		case None => textContentPointer.map { text => parseResult(text.notEmpty) }
	}
	
	// Input validation affects hint and highlight logic, if specified
	private val (actualHintPointer, actualHighlightingPointer) = inputValidation match {
		// Case: Input validation is used => Merges validation results input other pointers
		case Some(validation) =>
			// The input is validated only after (and not during) editing
			val validationResultPointer = valuePointer.mergeWith(_statePointer) { (value, state) =>
				if (state == AfterEdit)
					validation(value)
				else
					InputValidationResult.Default
			}
			val hintTextPointer = validationResultPointer.mergeWith(hintPointer) { (validation, hint) =>
				validation.message.nonEmptyOrElse(hint)
			}
			val colorPointer = validationResultPointer.mergeWith(highlightStylePointer) { (validation, default) =>
				validation.highlighting.orElse(default)
			}
			hintTextPointer -> colorPointer
		// Case: Input validation is not used => Uses the pointers specified earlier
		case None => hintPointer -> highlightStylePointer
	}
	
	private val actualPromptPointer = promptPointer.notFixedWhere { _.isEmpty } match {
		case Some(promptPointer) =>
			// Displays the prompt while text starts with the same characters or is empty
			promptPointer.mergeWith(textContentPointer) { (prompt, text) =>
				if (text.isEmpty || prompt.string.startsWith(text)) prompt else LocalizedString.empty
			}
		case None => promptPointer
	}
	
	private val isEmptyPointer = textContentPointer.map { _.isEmpty }
	
	private val _wrapped = Field(parentHierarchy).withContext(context).apply[EditableTextLabel](isEmptyPointer,
		fieldNamePointer, actualPromptPointer, actualHintPointer, errorMessagePointer, leftIconPointer, rightIconPointer,
		context.textInsets.total / 2, actualHighlightingPointer, focusColorRole, hintScaleFactor,
		fillBackground) { (fc, tc) =>
		
		val stylePointer = fc.textStylePointer.map { _.expandingHorizontally.withAllowLineBreaks(lineBreaksEnabled) }
		val selectedBackgroundPointer = fc.backgroundPointer.mergeWith(selectionStylePointer) { (bg, c) =>
			tc.colorScheme(c).forBackgroundPreferringLight(bg) }
		val selectedTextColorPointer = selectedBackgroundPointer.map { _.defaultTextColor }
		val caretColorPointer = fc.backgroundPointer.mergeWith(selectedBackgroundPointer, selectionStylePointer) { (mainBg, selectedBg, selectionStyle) =>
			val palet = tc.colorScheme(selectionStyle)
			val minimumContrast = Minimum.defaultMinimumContrast
			// Attempts to find a color that works against both backgrounds (standard & selected)
			val defaultOption = palet.bestAgainst(Vector(mainBg, selectedBg), minimumContrast)
			// However, if the default doesn't have enough contrast against the main background, finds an alternative
			if (defaultOption.contrastAgainst(mainBg) < minimumContrast)
				palet.forBackground(mainBg, minimumContrast): Color
			else
				defaultOption: Color
		}
		val caretWidth = (context.margins.verySmall / 2) max 1
		
		val label = EditableTextLabel(fc.parentHierarchy)
			.apply(tc.actorHandler, stylePointer, selectedTextColorPointer, selectedBackgroundPointer.map { Some(_) },
				caretColorPointer, caretWidth, caretBlinkFrequency, textContentPointer, inputFilter, maxLength,
				enabledPointer, allowSelectionWhileDisabled = false, tc.allowTextShrink)
		label.addFocusListener(fc.focusListener)
		fc.promptDrawers.foreach(label.addCustomDrawer)
		
		label
	} { (fc, tc) =>
		if (showCharacterCount)
			maxLength.map { maxLength =>
				implicit val localizer: Localizer = tc.localizer
				val countStylePointer = fc.backgroundPointer.map { background =>
					TextDrawContext(fc.font, background.textColorStandard.hintTextColor, Alignment.Right,
						tc.textInsets * hintScaleFactor)
				}
				val textLengthPointer = textContentPointer.map { _.length }
				Open.using(ViewTextLabel) { _.apply(
					textLengthPointer, countStylePointer,
					DisplayFunction.functionToDisplayFunction[Int] { length =>
						s"%i / %i".noLanguage.localized.interpolated(Vector(length, maxLength)) },
					allowTextShrink = true)
				}(parentHierarchy.top)
			}
		else
			None
	}
	
	
	// INITIAL CODE	------------------------------------------
	
	// Will not shrink below the default width
	_wrapped.wrappedField.addConstraintOver(X)(MaxBetweenLengthModifier(defaultWidth))
	
	addFocusListener {
		// Remembers the first time this field received focus
		case FocusGained => textContentPointer.addListener(goToEditInputListener)
		// Formats text contents whenever focus is lost
		case FocusLost =>
			textContentPointer.removeListener(goToEditInputListener)
			_statePointer.value = AfterEdit
			resultFilter.foreach { filter => textContentPointer.update(filter.filter) }
		case _ => ()
	}
	
	
	// COMPUTED	----------------------------------------------
	
	/**
	  * @return A read-only version of this text field's text pointer
	  */
	def textPointer = textContentPointer.view
	/**
	  * @return A pointer that contains the current state of this field
	  */
	def statePointer = _statePointer.view
	
	/**
	  * @return The current state of this field
	  */
	def state = statePointer.value
	
	
	// IMPLEMENTED	------------------------------------------
	
	override def hasFocus = _wrapped.hasFocus
	
	override protected def wrapped = _wrapped
	
	override protected def focusable = _wrapped.wrappedField
	
	
	// OTHER	----------------------------------------------
	
	/**
	  * Clears this field of all text
	  */
	def clear() = textContentPointer.value = ""
}
