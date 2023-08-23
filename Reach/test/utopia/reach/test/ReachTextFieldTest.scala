package utopia.reach.test

import utopia.firmament.localization.DisplayFunction
import utopia.firmament.localization.LocalString._
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackLength
import utopia.flow.collection.immutable.range.Span
import utopia.flow.time.Now
import utopia.paradigm.shape.shape2d.Bounds
import utopia.reach.component.factory.Mixed
import utopia.reach.component.input.InputValidationResult
import utopia.reach.component.input.InputValidationResult.Default
import utopia.reach.component.input.text.{ContextualTextFieldFactory, TextField}
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.container.multi.Stack
import utopia.reach.container.wrapper.Framing
import utopia.reach.window.ReachWindow

/**
  * A simple test for text fields.
  *
  * Instructions:
  *     - You should see 5 text fields
  *     - Each field should show their output value next to them
  *     - The first and the second field should have a maximum length (visible in first)
  *     - The first field should show a warning when empty
  *     - The fourth field should show an error if negative
  *     - The fifth field should show an error if > 1.0
  *
  * @author Mikko Hilpinen
  * @since 18.11.2020, v0.1
  */
object ReachTextFieldTest extends App
{
	import ReachTestContext._
	
	// Creates the components
	val window = ReachWindow.contentContextual.using(Framing) { (_, framingF) =>
		// Framing
		framingF.build(Stack) { stackF =>
			// Stack (Y)
			stackF.build(Stack) { rowF =>
				// Each row contains a text field and a value label
				def makeRow[A](displayFunction: DisplayFunction[A])(makeField: ContextualTextFieldFactory => TextField[A]) =
				{
					rowF.related.row.trailing.build(Mixed) { row =>
						val field = makeField(row(TextField))
						val summary = row(ViewTextLabel)(field.valuePointer, displayFunction)
						Vector(field, summary)
					}.parent
				}
				
				// Contains 5 rows
				Vector(
					makeRow[String](DisplayFunction.raw) {
						_.withFieldName("Text").withMaxLength(32).displayingCharacterCount
							.validatedString(320.any) { in =>
								println(s"Validating input '$in'")
								if (in.isEmpty) InputValidationResult.Warning("Should not be empty") else Default
							}
					},
					makeRow[String](DisplayFunction.raw) {
						_.withFieldName("Text").withMaxLength(32).outlined
							.string(StackLength(160, 320))
					},
					makeRow[Option[Int]](DisplayFunction.rawOption) { _.withFieldName("Int").outlined.int() },
					makeRow[Option[Int]](DisplayFunction.rawOption) {
						_.withFieldName("Int+").outlined.int(Span.numeric(0, 10))
					},
					makeRow[Option[Double]](DisplayFunction.rawOption) {
						_.withFieldName("Double").withPrompt("0.".noLanguageLocalizationSkipped)
							.double(Span.numeric(0.0, 1.0), expectedNumberOfDecimals = 2)
					}
				)
			}
		}
	}
	
	// Displays the window
	window.boundsPointer.addContinuousListenerAndSimulateEvent(Bounds.zero) { e =>
		println(s"${Now.toLocalTime}: ${e.newValue}")
	}
	window.setToExitOnClose()
	window.setToCloseOnEsc()
	window.display(centerOnParent = true)
	start()
}
