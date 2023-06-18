package utopia.reach.component.input.text

import utopia.firmament.component.input.InputWithPointer
import utopia.firmament.context.TextContext
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.LocalString._
import utopia.firmament.localization.{LocalizedString, Localizer}
import utopia.firmament.model.stack.StackLength
import utopia.flow.async.process
import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.range.Span
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.immutable.eventful.{AlwaysTrue, Fixed}
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.ColorRole
import utopia.reach.component.factory.contextual.VariableContextualFactory
import utopia.reach.component.factory.{FromVariableContextComponentFactoryFactory, Mixed}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.input.text.DurationField.focusTransferDelay
import utopia.reach.component.input.{FieldSettings, FieldSettingsLike}
import utopia.reach.component.label.image.ViewImageLabelSettings
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.component.label.text.selectable.{SelectableTextLabelSettings, SelectableTextLabelSettingsLike}
import utopia.reach.component.template.ReachComponentWrapper
import utopia.reach.component.wrapper.ComponentCreationResult
import utopia.reach.container.multi.ViewStack
import utopia.reach.focus.{FocusListener, ManyFocusableWrapper}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

/**
  * Common trait for duration field factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 18.06.2023, v1.1
  */
trait DurationFieldSettingsLike[+Repr]
	extends FieldSettingsLike[Repr] with SelectableTextLabelSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings that apply to the individual fields that form this component. Applied selectively.
	  */
	def fieldSettings: FieldSettings
	/**
	  * Settings that apply to each of the input text fields that form this component.
	  */
	def labelSettings: SelectableTextLabelSettings
	
	/**
	  * A pointer that determines whether this field is interactive or not
	  */
	def enabledPointer: Changing[Boolean]
	/**
	  * The initially selected value in created fields
	  */
	def initialValue: Duration
	/**
	  * The largest allowed input value (inclusive)
	  */
	def maxValue: Duration
	/**
	  * A separator placed between the individual input fields (i.e. the hours, minutes and seconds -fields)
	  */
	def separator: LocalizedString
	/**
	  * Whether an input field should be provided for seconds
	  */
	def capturesSeconds: Boolean
	/**
	  * Whether the hours, minutes and seconds -field headers should be displayed.
	  */
	def showsLabels: Boolean
	
	/**
	  * Whether an input field should be provided for seconds
	  * @param capture New captures seconds to use.
	  *                Whether an input field should be provided for seconds
	  * @return Copy of this factory with the specified captures seconds
	  */
	def withCapturesSeconds(capture: Boolean): Repr
	/**
	  * A pointer that determines whether this field is interactive or not
	  * @param p New enabled pointer to use.
	  *          A pointer that determines whether this field is interactive or not
	  * @return Copy of this factory with the specified enabled pointer
	  */
	def withEnabledPointer(p: Changing[Boolean]): Repr
	/**
	  * Settings that apply to the individual fields that form this component. Applied selectively.
	  * @param settings New field settings to use.
	  *                 Settings that apply to the individual fields that form this component. Applied selectively.
	  * @return Copy of this factory with the specified field settings
	  */
	def withFieldSettings(settings: FieldSettings): Repr
	/**
	  * The initially selected value in created fields
	  * @param v New initial value to use.
	  *          The initially selected value in created fields
	  * @return Copy of this factory with the specified initial value
	  */
	def withInitialValue(v: Duration): Repr
	/**
	  * Settings that apply to each of the input text fields that form this component.
	  * @param settings New label settings to use.
	  *                 Settings that apply to each of the input text fields that form this component.
	  * @return Copy of this factory with the specified label settings
	  */
	def withLabelSettings(settings: SelectableTextLabelSettings): Repr
	/**
	  * The largest allowed input value (inclusive)
	  * @param max New max value to use.
	  *            The largest allowed input value (inclusive)
	  * @return Copy of this factory with the specified max value
	  */
	def withMaxValue(max: Duration): Repr
	/**
	  * A separator placed between the individual input fields (i.e. the hours, minutes and seconds -fields)
	  * @param separator New separator to use.
	  *                  A separator placed between the individual input fields (i.e. the hours, minutes and seconds -fields)
	  * @return Copy of this factory with the specified separator
	  */
	def withSeparator(separator: LocalizedString): Repr
	/**
	  * Whether the hours, minutes and seconds -field headers should be displayed.
	  * @param show New shows labels to use.
	  *             Whether the hours, minutes and seconds -field headers should be displayed.
	  * @return Copy of this factory with the specified shows labels
	  */
	def withShowsLabels(show: Boolean): Repr
	
	
	// COMPUTED ------------------------
	
	/**
	  * @return Copy of this factory that requests seconds input, also
	  */
	def includingSeconds = withCapturesSeconds(capture = true)
	/**
	  * @return Copy of this factory that displays hours/minutes/seconds field names
	  */
	def withVisibleFieldNames = withShowsLabels(show = true)
	
	
	// IMPLEMENTED	--------------------
	
	override def caretBlinkFrequency = labelSettings.caretBlinkFrequency
	override def customCaretColorPointer = labelSettings.customCaretColorPointer
	override def customDrawers = labelSettings.customDrawers
	override def drawsSelectionBackground = labelSettings.drawsSelectionBackground
	override def errorMessagePointer = fieldSettings.errorMessagePointer
	override def fieldNamePointer = fieldSettings.fieldNamePointer
	override def fillBackground = fieldSettings.fillBackground
	override def focusColorRole = fieldSettings.focusColorRole
	override def focusListeners = labelSettings.focusListeners
	override def highlightColorPointer = labelSettings.highlightColorPointer
	override def highlightPointer = fieldSettings.highlightPointer
	override def hintPointer = fieldSettings.hintPointer
	override def hintScaleFactor = fieldSettings.hintScaleFactor
	override def iconPointers = fieldSettings.iconPointers
	override def imageSettings = fieldSettings.imageSettings
	override def promptPointer = fieldSettings.promptPointer
	
	override def withCaretBlinkFrequency(frequency: Duration) =
		withLabelSettings(labelSettings.withCaretBlinkFrequency(frequency))
	override def withCustomCaretColorPointer(p: Option[Changing[ColorRole]]) =
		withLabelSettings(labelSettings.withCustomCaretColorPointer(p))
	override def withCustomDrawers(drawers: Vector[CustomDrawer]) =
		withLabelSettings(labelSettings.withCustomDrawers(drawers))
	override def withDrawSelectionBackground(drawBackground: Boolean) =
		withLabelSettings(labelSettings.withDrawSelectionBackground(drawBackground))
	override def withErrorMessagePointer(p: Changing[LocalizedString]) =
		withFieldSettings(fieldSettings.withErrorMessagePointer(p))
	override def withFieldNamePointer(p: Changing[LocalizedString]) =
		withFieldSettings(fieldSettings.withFieldNamePointer(p))
	override def withFillBackground(fill: Boolean) = withFieldSettings(fieldSettings.withFillBackground(fill))
	override def withFocusColorRole(color: ColorRole) = withFieldSettings(fieldSettings.withFocusColorRole(color))
	override def withFocusListeners(listeners: Vector[FocusListener]) =
		withLabelSettings(labelSettings.withFocusListeners(listeners))
	override def withHighlightColorPointer(p: Changing[ColorRole]) =
		withLabelSettings(labelSettings.withHighlightColorPointer(p))
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
	
	
	// OTHER	--------------------
	
	def mapEnabledPointer(f: Changing[Boolean] => Changing[Boolean]) = withEnabledPointer(f(enabledPointer))
	def mapFieldSettings(f: FieldSettings => FieldSettings) = withFieldSettings(f(fieldSettings))
	def mapInitialValue(f: Duration => Duration) = withInitialValue(f(initialValue))
	def mapLabelSettings(f: SelectableTextLabelSettings => SelectableTextLabelSettings) =
		withLabelSettings(f(labelSettings))
	def mapMaxValue(f: Duration => Duration) = withMaxValue(f(maxValue))
	def mapSeparator(f: LocalizedString => LocalizedString) = withSeparator(f(separator))
}

object DurationFieldSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply()
}

/**
  * Combined settings used when constructing duration fields
  * @param fieldSettings   Settings that apply to the individual fields that form this component.
  *                        Applied selectively.
  * @param labelSettings   Settings that apply to each of the input text fields that form this component.
  * @param enabledPointer  A pointer that determines whether this field is interactive or not
  * @param initialValue    The initially selected value in created fields
  * @param maxValue        The largest allowed input value (inclusive)
  * @param separator       A separator placed between the individual input fields (i.e. the hours,
  *                        minutes and seconds -fields)
  * @param capturesSeconds Whether an input field should be provided for seconds
  * @param showsLabels     Whether the hours, minutes and seconds -field headers should be displayed.
  * @author Mikko Hilpinen
  * @since 18.06.2023, v1.1
  */
case class DurationFieldSettings(fieldSettings: FieldSettings = FieldSettings.default,
                                 labelSettings: SelectableTextLabelSettings = SelectableTextLabelSettings.default,
                                 enabledPointer: Changing[Boolean] = AlwaysTrue, initialValue: Duration = Duration.Zero,
                                 maxValue: Duration = 99.hours + 59.minutes + 59.seconds,
                                 separator: LocalizedString = ":".noLanguageLocalizationSkipped,
                                 capturesSeconds: Boolean = false, showsLabels: Boolean = false)
	extends DurationFieldSettingsLike[DurationFieldSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withCapturesSeconds(capture: Boolean) = copy(capturesSeconds = capture)
	override def withEnabledPointer(p: Changing[Boolean]) = copy(enabledPointer = p)
	override def withFieldSettings(settings: FieldSettings) = copy(fieldSettings = settings)
	override def withInitialValue(v: Duration) = copy(initialValue = v)
	override def withLabelSettings(settings: SelectableTextLabelSettings) = copy(labelSettings = settings)
	override def withMaxValue(max: Duration) = copy(maxValue = max)
	override def withSeparator(separator: LocalizedString) = copy(separator = separator)
	override def withShowsLabels(show: Boolean) = copy(showsLabels = show)
}

/**
  * Common trait for factories that wrap a duration field settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 18.06.2023, v1.1
  */
trait DurationFieldSettingsWrapper[+Repr] extends DurationFieldSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: DurationFieldSettings
	
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: DurationFieldSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def capturesSeconds = settings.capturesSeconds
	override def enabledPointer = settings.enabledPointer
	override def fieldSettings = settings.fieldSettings
	override def initialValue = settings.initialValue
	override def labelSettings = settings.labelSettings
	override def maxValue = settings.maxValue
	override def separator = settings.separator
	override def showsLabels = settings.showsLabels
	
	override def withCapturesSeconds(capture: Boolean) = mapSettings { _.withCapturesSeconds(capture) }
	override def withEnabledPointer(p: Changing[Boolean]) = mapSettings { _.withEnabledPointer(p) }
	override def withFieldSettings(settings: FieldSettings) = mapSettings { _.withFieldSettings(settings) }
	override def withInitialValue(v: Duration) = mapSettings { _.withInitialValue(v) }
	override def withLabelSettings(settings: SelectableTextLabelSettings) =
		mapSettings { _.withLabelSettings(settings) }
	override def withMaxValue(max: Duration) = mapSettings { _.withMaxValue(max) }
	override def withSeparator(separator: LocalizedString) = mapSettings { _.withSeparator(separator) }
	override def withShowsLabels(show: Boolean) = mapSettings { _.withShowsLabels(show) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: DurationFieldSettings => DurationFieldSettings) = withSettings(f(settings))
}

/**
  * Factory class used for constructing duration fields using contextual component creation information
  * @author Mikko Hilpinen
  * @since 18.06.2023, v1.1
  */
case class ContextualDurationFieldFactory(parentHierarchy: ComponentHierarchy,
                                          contextPointer: Changing[TextContext],
                                          settings: DurationFieldSettings = DurationFieldSettings.default)
	extends DurationFieldSettingsWrapper[ContextualDurationFieldFactory]
		with VariableContextualFactory[TextContext, ContextualDurationFieldFactory]
{
	// IMPLEMENTED  ---------------------------
	
	override def withContextPointer(contextPointer: Changing[TextContext]) =
		copy(contextPointer = contextPointer)
	override def withSettings(settings: DurationFieldSettings) =
		copy(settings = settings)
	
	
	// OTHER    -------------------------------
	
	/**
	  * Creates a new duration input field
	  * @param exc Implicit execution context for delayed focus transfer events
	  * @return A new duration input field
	  */
	def apply()(implicit exc: ExecutionContext) = new DurationField(parentHierarchy, contextPointer, settings)
}

/**
  * Used for defining duration field creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 18.06.2023, v1.1
  */
case class DurationFieldSetup(settings: DurationFieldSettings = DurationFieldSettings.default)
	extends DurationFieldSettingsWrapper[DurationFieldSetup]
		with FromVariableContextComponentFactoryFactory[TextContext, ContextualDurationFieldFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContextPointer(hierarchy: ComponentHierarchy,
	                                context: Changing[TextContext]): ContextualDurationFieldFactory =
		ContextualDurationFieldFactory(hierarchy, context, settings)
	
	override def withSettings(settings: DurationFieldSettings) = copy(settings = settings)
	
	
	// OTHER	--------------------
	
	/**
	  * @return A new duration field factory that uses the specified (variable) context
	  */
	def withContext(hierarchy: ComponentHierarchy, context: Changing[TextContext]) =
		ContextualDurationFieldFactory(hierarchy, context, settings)
}

object DurationField extends DurationFieldSetup()
{
	private val focusTransferDelay = 0.05.seconds
}

/**
  * A combination of input fields for the purpose of requesting a time duration
  * @author Mikko Hilpinen
  * @since 9.3.2021, v0.1
  */
class DurationField(parentHierarchy: ComponentHierarchy, contextPointer: Changing[TextContext],
                    settings: DurationFieldSettings = DurationFieldSettings.default)
				   (implicit exc: ExecutionContext)
	extends ReachComponentWrapper with InputWithPointer[Duration, Changing[Duration]] with ManyFocusableWrapper
{
	// ATTRIBUTES	-------------------------------
	
	implicit val logger: Logger = SysErrLogger
	private implicit val languageCode: String = "en"
	
	// Makes sure the passed duration argument is positive
	if (settings.maxValue <= Duration.Zero)
		throw new IllegalArgumentException("DurationField max value must be positive.")
	
	// TODO: This approach creates additional dependencies. Consider creating a cached margin (etc) pointers
	private val marginPointer = {
		if (settings.separator.isEmpty)
			contextPointer.map { _.smallStackMargin }
		else
			Fixed(StackLength.fixedZero)
	}
	
	// The input fields are placed in a horizontal stack and separated with ":"
	private val (_wrapped, (fields, _valuePointer)) = ViewStack(parentHierarchy)
		.withMarginPointer(marginPointer).centered.row.withCustomDrawers(settings.customDrawers)
		.build(Mixed) { factories =>
			// Creates the input fields
			val maxHours = settings.maxValue.toHours
			val hoursFieldLength = maxHours.toString.length
			val initialHours = settings.initialValue.toHours min maxHours
			
			val maxMinutes = if (maxHours > 0) 59 else settings.maxValue.toMinutes.toInt
			val minutesFieldLength = maxMinutes.toString.length
			val initialMinutes = (settings.initialValue - initialHours.hours).toMinutes.toInt min maxMinutes
			
			val defaultFieldSettings = TextFieldSettings(fieldSettings = settings.fieldSettings)
				.withLabelSettings(settings.labelSettings).withEnabledPointer(settings.enabledPointer)
			
			def makeField(label: => LocalizedString, prompt: LocalizedString, initialValue: Int, maxValue: Int,
						  isLeftmost: Boolean = false, isRightmost: Boolean = false) =
			{
				// Determines the actual field name to display
				val fieldNamePointer = {
					// Case: Leftmost field with a custom field name -pointer => Presents the custom field name
					if (isLeftmost && !settings.fieldNamePointer.existsFixed { _.isEmpty }) {
						// Case: Labels are also requested => Displays the label when no custom field name is present
						if (settings.showsLabels)
							settings.fieldNamePointer.map { _.nonEmptyOrElse(label) }
						// Case: No labels to display => Displays the custom field name
						else
							settings.fieldNamePointer
					}
					// Case: Default field names should be displayed => Displays those
					else if (settings.showsLabels)
						Fixed(label)
					// Case: No field name to display
					else
						LocalizedString.alwaysEmpty
				}
				// Specified custom icons are only applied to the fields at the edges
				val iconPointers = {
					if (isLeftmost)
						settings.iconPointers.withSecond(SingleColorIcon.alwaysEmpty)
					else if (isRightmost)
						settings.iconPointers.withFirst(SingleColorIcon.alwaysEmpty)
					else
						Pair.twice(SingleColorIcon.alwaysEmpty)
				}
				
				factories.next().withContextPointer(contextPointer)(TextField)
					.withSettings(defaultFieldSettings
						.withFieldNamePointer(fieldNamePointer)
						.withIconPointers(iconPointers)
						.withPromptPointer(Fixed(prompt)))
					.int(Span.numeric(0, maxValue), Some(initialValue).filter { _ > 0 },
						disableLengthHint = true)
			}
			
			// Creates the hours, minutes and seconds -fields, plus separators (if applicable)
			val hoursField = {
				if (maxHours > 0)
					Some(makeField("Hours", "h" * hoursFieldLength,
						initialHours.toInt, maxHours.toInt, isLeftmost = true))
				else
					None
			}
			val minutesField = {
				if (maxMinutes > 0)
					Some(makeField("Minutes", "m" * minutesFieldLength, initialMinutes, maxMinutes,
						hoursField.isEmpty, !settings.capturesSeconds))
				else
					None
			}
			val secondsField = {
				if (settings.capturesSeconds) {
					val maxSeconds = if (maxMinutes > 0) 59 else settings.maxValue.toSeconds.toInt
					val secondsFieldLength = maxSeconds.toString.length
					val initialSeconds = (settings.initialValue - initialHours.hours - initialMinutes.minutes)
						.toSeconds.toInt min maxSeconds
					
					Some(makeField("Seconds", "s" * secondsFieldLength, initialSeconds, maxSeconds,
						minutesField.isEmpty, isRightmost = true))
				}
				else
					None
			}
			
			// Determines the final value pointer
			def fieldDurationPointer(field: Option[TextField[Option[Int]]])(valueToDuration: Int => Duration) =
				field.map { _.valuePointer.map {
					case Some(amount) => valueToDuration(amount)
					case None => Duration.Zero
				} }
			val valuePointer = Vector(
				fieldDurationPointer(hoursField) { _.hours },
				fieldDurationPointer(minutesField) { _.minutes },
				fieldDurationPointer(secondsField) { _.seconds }
			).flatten.reduce { _.mergeWith(_) { _ + _ } }
			
			// Combines the fields
			val inputFields = Vector(hoursField, minutesField, secondsField).flatten
			val components = {
				// Case: There is only a single field to use => wraps it
				if (inputFields.size < 2)
					Vector(inputFields.head)
				// Case: There are multiple fields to combine => Places them in a stack
				else {
					// Also automatically transfers the focus between fields when they get filled
					(0 until inputFields.size - 1).foreach { sourceIndex =>
						val sourceField = inputFields(sourceIndex)
						sourceField.maxLength.foreach { maxLength =>
							val targetField = inputFields(sourceIndex + 1)
							sourceField.textPointer.addContinuousListener { event =>
								// Focus transfer is slightly delayed to avoid duplicate key event triggering
								if (event.newValue.length == maxLength &&
									event.oldValue.length != maxLength && sourceField.hasFocus)
									process.Delay(focusTransferDelay) { targetField.requestFocus() }
							}
						}
					}
					
					if (settings.separator.nonEmpty) {
						// Places separators between the fields
						// TODO: There is a potential bug here, as the separator field factories are pulled out of
						//  order from the hierarchy iterator
						val separatorFields = Vector.fill(inputFields.size - 1) {
							factories.next()(ViewTextLabel).withContextPointer(contextPointer)
								.hint.text(Fixed(settings.separator))
						}
						inputFields.dropRight(1).zip(separatorFields)
							.flatMap { case (field, separator) => Vector(field, separator) } :+ inputFields.last
					}
					else
						inputFields
				}
			}
			components.map { ComponentCreationResult(_, AlwaysTrue) } -> (inputFields, valuePointer)
		}.parentAndResult
	
	
	// COMPUTED -------------------------
	
	private implicit def localizer: Localizer = contextPointer.value.localizer
	
	
	// IMPLEMENTED	-------------------------------
	
	override protected def wrapped = _wrapped
	
	override protected def focusTargets = fields
	
	override def valuePointer = _valuePointer
}
