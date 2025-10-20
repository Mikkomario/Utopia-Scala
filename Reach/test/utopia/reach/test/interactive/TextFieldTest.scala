package utopia.reach.test.interactive

import utopia.firmament.localization.Display
import utopia.firmament.localization.LocalString._
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackLength
import utopia.flow.collection.immutable.range.Span
import utopia.flow.view.mutable.eventful.OnceFlatteningPointer
import utopia.reach.component.factory.Mixed
import utopia.reach.component.interactive.input.InputValidationResult.Default
import utopia.reach.component.interactive.input.InputValidationResult
import utopia.reach.component.interactive.input.text.{ContextualTextFieldFactory, TextField}
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.container.multi.Stack
import utopia.reach.container.wrapper.Framing
import utopia.reach.test.ReachTestContext
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
  *     - The fifth field should show an error if > 1.99
  *
  * @author Mikko Hilpinen
  * @since 18.11.2020, v0.1
  */
object TextFieldTest extends App
{
	import ReachTestContext._
	
	// Creates the components
	val window = ReachWindow.contentContextual.borderless.using(Framing) { (_, framingF) =>
		// Framing
		framingF.build(Stack) { stackF =>
			// Stack (Y)
			stackF.build(Stack) { rowF =>
				// Each row contains a text field and a value label
				def makeRow[A](display: Display[A])(makeField: ContextualTextFieldFactory => TextField[A]) = {
					rowF.related.row.trailing.build(Mixed) { row =>
						val field = makeField(row(TextField))
						val summary = row(ViewTextLabel)(field.valuePointer, display)
						field.linkedFlag.addListener { e => println(s"Field linked $e") }
						Vector(field, summary)
					}.parent
				}
				// A pointer based on field #3, which contains true when that field contains a value > 0.
				val intLargerThanZeroP = OnceFlatteningPointer(false)
				intLargerThanZeroP.addListenerAndSimulateEvent(false) { e =>
					println(s"Int is larger than zero: ${ e.newValue }")
				}
				
				// Contains 5 rows
				Vector(
					makeRow[String](Display.identity) {
						_.mapContext { _.withLineSplitThreshold(320) }
							.withFieldName("Text").withMaxLength(999)// .displayingCharacterCount
							.validatedString(320.any) { in =>
								println(s"Validating input '$in'")
								if (in.isEmpty) InputValidationResult.Warning("Should not be empty") else Default
							}
					},
					makeRow[String](Display.identity) {
						_.withFieldName("Text").withMaxLength(32).outlined
							.string(StackLength(160, 320))
					},
					makeRow[Option[Int]](Display.identity.optional) { factory =>
						val field = factory.withFieldName("Int").outlined.int()
						intLargerThanZeroP.complete(field.valuePointer.map { _.exists { _ > 0 } })
						field
					},
					makeRow[Option[Int]](Display.identity.optional) {
						_.withFieldName("Int+")
							.allowingSelectionWhileDisabled.withEnabledFlag(intLargerThanZeroP)
							.int(Span.numeric(0, 10))
					},
					makeRow[Option[Double]](Display.identity.optional) {
						_.withFieldName("Double").withPrompt("0.".noLanguage.skipLocalization)
							.double(Span.numeric(0.0, 1.99), expectedNumberOfDecimals = 2, round = true)
					}
				)
			}
		}
	}
	
	// window.canvas.focusManager.
	
	// Displays the window
	window.setToExitOnClose()
	window.setToCloseOnEsc()
	window.display(centerOnParent = true)
	start()
}
