package utopia.reach.component.input.text

import utopia.flow.async.process
import utopia.flow.async.process.Delay
import utopia.flow.event.AlwaysTrue
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.immutable.eventful.{AlwaysTrue, Fixed}
import utopia.flow.view.template.eventful.ChangingLike
import utopia.paradigm.enumeration.Axis.X
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory, Mixed}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.input.text.DurationField.focusTransferDelay
import utopia.reach.component.label.text.TextLabel
import utopia.reach.component.template.ReachComponentWrapper
import utopia.reach.container.multi.stack.Stack
import utopia.reach.focus.ManyFocusableWrapper
import utopia.reflection.color.ColorRole
import utopia.reflection.color.ColorRole.Secondary
import utopia.reflection.component.context.TextContextLike
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.template.input.InputWithPointer
import utopia.reflection.container.stack.StackLayout.Center
import utopia.reflection.image.SingleColorIcon
import utopia.reflection.localization.{LocalizedString, Localizer}
import utopia.reflection.shape.stack.StackLength

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

object DurationField extends ContextInsertableComponentFactoryFactory[TextContextLike, DurationFieldFactory,
	ContextualDurationFieldFactory]
{
	private val focusTransferDelay = 0.05.seconds
	
	override def apply(hierarchy: ComponentHierarchy) = new DurationFieldFactory(hierarchy)
}

class DurationFieldFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[TextContextLike, ContextualDurationFieldFactory]
{
	override def withContext[N <: TextContextLike](context: N) =
		ContextualDurationFieldFactory(parentHierarchy, context)
}

case class ContextualDurationFieldFactory[+N <: TextContextLike](parentHierarchy: ComponentHierarchy, context: N)
	extends ContextualComponentFactory[N, TextContextLike, ContextualDurationFieldFactory]
{
	private implicit def c: TextContextLike = context
	private implicit def localizer: Localizer = context.localizer
	private implicit val languageCode: String = "en"
	
	override def withContext[N2 <: TextContextLike](newContext: N2) =
		copy(context = newContext)
	
	/**
	  * Creates a new duration input field
	  * @param initialValue Initially selected value (default = 0s)
	  * @param maxValue Maximum allowed value (default = 99h 59min 59s)
	  * @param separatorText Text placed between input fields. May be empty. (default = ":")
	  * @param enabledPointer A pointer to this input field's enabled state (default = always enabled)
	  * @param leftIconPointer A pointer to the icon displayed on the leftmost input (default = always none)
	  * @param rightIconPointer A pointer to the icon displayed on the rightmost input (default = always none)
	  * @param selectionStylePointer A pointer to the color role used to highlight selected text
	  *                              (default = always secondary)
	  * @param focusColorRole Color role used for representing focused state (default = secondary)
	  * @param customDrawers Custom drawers applied to this field (default = empty)
	  * @param captureSeconds Whether seconds should be captured (default = false = only capture hours and/or minutes)
	  * @param showLabels Whether field name labels should be displayed
	  *                   (default = false = only prompts will be displayed).
	  * @param exc Implicit execution context for delayed focus transfer events
	  * @return A new duration input field
	  */
	def apply(initialValue: Duration = Duration.Zero,
			  maxValue: Duration = 99.hours + 59.minutes + 59.seconds,
			  separatorText: LocalizedString = ":",
			  enabledPointer: ChangingLike[Boolean] = AlwaysTrue,
			  leftIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
			  rightIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
			  selectionStylePointer: ChangingLike[ColorRole] = Fixed(Secondary),
			  focusColorRole: ColorRole = Secondary, customDrawers: Vector[CustomDrawer] = Vector(),
			  captureSeconds: Boolean = false, showLabels: Boolean = false)(implicit exc: ExecutionContext) =
		new DurationField(parentHierarchy, initialValue, maxValue, separatorText, enabledPointer, leftIconPointer,
			rightIconPointer, selectionStylePointer, focusColorRole, customDrawers, captureSeconds, showLabels)
}

/**
  * A combination of input fields for the purpose of requesting a time duration
  * @author Mikko Hilpinen
  * @since 9.3.2021, v0.1
  */
class DurationField(parentHierarchy: ComponentHierarchy, initialValue: Duration = Duration.Zero,
					maxValue: Duration = 99.hours + 59.minutes + 59.seconds,
					separatorText: LocalizedString = LocalizedString.empty,
					enabledPointer: ChangingLike[Boolean] = AlwaysTrue,
					leftIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
					rightIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
					selectionStylePointer: ChangingLike[ColorRole] = Fixed(Secondary),
					focusColorRole: ColorRole = Secondary, customDrawers: Vector[CustomDrawer] = Vector(),
					captureSeconds: Boolean = false, showLabels: Boolean = false)
				   (implicit context: TextContextLike, exc: ExecutionContext)
	extends ReachComponentWrapper with InputWithPointer[Duration, ChangingLike[Duration]] with ManyFocusableWrapper
{
	// ATTRIBUTES	-------------------------------
	
	implicit val logger: Logger = SysErrLogger
	
	// Makes sure the passed duration argument is positive
	if (maxValue <= Duration.Zero)
		throw new IllegalArgumentException("DurationField max value must be positive.")
	
	private implicit val languageCode: String = "en"
	private implicit val localizer: Localizer = context.localizer
	
	// The input fields are placed in a horizontal stack and separated with ":"
	private val (_wrapped, (fields, _valuePointer)) = Stack(parentHierarchy).contextual.build(Mixed)
		.withMargin(if (separatorText.isEmpty) context.relatedItemsStackMargin else StackLength.fixedZero, X,
			Center, customDrawers = customDrawers) { factories =>
			// Creates the input fields
			val maxHours = maxValue.toHours
			val hoursFieldLength = maxHours.toString.length
			val initialHours = initialValue.toHours min maxHours
			
			val maxMinutes = if (maxHours > 0) 59 else maxValue.toMinutes.toInt
			val minutesFieldLength = maxMinutes.toString.length
			val initialMinutes = (initialValue - initialHours.hours).toMinutes.toInt min maxMinutes
			
			val fieldFactory = factories(TextField)
			def makeField(label: => LocalizedString, hint: LocalizedString, initialValue: Int, maxValue: Int,
						  isLeftmost: Boolean = false, isRightmost: Boolean = false) = fieldFactory.forInt(
				if (showLabels) Fixed(label) else Fixed(LocalizedString.empty), Fixed(hint),
				leftIconPointer = if (isLeftmost) leftIconPointer else Fixed(None),
				rightIconPointer = if (isRightmost) rightIconPointer else Fixed(None),
				enabledPointer = enabledPointer, initialValue = if (initialValue > 0) Some(initialValue) else None,
				minValue = 0, maxValue = maxValue, selectionStylePointer = selectionStylePointer,
				focusColorRole = focusColorRole, allowAutoHint = false)
			
			val hoursField =
			{
				if (maxHours > 0)
					Some(makeField("Hours", "h" * hoursFieldLength,
						initialHours.toInt, maxHours.toInt, isLeftmost = true))
				else
					None
			}
			val minutesField =
			{
				if (maxMinutes > 0)
					Some(makeField("Minutes", "m" * minutesFieldLength, initialMinutes, maxMinutes,
						hoursField.isEmpty, !captureSeconds))
				else
					None
			}
			val secondsField =
			{
				if (captureSeconds)
				{
					val maxSeconds = if (maxMinutes > 0) 59 else maxValue.toSeconds.toInt
					val secondsFieldLength = maxSeconds.toString.length
					val initialSeconds = (initialValue - initialHours.hours - initialMinutes.minutes).toSeconds.toInt min maxSeconds
					
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
			val components =
			{
				// Case: There is only a single field to use => wraps it
				if (inputFields.size < 2)
					Vector(inputFields.head)
				// Case: There are multiple fields to combine => Places them in a stack
				else
				{
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
					
					if (separatorText.nonEmpty)
					{
						// Places separators between the fields
						val separatorFactory = factories(TextLabel)
						val separatorFields = Vector.fill(inputFields.size - 1) {
							separatorFactory(separatorText, isHint = true)
						}
						inputFields.dropRight(1).zip(separatorFields)
							.flatMap { case (field, separator) => Vector(field, separator) } :+ inputFields.last
					}
					else
						inputFields
				}
			}
			components -> (inputFields, valuePointer)
		}.parentAndResult
	
	
	// IMPLEMENTED	-------------------------------
	
	override protected def wrapped = _wrapped
	
	override protected def focusTargets = fields
	
	override def valuePointer = _valuePointer
}
