package utopia.reach.test

import utopia.firmament.localization.DisplayFunction
import utopia.firmament.localization.LocalString._
import utopia.firmament.model.enumeration.StackLayout.Trailing
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackLength
import utopia.flow.view.immutable.eventful.Fixed
import utopia.paradigm.enumeration.Axis.X
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
		framingF.build(Stack).apply(margins.aroundMedium) { stackF =>
			// Stack (Y)
			stackF.build(Stack) { rowF =>
				// Each row contains a text field and a value label
				def makeRow[A](displayFunction: DisplayFunction[A])(makeField: ContextualTextFieldFactory => TextField[A]) =
				{
					rowF.copy(axis = X, layout = Trailing, areRelated = true).build(Mixed) { row =>
						val field = makeField(row(TextField))
						val summary = row(ViewTextLabel)(field.valuePointer, displayFunction)
						Vector(field, summary)
					}.parent
				}
				
				// Contains 5 rows
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
	}
	
	// Displays the window
	window.setToExitOnClose()
	window.setToCloseOnEsc()
	window.display(centerOnParent = true)
	start()
}
