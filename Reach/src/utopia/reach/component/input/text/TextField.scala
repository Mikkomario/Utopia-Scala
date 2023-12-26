package utopia.reach.component.input.text

import utopia.firmament.component.input.InputWithPointer
import utopia.firmament.context.TextContext
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.LocalString._
import utopia.firmament.localization.{DisplayFunction, LocalizedString, Localizer}
import utopia.firmament.model.stack.StackLength
import utopia.firmament.model.stack.modifier.MaxBetweenLengthModifier
import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.range.{HasInclusiveOrderedEnds, NumericSpan}
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeResponse.Detach
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.Identity
import utopia.flow.parse.string.Regex
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.ColorRole
import utopia.paradigm.enumeration.Axis.X
import utopia.reach.component.factory.FromVariableContextComponentFactoryFactory
import utopia.reach.component.factory.contextual.VariableContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.input.FieldState.{AfterEdit, BeforeEdit, Editing}
import utopia.reach.component.input.InputValidationResult.Default
import utopia.reach.component.input._
import utopia.reach.component.label.image.ViewImageLabelSettings
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.component.label.text.selectable.SelectableTextLabelSettings
import utopia.reach.component.template.ReachComponentWrapper
import utopia.reach.component.template.focus.{FocusableWithState, FocusableWrapper}
import utopia.reach.component.wrapper.Open
import utopia.reach.focus.FocusEvent.{FocusGained, FocusLost}
import utopia.reach.focus.FocusListener

import scala.math.Ordered.orderingToOrdered

/**
  * Common trait for text field factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
trait TextFieldSettingsLike[+Repr] extends FieldSettingsLike[Repr] with EditableTextLabelSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Wrapped settings that specify more general field functionality
	  */
	def fieldSettings: FieldSettings
	/**
	  * Wrapped settings that define how the text-editing should function
	  */
	def editingSettings: EditableTextLabelSettings
	/**
	  * A filter applied to the text contents of this field whenever results are acquired or focus lost
	  */
	def resultFilter: Option[Regex]
	/**
	  * Whether the number of typed characters should be displayed at the bottom right part of the field
	  */
	def showsCharacterCount: Boolean
	
	/**
	  * Wrapped settings that define how the text-editing should function
	  * @param settings New editing settings to use.
	  *                 Wrapped settings that define how the text-editing should function
	  * @return Copy of this factory with the specified editing settings
	  */
	def withEditingSettings(settings: EditableTextLabelSettings): Repr
	/**
	  * Wrapped settings that specify more general field functionality
	  * @param settings New field settings to use.
	  *                 Wrapped settings that specify more general field functionality
	  * @return Copy of this factory with the specified field settings
	  */
	def withFieldSettings(settings: FieldSettings): Repr
	/**
	  * A filter applied to the text contents of this field whenever results are acquired or focus lost
	  * @param filter New result filter to use.
	  *               A filter applied to the text contents of this field whenever results are acquired or focus lost
	  * @return Copy of this factory with the specified result filter
	  */
	def withResultFilter(filter: Option[Regex]): Repr
	/**
	  * Whether the number of typed characters should be displayed at the bottom right part of the field
	  * @param show New shows character count to use.
	  *             Whether the number of typed characters should be displayed at the bottom right part of the field
	  * @return Copy of this factory with the specified shows character count
	  */
	def withShowsCharacterCount(show: Boolean): Repr
	
	
	// COMPUTED ------------------------
	
	/**
	  * @return Copy of this factory that displays input and maximum character count
	  */
	def displayingCharacterCount = withShowsCharacterCount(show = true)
	
	
	// IMPLEMENTED	--------------------
	
	override def labelSettings: SelectableTextLabelSettings = editingSettings.labelSettings
	override def errorMessagePointer = fieldSettings.errorMessagePointer
	override def fieldNamePointer = fieldSettings.fieldNamePointer
	override def fillBackground = fieldSettings.fillBackground
	override def focusColorRole = fieldSettings.focusColorRole
	override def highlightPointer = fieldSettings.highlightPointer
	override def hintPointer = fieldSettings.hintPointer
	override def hintScaleFactor = fieldSettings.hintScaleFactor
	override def iconPointers = fieldSettings.iconPointers
	override def imageSettings = fieldSettings.imageSettings
	override def promptPointer = fieldSettings.promptPointer
	override def allowsSelectionWhileDisabled = editingSettings.allowsSelectionWhileDisabled
	override def enabledPointer: Changing[Boolean] = editingSettings.enabledPointer
	override def focusListeners = labelSettings.focusListeners
	override def inputFilter = editingSettings.inputFilter
	override def maxLength = editingSettings.maxLength
	
	override def withErrorMessagePointer(p: Changing[LocalizedString]) =
		withFieldSettings(fieldSettings.withErrorMessagePointer(p))
	override def withFieldNamePointer(p: Changing[LocalizedString]) =
		withFieldSettings(fieldSettings.withFieldNamePointer(p))
	override def withFillBackground(fill: Boolean) = withFieldSettings(fieldSettings.withFillBackground(fill))
	override def withFocusColorRole(color: ColorRole) = withFieldSettings(fieldSettings.withFocusColorRole(color))
	override def withHighlightPointer(p: Changing[Option[ColorRole]]) =
		withFieldSettings(fieldSettings.withHighlightPointer(p))
	override def withHintPointer(p: Changing[LocalizedString]) =
		withFieldSettings(fieldSettings.withHintPointer(p))
	override def withHintScaleFactor(scaling: Double) =
		withFieldSettings(fieldSettings.withHintScaleFactor(scaling))
	override def withIconPointers(pointers: Pair[Changing[SingleColorIcon]]) =
		withFieldSettings(fieldSettings.withIconPointers(pointers))
	override def withImageSettings(settings: ViewImageLabelSettings) =
		withFieldSettings(fieldSettings.withImageSettings(settings))
	override def withPromptPointer(p: Changing[LocalizedString]) =
		withFieldSettings(fieldSettings.withPromptPointer(p))
	override def withAllowsSelectionWhileDisabled(allow: Boolean) =
		withEditingSettings(editingSettings.withAllowsSelectionWhileDisabled(allow))
	override def withEnabledPointer(p: Changing[Boolean]): Repr =
		withEditingSettings(editingSettings.withEnabledPointer(p))
	override def withInputFilter(filter: Option[Regex]) =
		withEditingSettings(editingSettings.withInputFilter(filter))
	override def withLabelSettings(settings: SelectableTextLabelSettings) =
		withEditingSettings(editingSettings.withLabelSettings(settings))
	override def withMaxLength(max: Option[Int]) = withEditingSettings(editingSettings.withMaxLength(max))
	override def withFocusListeners(listeners: Vector[FocusListener]): Repr =
		mapLabelSettings { _.withFocusListeners(listeners) }
	
	
	// OTHER	--------------------
	
	def mapFieldSettings(f: FieldSettings => FieldSettings) = withFieldSettings(f(fieldSettings))
	def mapEditingSettings(f: EditableTextLabelSettings => EditableTextLabelSettings) =
		withEditingSettings(f(editingSettings))
	
	def mapResultFilter(f: Option[Regex] => Option[Regex]) = withResultFilter(f(resultFilter))
	
	/**
	  * @param filter A filter to modify text input with, before output value is parsed
	  * @return Copy of this factory with the specified filter
	  */
	def withResultFilter(filter: Regex): Repr = withResultFilter(Some(filter))
}

object TextFieldSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply()
}
/**
  * Combined settings used when constructing text fields
  * @param fieldSettings       Wrapped settings that specify more general field functionality
  * @param editingSettings     Wrapped settings that defined how text editing and the underlying label functions
  * @param resultFilter        A filter applied to the text contents of this field whenever results are acquired or
  *                            focus lost
  * @param showsCharacterCount Whether the number of typed characters should be displayed at the bottom right
  *                            part of the field
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
case class TextFieldSettings(fieldSettings: FieldSettings = FieldSettings.default,
                             editingSettings: EditableTextLabelSettings = EditableTextLabelSettings.default,
                             resultFilter: Option[Regex] = None, showsCharacterCount: Boolean = false)
	extends TextFieldSettingsLike[TextFieldSettings]
{
	// IMPLEMENTED	--------------------
	
	override def onlyIntegers = super.onlyIntegers.withResultFilter(Regex.integer)
	override def onlyPositiveNumbers =
		super.onlyPositiveNumbers.withResultFilter(Regex.positiveNumber)
	override def onlyNumbers = super.onlyNumbers.withResultFilter(Regex.number)
	
	override def withEditingSettings(settings: EditableTextLabelSettings) = copy(editingSettings = settings)
	override def withFieldSettings(settings: FieldSettings) = copy(fieldSettings = settings)
	override def withResultFilter(filter: Option[Regex]) = copy(resultFilter = filter)
	override def withShowsCharacterCount(show: Boolean) = copy(showsCharacterCount = show)
}

/**
  * Common trait for factories that wrap a text field settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
trait TextFieldSettingsWrapper[+Repr] extends TextFieldSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: TextFieldSettings
	
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: TextFieldSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	// Uses the methods directly defined in the settings because they also alter the result filter
	override def onlyIntegers = mapSettings { _.onlyIntegers }
	override def onlyPositiveNumbers = mapSettings { _.onlyPositiveNumbers }
	override def onlyNumbers = mapSettings { _.onlyNumbers }
	
	override def editingSettings = settings.editingSettings
	override def fieldSettings = settings.fieldSettings
	override def resultFilter = settings.resultFilter
	override def showsCharacterCount = settings.showsCharacterCount
	
	override def withEditingSettings(settings: EditableTextLabelSettings) =
		mapSettings { _.withEditingSettings(settings) }
	override def withFieldSettings(settings: FieldSettings) = mapSettings { _.withFieldSettings(settings) }
	override def withResultFilter(filter: Option[Regex]) = mapSettings { _.withResultFilter(filter) }
	override def withShowsCharacterCount(show: Boolean) = mapSettings { _.withShowsCharacterCount(show) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: TextFieldSettings => TextFieldSettings) = withSettings(f(settings))
}

/**
  * Factory class used for constructing text fields using contextual component creation information
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
case class ContextualTextFieldFactory(parentHierarchy: ComponentHierarchy,
                                      contextPointer: Changing[TextContext],
                                      settings: TextFieldSettings = TextFieldSettings.default)
	extends TextFieldSettingsWrapper[ContextualTextFieldFactory]
		with VariableContextualFactory[TextContext, ContextualTextFieldFactory]
{
	// IMPLICIT	------------------------------------
	
	private implicit def languageCode: String = "en"
	
	
	// IMPLEMENTED	--------------------
	
	override def withContextPointer(contextPointer: Changing[TextContext]) =
		copy(contextPointer = contextPointer)
	
	override def withSettings(settings: TextFieldSettings) = copy(settings = settings)
	
	
	// OTHER	------------------------------------
	
	/**
	  * Creates a new text field
	  * @param defaultWidth The default stack width used for the editable portion of this text field.
	  *                     The actual width may be larger, based on input.
	  * @param textPointer A mutable text pointer used and modified by this field (default = new empty pointer)
	  * @param inputValidation A validation function for specified input. Returns a validation result.
	  *                        Called only once the user leaves this input field.
	  *                        None if no validation should be applied (default).
	  *                        Input validation only affects styling and state, but not output value.
	  * @param parseResult A function for parsing the field value from input string.
	  *                    Result filter, if provided, will be applied before passing the input to this function.
	  * @tparam A Type of field output value
	  * @return A new text field
	  */
	def apply[A](defaultWidth: StackLength,
	             textPointer: EventfulPointer[String] = new EventfulPointer[String](""),
	             inputValidation: Option[A => InputValidationResult] = None)
	            (parseResult: String => A) =
		new TextField[A](parentHierarchy, contextPointer, defaultWidth, settings, textPointer,
			inputValidation)(parseResult)
	
	/**
	  * Creates a new text field
	  * @param defaultWidth    The default stack width used for the editable portion of this text field.
	  *                        The actual width may be larger, based on input.
	  * @param textPointer     A mutable text pointer used and modified by this field (default = new empty pointer)
	  *
	  * @param parse     A function for parsing the field value from input string.
	  *                        Result filter, if provided, will be applied before passing the input to this function.
	  * @param validate A validation function for parsed output values. Returns a validation result.
	  *                        Called only once the user leaves this input field.
	  *                        None if no validation should be applied (default).
	  *                        Input validation only affects styling and state, but not output value.
	  * @tparam A Type of field output value
	  * @return A new text field
	  */
	def validating[A](defaultWidth: StackLength,
	                  textPointer: EventfulPointer[String] = new EventfulPointer[String](""))
	                 (parse: String => A)(validate: A => InputValidationResult) =
		apply[A](defaultWidth, textPointer, Some(validate))(parse)
	
	/**
	  * Creates a new text field that produces string output values
	  * @param defaultWidth The default stack width used for the editable portion of this text field.
	  *                     The actual width may be larger, based on input.
	  * @param textPointer A mutable text pointer to use in this field (default = new empty pointer)
	  * @param validate A function used for testing user input. Returns a validation result.
	  *                   Called only when focus is lost and/or results are retrieved.
	  *                   Please note that this function will not alter the value in any way,
	  *                   but will only be used to alter the visible field state.
	  *                   None if no validation should be applied (default).
	  * @return A new text field
	  */
	def string(defaultWidth: StackLength, textPointer: EventfulPointer[String] = new EventfulPointer[String](""),
	           validate: Option[String => InputValidationResult] = None) =
		apply[String](defaultWidth, textPointer, validate)(Identity)
	/**
	  * Creates a new text field that produces string output values
	  * @param defaultWidth The default stack width used for the editable portion of this text field.
	  *                     The actual width may be larger, based on input.
	  * @param textPointer  A mutable text pointer to use in this field (default = new empty pointer)
	  * @param validate   A function used for testing user input. Returns a validation result.
	  *                     Called only when focus is lost and/or results are retrieved.
	  *                     Please note that this function will not alter the value in any way,
	  *                     but will only be used to alter the visible field state.
	  * @return A new text field
	  */
	def validatedString(defaultWidth: StackLength,
	                    textPointer: EventfulPointer[String] = new EventfulPointer[String](""))
	                   (validate: String => InputValidationResult) =
		string(defaultWidth, textPointer, Some(validate))
	
	/**
	  * Creates a new text field
	  * @param defaultWidth The default stack width used for the editable portion of this text field.
	  *                     The actual width may be larger, based on input.
	  * @param textPointer A mutable text pointer to use in this field (default = new empty pointer)
	  * @param inputValidation A validator function for specified input. Returns a validation result.
	  *                        Called only once the user leaves this input field.
	  *                        None if no validation should be applied (default)
	  * @return A new text field
	  */
	@deprecated("Renamed to .string(...)", "v1.1")
	def forString(defaultWidth: StackLength, textPointer: EventfulPointer[String] = new EventfulPointer[String](""),
	              inputValidation: Option[String => InputValidationResult] = None) =
		string(defaultWidth, textPointer, inputValidation)
	
	/**
	  * Creates a new field that accepts integer numbers
	  * @param allowedRange The smallest and largest allowed input value.
	  *                     Default = whole range of valid integers.
	  * @param initialValue          Value to place in this field initially (optional)
	  * @param validate An optional input validation function that is applied in addition to the allowed range
	  *                 and numeric conversion checks.
	  *                 Accepts the input value or None if this field is empty. Returns an input validation result.
	  *                 Default = None = No additional validation should be performed.
	  * @param disableLengthHint Whether the minimum and/or maximum length should NOT be displayed as a hint.
	  *                          Please note that the default range (i.e. Integer min and/or max value) is never
	  *                          displayed as a hint.
	  * @return A new text field
	  */
	def int(allowedRange: HasInclusiveOrderedEnds[Int] = NumericSpan(Int.MinValue, Int.MaxValue),
	        initialValue: Option[Int] = None, validate: Option[Option[Int] => InputValidationResult] = None,
	        disableLengthHint: Boolean = false) =
	{
		val allowsNegative = allowedRange.start < 0
		val inputFilter = if (allowsNegative) Regex.integerPart else Regex.digit
		val resultFilter = if (allowsNegative) Regex.integer else Regex.positiveInteger
		val markMinMax = {
			if (disableLengthHint)
				Pair.twice(false)
			else
				Pair(allowedRange.start > Int.MinValue, allowedRange.end < Int.MaxValue)
		}
		number[Int](allowedRange, initialValue, 0, inputFilter, resultFilter, validate,
			markMinMax) { _.int }
	}
	/**
	  * Creates a new field that accepts integer numbers
	  * @param allowedRange      The smallest and largest allowed input value.
	  *                          Default = whole range of valid integers.
	  * @param initialValue      Value to place in this field initially (optional)
	  * @param disableLengthHint Whether the minimum and/or maximum length should NOT be displayed as a hint.
	  *                          Please note that the default range (i.e. Integer min and/or max value) is never
	  *                          displayed as a hint.
	  * @param validate An optional input validation function that is applied in addition to the allowed range
	  *                 and numeric conversion checks.
	  *                 Accepts the input value or None if this field is empty. Returns an input validation result.
	  * @return A new text field
	  */
	def validatedInt(allowedRange: HasInclusiveOrderedEnds[Int] = NumericSpan(Int.MinValue, Int.MaxValue),
	                 initialValue: Option[Int] = None, disableLengthHint: Boolean = false)
	                (validate: Option[Int] => InputValidationResult) =
		int(allowedRange, initialValue, Some(validate), disableLengthHint)
	/**
	  * Creates a new field that accepts integer numbers
	  * @param initialValue Value to place in this field initially (optional)
	  * @param minValue Smallest allowed value (default = smallest possible integer)
	  * @param maxValue Largest allowed value (default = largest possible integer)
	  * @param allowAutoHint Whether use of a min / max value hints should be allowed (default = true)
	  * @return A new text field
	  */
	@deprecated("Please use .int(...) instead", "v1.1")
	def forInt(initialValue: Option[Int] = None, minValue: Int = Int.MinValue, maxValue: Int = Int.MaxValue,
	           allowAutoHint: Boolean = true) =
		int(NumericSpan(minValue, maxValue), initialValue, None, !allowAutoHint)
	
	/**
	  * Creates a new field that accepts integer numbers
	  * @param allowedRange      The smallest and largest allowed input value.
	  * @param initialValue      Value to place in this field initially (optional)
	  * @param expectedNumberOfDecimals The expected (maximum) number of decimal places in the resulting input value.
	  *                                 This value is used for determining input maximum length in characters,
	  *                                 and also the field default width.
	  *                                 This value assumes that the minimum and maximum values are provided as whole
	  *                                 integers.
	  *
	  *                                 E.g. If minimum value is 0.0 (3 characters),
	  *                                 maximum value is 1000.0 (6 characters) and 'expectedNumberOfDecimals' is 2,
	  *                                 the maximum input length is 7 characters.
	  *                                 This of course could be provided as 1.23456, so it doesn't necessarily mean
	  *                                 that the number of decimal places will be restricted to the specified value.
	  *
	  *                                 Default = 4.
	  *
	  * @param validate          An optional input validation function that is applied in addition to the allowed range
	  *                          and numeric conversion checks.
	  *                          Accepts the input value or None if this field is empty. Returns an input validation result.
	  *                          Default = None = No additional validation should be performed.
	  * @param disableLengthHint Whether the minimum and/or maximum length should NOT be displayed as a hint.
	  *                          Please note that a minimum value of 0 is never displayed as a hint.
	  * @return A new text field
	  */
	def double(allowedRange: HasInclusiveOrderedEnds[Double], initialValue: Option[Double] = None,
	           expectedNumberOfDecimals: Int = 4, validate: Option[Option[Double] => InputValidationResult] = None,
	           disableLengthHint: Boolean = false) =
	{
		// Only accepts decimal numbers / number parts
		val allowsNegative = allowedRange.start < 0
		val inputFilter = if (allowsNegative) Regex.numberPart else Regex.positiveNumberPart
		val resultFilter = if (allowsNegative) Regex.number else Regex.positiveNumber
		
		number[Double](allowedRange, initialValue, (expectedNumberOfDecimals - 1) max 0, inputFilter, resultFilter,
			validate,
			if (disableLengthHint) Pair.twice(false) else Pair(allowedRange.start != 0, true)) { _.double }
	}
	/**
	  * Creates a new field that accepts integer numbers
	  * @param allowedRange             The smallest and largest allowed input value.
	  * @param initialValue             Value to place in this field initially (optional)
	  * @param expectedNumberOfDecimals The expected (maximum) number of decimal places in the resulting input value.
	  *                                 This value is used for determining input maximum length in characters,
	  *                                 and also the field default width.
	  *                                 This value assumes that the minimum and maximum values are provided as whole
	  *                                 integers.
	  *
	  *                                 E.g. If minimum value is 0.0 (3 characters),
	  *                                 maximum value is 1000.0 (6 characters) and 'expectedNumberOfDecimals' is 2,
	  *                                 the maximum input length is 7 characters.
	  *                                 This of course could be provided as 1.23456, so it doesn't necessarily mean
	  *                                 that the number of decimal places will be restricted to the specified value.
	  *
	  *                                 Default = 4.
	  *
	  * @param disableLengthHint        Whether the minimum and/or maximum length should NOT be displayed as a hint.
	  *                                 Please note that a minimum value of 0 is never displayed as a hint.
	  * @param validate An input validation function that is applied in addition to the allowed range
	  *                 and numeric conversion checks.
	  *                 Accepts the input value or None if this field is empty. Returns an input validation result.
	  * @return A new text field
	  */
	def validatedDouble(allowedRange: HasInclusiveOrderedEnds[Double], initialValue: Option[Double] = None,
	                    expectedNumberOfDecimals: Int = 4, disableLengthHint: Boolean = false)
	                   (validate: Option[Double] => InputValidationResult) =
		double(allowedRange, initialValue, expectedNumberOfDecimals, Some(validate), disableLengthHint)
	/**
	  * Creates a new field that accepts decimal numbers
	  * @param minValue Smallest allowed value
	  * @param maxValue Largest allowed value
	  * @param initialValue Value to place in this field initially (optional)
	  * @param allowAutoHint Whether use of a min / max value hints should be allowed (default = true)
	  * @return A new text field
	  */
	@deprecated("Please use .double instead", "v1.1")
	def forDouble(minValue: Double, maxValue: Double,
	              initialValue: Option[Double] = None,
	              proposedNumberOfDecimals: Int = 4, allowAutoHint: Boolean = true) =
		double(NumericSpan(minValue, maxValue), initialValue, proposedNumberOfDecimals, None, !allowAutoHint)
	
	/**
	  * Creates a text field for numeric input
	  * @param allowedRange Smallest and largest allowed input value
	  * @param initialValue Initial input value (optional)
	  * @param extraInputLength Additional allowed input length relative to the longer of the minimum and maximum value.
	  *                         I.e. If minimum value is 0 (1 character) and maximum value is 999 (3 characters),
	  *                         'extraInputLength' of 2 would result in maximum input length of 5.
	  *                         Default = 0.
	  * @param inputFilter A filter applied to every input character. Typically used to restrict the input to correct
	  *                    numeric characters and symbols.
	  *                    Default = an explicit filter specified in the settings, or a filter that only accepts
	  *                    characters used in decimal numbers (i.e. digits, '-', '.' and ',')
	  * @param resultFilter A filter used for formatting the input string before converting it into an output
	  *                     (numeric) value.
	  *                     Default = explicit filter specified in the settings, or a filter that formats strings
	  *                     into decimal numbers.
	  * @param validate   An optional input validation function that is applied in addition to the allowed range
	  *                   and numeric conversion checks.
	  *                   Accepts the input value or None if this field is empty. Returns an input validation result.
	  *                   Default = None = No additional validation should be performed.
	  * @param markMinMax Whether minimum and/or maximum values should be displayed as a hint at the bottom of
	  *                   this field.
	  *                   The input is provided as a pair, where the first value determines whether minimum value is
	  *                   displayed, and the second value determines whether the maximum value is displayed.
	  *                   By default, neither value is displayed.
	  *                   This hint is not displayed while there is another hint being displayed.
	  * @param parse A function that accepts a value and yields the (numeric) value of correct type.
	  * @tparam A Type of numeric values provided by this field
	  * @return A new input field that yields numeric values
	  */
	def number[A](allowedRange: HasInclusiveOrderedEnds[A], initialValue: Option[A] = None, extraInputLength: Int = 0,
	              inputFilter: Regex = settings.inputFilter.getOrElse(Regex.numberPart),
	              resultFilter: Regex = settings.resultFilter.getOrElse(Regex.number),
	              validate: Option[Option[A] => InputValidationResult] = None,
	              markMinMax: Pair[Boolean] = Pair.twice(false))
	             (parse: Value => Option[A]) =
	{
		implicit def localizer: Localizer = contextPointer.value.localizer
		
		// Field width is based on minimum and maximum values and their lengths
		val minMaxStrings = allowedRange.ends.map { _.toString }
		val extraCharsString = String.valueOf(Vector.fill(extraInputLength)('0'))
		val maxLength = minMaxStrings.map { _.length }.max + extraInputLength
		// TODO: Uses a static default width, although the context is variable => May require refactoring
		val fontMetrics = parentHierarchy.fontMetricsWith(contextPointer.value.font)
		val stringWidthRange = minMaxStrings.map { s => fontMetrics.widthOf(s"$s$extraCharsString") }.minMax
		val defaultWidth = StackLength(stringWidthRange.first, stringWidthRange.second)
		
		// May display min / max values as hints
		val effectiveHintPointer = {
			if (markMinMax.contains(true)) {
				val autoHint = {
					if (markMinMax.second) {
						if (markMinMax.first)
							allowedRange.ends.mkString(" - ").noLanguageLocalizationSkipped
						else
							s"Up to %s".autoLocalized.interpolated(Vector(allowedRange.end))
					}
					else
						s"${allowedRange.start}+".noLanguageLocalizationSkipped
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
		val textPointer = new EventfulPointer(initialText)
		
		// Applies generic input and output processing, if not defined already
		val appliedSettings = settings.withHintPointer(effectiveHintPointer).withMaxLength(maxLength)
			.withInputFilter(inputFilter).withResultFilter(resultFilter)
		
		withSettings(appliedSettings).validating[Option[A]](defaultWidth, textPointer) { s => parse(s) } {
			// Case: Input could be parsed => Checks for min / max values
			case Some(input) =>
				implicit val ord: Ordering[A] = allowedRange.ordering
				if (input < allowedRange.start)
					InputValidationResult
						.Failure("Minimum value is %i".autoLocalized.interpolated(Vector(allowedRange.start)))
				else if (input > allowedRange.end)
					InputValidationResult
						.Failure("Maximum value is %i".autoLocalized.interpolated(Vector(allowedRange.end)))
				else
					validate match {
						case Some(validation) => validation(Some(input))
						case None => Default
					}
			// Case: Input couldn't be parsed => May inform the user
			case None =>
				if (textPointer.value.isEmpty)
					validate match {
						case Some(validation) => validation(None)
						case None => Default
					}
				else
					InputValidationResult.Failure("Not a valid number".autoLocalized)
		}
	}
	/**
	  * Creates a text field for numeric input
	  * @param allowedRange     Smallest and largest allowed input value
	  * @param initialValue     Initial input value (optional)
	  * @param extraInputLength Additional allowed input length relative to the longer of the minimum and maximum value.
	  *                         I.e. If minimum value is 0 (1 character) and maximum value is 999 (3 characters),
	  *                         'extraInputLength' of 2 would result in maximum input length of 5.
	  *                         Default = 0.
	  * @param inputFilter  A filter applied to every input character. Typically used to restrict the input to correct
	  *                     numeric characters and symbols.
	  *                     Default = an explicit filter specified in the settings, or a filter that only accepts
	  *                     characters used in decimal numbers (i.e. digits, '-', '.' and ',')
	  * @param resultFilter A filter used for formatting the input string before converting it into an output
	  *                     (numeric) value.
	  *                     Default = explicit filter specified in the settings, or a filter that formats strings
	  *                     into decimal numbers.
	  * @param markMinMax       Whether minimum and/or maximum values should be displayed as a hint at the bottom of
	  *                         this field.
	  *                         The input is provided as a pair, where the first value determines whether minimum value is
	  *                         displayed, and the second value determines whether the maximum value is displayed.
	  *                         By default, neither value is displayed.
	  *                         This hint is not displayed while there is another hint being displayed.
	  * @param parse            A function that accepts a value and yields the (numeric) value of correct type.
	  * @param validate An input validation function that is applied in addition to the allowed range
	  *                 and numeric conversion checks.
	  *                 Accepts the input value or None if this field is empty. Returns an input validation result.
	  * @tparam A Type of numeric values provided by this field
	  * @return A new input field that yields numeric values
	  */
	def validatedNumber[A](allowedRange: HasInclusiveOrderedEnds[A], initialValue: Option[A] = None, extraInputLength: Int = 0,
	                       inputFilter: Regex = settings.inputFilter.getOrElse(Regex.numberPart),
	                       resultFilter: Regex = settings.resultFilter.getOrElse(Regex.number),
	                       markMinMax: Pair[Boolean] = Pair.twice(false))
	                      (parse: Value => Option[A])(validate: Option[A] => InputValidationResult) =
		number[A](allowedRange, initialValue, extraInputLength, inputFilter, resultFilter, Some(validate),
			markMinMax)(parse)
}

/**
  * Used for defining text field creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
case class TextFieldSetup(settings: TextFieldSettings = TextFieldSettings.default)
	extends TextFieldSettingsWrapper[TextFieldSetup]
		with FromVariableContextComponentFactoryFactory[TextContext, ContextualTextFieldFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContextPointer(hierarchy: ComponentHierarchy, context: Changing[TextContext]): ContextualTextFieldFactory =
		ContextualTextFieldFactory(hierarchy, context, settings)
	
	override def withSettings(settings: TextFieldSettings) = copy(settings = settings)
	
	
	// OTHER	--------------------
	
	/**
	  * @return A new text field factory that uses the specified (variable) context
	  */
	def withContext(hierarchy: ComponentHierarchy, context: Changing[TextContext]) =
		ContextualTextFieldFactory(hierarchy, context, settings)
}

object TextField extends TextFieldSetup()
{
	// OTHER	--------------------
	
	def apply(settings: TextFieldSettings) = withSettings(settings)
}

/**
  * Used for requesting text input from the user
  * @author Mikko Hilpinen
  * @since 14.11.2020, v0.1
  */
class TextField[A](parentHierarchy: ComponentHierarchy, contextPointer: Changing[TextContext],
                   defaultWidth: StackLength, settings: TextFieldSettings = TextFieldSettings.default,
                   textContentPointer: EventfulPointer[String] = new EventfulPointer(""),
                   inputValidation: Option[A => InputValidationResult] = None)
				  (parseResult: String => A)
	extends ReachComponentWrapper with InputWithPointer[A, Changing[A]] with FocusableWithState with FocusableWrapper
{
	// ATTRIBUTES	------------------------------------------
	
	private val _statePointer = new EventfulPointer[FieldState](BeforeEdit)
	private val goToEditInputListener: ChangeListener[Any] = ChangeListener.onAnyChange {
		if (hasFocus)
			_statePointer.value = Editing
		Detach
	}
	
	private val isEmptyPointer = textContentPointer.map { _.isEmpty }
	
	override val valuePointer = settings.resultFilter match {
		case Some(filter) => textContentPointer.map { text => parseResult(filter.filter(text)) }
		case None => textContentPointer.map(parseResult)
	}
	
	// Input validation affects hint and highlight logic, if specified
	// Also, displayed text affects prompt-displaying
	private val appliedFieldSettings = {
		val (actualHintPointer, actualHighlightingPointer) = inputValidation match {
			// Case: Input validation is used => Merges validation results input other pointers
			case Some(validation) =>
				// The input is validated only after (and not during) editing
				val validationResultPointer = valuePointer.mergeWith(_statePointer) { (value, state) =>
					if (state == AfterEdit) validation(value) else InputValidationResult.Default
				}
				val hintTextPointer = validationResultPointer.mergeWith(settings.hintPointer) { (validation, hint) =>
					validation.message.nonEmptyOrElse(hint)
				}
				val colorPointer: Changing[Option[ColorRole]] = validationResultPointer
					.mergeWith(settings.highlightPointer) { (validation, default) =>
						validation.highlighting.orElse(default)
					}
				hintTextPointer -> colorPointer
			// Case: Input validation is not used => Uses the pointers specified earlier
			case None => settings.hintPointer -> settings.highlightPointer
		}
		val actualPromptPointer = settings.promptPointer.notFixedWhere { _.isEmpty } match {
			case Some(promptPointer) =>
				// Displays the prompt while text starts with the same characters or is empty
				promptPointer.mergeWith(textContentPointer) { (prompt, text) =>
					if (text.isEmpty || prompt.string.startsWith(text)) prompt else LocalizedString.empty
				}
			case None => settings.promptPointer
		}
		settings.fieldSettings
			.withHintPointer(actualHintPointer)
			.withHighlightPointer(actualHighlightingPointer)
			.withPromptPointer(actualPromptPointer)
	}
	
	private val _wrapped = Field.withContext(parentHierarchy, contextPointer).withSettings(appliedFieldSettings)
		.apply(isEmptyPointer) { fieldContext =>
			// Modifies the context
			val labelContextPointer = fieldContext.contextPointer
				.mapWhile(parentHierarchy.linkPointer) { _.withHorizontallyExpandingText }
			// Assigns focus listeners and prompt drawers to label settings
			val mainFocusListener: FocusListener = {
				// Remembers the first time this field received focus
				case FocusGained => textContentPointer.addListener(goToEditInputListener)
				// Formats text contents whenever focus is lost
				case FocusLost =>
					textContentPointer.removeListener(goToEditInputListener)
					_statePointer.value = AfterEdit
					settings.resultFilter.foreach { filter => textContentPointer.update(filter.filter) }
				case _ => ()
			}
			val appliedLabelSettings = settings.editingSettings
				.withAdditionalFocusListeners(Vector(fieldContext.focusListener, mainFocusListener))
				.withAdditionalCustomDrawers(fieldContext.promptDrawers)
			EditableTextLabel.withContext(fieldContext.parentHierarchy, labelContextPointer)
				.withSettings(appliedLabelSettings)
				.apply(textContentPointer)
		} { fieldContext =>
			// Case: Shows input character count at the bottom right label
			if (settings.showsCharacterCount)
				settings.maxLength.map { maxLength =>
					implicit def localizer: Localizer = fieldContext.contextPointer.value.localizer
					
					val textLengthPointer = textContentPointer.map { _.length }
					Open.using(ViewTextLabel) {
						_.withContextPointer(fieldContext.contextPointer).apply(
							textLengthPointer,
							DisplayFunction.functionToDisplayFunction[Int] { length =>
								s"%i / %i".noLanguage.localized.interpolated(Vector(length, maxLength))
							})
					}(parentHierarchy.top)
				}
			// Case: No bottom right label is required
			else
				None
		}
	
	
	// INITIAL CODE	------------------------------------------
	
	// Will not shrink below the default width
	_wrapped.wrappedField.addConstraintOver(X)(MaxBetweenLengthModifier(defaultWidth))
	
	
	// COMPUTED	----------------------------------------------
	
	/**
	  * @return A read-only version of this text field's text pointer
	  */
	def textPointer = textContentPointer.readOnly
	/**
	  * @return A pointer that contains the current state of this field
	  */
	def statePointer = _statePointer.readOnly
	
	/**
	  * @return The current state of this field
	  */
	def state = statePointer.value
	
	/**
	  * @return The maximum input length of this field.
	  *         None if there is no maximum.
	  */
	def maxLength = settings.maxLength
	
	
	// IMPLEMENTED	------------------------------------------
	
	override def hasFocus = _wrapped.hasFocus
	
	override protected def wrapped = _wrapped
	override protected def focusable = _wrapped.wrappedField
	
	
	// OTHER	----------------------------------------------
	
	/**
	  * Clears this field of all text
	  */
	@deprecated("This method will be removed, as it violates this field's capsuling principles", "< v1.1.1")
	def clear() = textContentPointer.value = ""
}
