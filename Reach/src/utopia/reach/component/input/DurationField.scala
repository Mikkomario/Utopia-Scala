package utopia.reach.component.input

import utopia.flow.event.{AlwaysTrue, ChangingLike, Fixed}
import utopia.flow.util.TimeExtensions._
import utopia.reach.component.factory.Mixed
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.TextLabel
import utopia.reach.component.template.ReachComponentWrapper
import utopia.reach.container.Stack
import utopia.reach.focus.ManyFocusableWrapper
import utopia.reflection.color.ColorRole
import utopia.reflection.color.ColorRole.Secondary
import utopia.reflection.component.context.TextContextLike
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.template.input.InputWithPointer
import utopia.reflection.container.stack.StackLayout.Center
import utopia.reflection.image.SingleColorIcon
import utopia.reflection.localization.{LocalizedString, Localizer}

import scala.concurrent.duration.Duration

/**
  * A combination of input fields for the purpose of requesting a time duration
  * @author Mikko Hilpinen
  * @since 9.3.2021, v1
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
				   (implicit context: TextContextLike)
	extends ReachComponentWrapper with InputWithPointer[Duration, ChangingLike[Duration]] with ManyFocusableWrapper
{
	// ATTRIBUTES	-------------------------------
	
	// Makes sure the passed duration argument is positive
	if (maxValue <= Duration.Zero)
		throw new IllegalArgumentException("DurationField max value must be positive.")
	
	private implicit val languageCode: String = "en"
	private implicit val localizer: Localizer = context.localizer
	
	// The input fields are placed in a horizontal stack and separated with ":"
	private val (_wrapped, (fields, _valuePointer)) = Stack(parentHierarchy).contextual.build(Mixed)
		.row(Center, customDrawers = customDrawers, areRelated = true) { factories =>
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
							sourceField.textPointer.addListener { event =>
								if (event.newValue.length == maxLength && event.oldValue.length != maxLength && sourceField.hasFocus)
									targetField.requestFocus()
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
