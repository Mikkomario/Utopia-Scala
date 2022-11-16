package utopia.reach.test

import utopia.flow.generic.model.mutable.DataType
import utopia.flow.view.immutable.eventful.Fixed
import utopia.reach.component.factory.Mixed
import utopia.reach.component.input.InputValidationResult
import utopia.reach.component.input.InputValidationResult.Default
import utopia.reach.component.input.text.{ContextualTextFieldFactory, TextField}
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.container.multi.stack.Stack
import utopia.reach.container.ReachCanvas
import utopia.reach.container.wrapper.Framing
import utopia.reflection.component.context.TextContext
import utopia.reflection.container.stack.StackLayout.Trailing
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.localization.DisplayFunction
import utopia.reflection.test.TestContext
import utopia.reflection.util.SingleFrameSetup
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.localization.LocalString._
import utopia.reflection.shape.stack.StackLength

/**
  * A simple test for text fields
  * @author Mikko Hilpinen
  * @since 18.11.2020, v0.1
  */
object ReachTextFieldTest extends App
{
	System.setProperty("sun.java2d.noddraw", true.toString)
	
	import TestContext._
	import TestCursors._
	
	// Creates text fields (+ result views)
	val canvas = ReachCanvas(cursors) { hierarchy =>
		Framing(hierarchy).buildFilledWithContext(baseContext, colorScheme.gray, Stack).apply(margins.medium.any.square) { stack =>
			stack.build(Stack).column() { r =>
				val rows = r.mapContext { _.forTextComponents }
				
				def makeRow[A](displayFunction: DisplayFunction[A])(makeField: ContextualTextFieldFactory[TextContext] => TextField[A]) =
				{
					rows.build(Mixed).row(layout = Trailing, areRelated = true) { row =>
						val field = makeField(row(TextField))
						val summary = row(ViewTextLabel)(field.valuePointer, displayFunction)
						Vector(field, summary)
					}.parent
				}
				
				Vector(
					makeRow[String](DisplayFunction.raw) {
						_.forString(320.any, Fixed("Text"), maxLength = Some(32),
							inputValidation = Some(in =>
								if (in.isEmpty) InputValidationResult.Warning("Should not be empty") else Default),
							showCharacterCount = true)
					},
					makeRow[String](DisplayFunction.raw) {
						_.forString(StackLength(160, 320), Fixed("Text"), maxLength = Some(32), fillBackground = false)
					},
					makeRow[Option[Int]](DisplayFunction.rawOption) { _.forInt(Fixed("Int"), fillBackground = false) },
					makeRow[Option[Int]](DisplayFunction.rawOption) {
						_.forInt(Fixed("Int+"),
							minValue = 0, maxValue = 10, fillBackground = false)
					},
					makeRow[Option[Double]](DisplayFunction.rawOption) {
						_.forDouble(0.0, 1.0,
							Fixed("Double"), Fixed("0.".noLanguageLocalizationSkipped), proposedNumberOfDecimals = 2)
					}
				)
			}
		}
	}.parent
	
	val frame = Frame.windowed(canvas, "Reach Test", Program)
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
}
