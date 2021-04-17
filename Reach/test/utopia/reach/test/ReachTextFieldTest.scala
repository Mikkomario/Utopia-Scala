package utopia.reach.test

import utopia.flow.event.Fixed
import utopia.flow.generic.DataType
import utopia.reach.component.factory.Mixed
import utopia.reach.component.input.{ContextualTextFieldFactory, TextField}
import utopia.reach.component.label.ViewTextLabel
import utopia.reach.container.{Framing, ReachCanvas, Stack}
import utopia.reflection.component.context.TextContext
import utopia.reflection.container.stack.StackLayout.Trailing
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.localization.DisplayFunction
import utopia.reflection.test.TestContext
import utopia.reflection.util.SingleFrameSetup
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.localization.LocalString._

/**
  * A simple test for text fields
  * @author Mikko Hilpinen
  * @since 18.11.2020, v0.1
  */
object ReachTextFieldTest extends App
{
	System.setProperty("sun.java2d.noddraw", true.toString)
	
	DataType.setup()
	
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
							showCharacterCount = true)
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
