package utopia.reflection.test.swing

import utopia.paradigm.generic.ParadigmDataType
import utopia.reflection.component.swing.display.MultiLineTextView
import utopia.reflection.component.swing.input.{JDropDownWrapper, TextField}
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.localization.LocalString
import utopia.paradigm.enumeration.Alignment
import utopia.reflection.shape.stack.StackLength
import utopia.reflection.test.TestContext
import utopia.reflection.util.SingleFrameSetup
import utopia.reflection.shape.LengthExtensions._

/**
  * Tests text display with multiple lines
  * @author Mikko Hilpinen
  * @since 17.12.2019, v1+
  */
object MultiLineTextViewTest extends App
{
	ParadigmDataType.setup()

	import TestContext._

	val background = colorScheme.primary.light
	val standardWidth = StackLength(240, 360, 540)
	val content = baseContext.inContextWithBackground(background).forTextComponents.use { bc =>
		// Displays text view at the top
		val textView = bc.withoutInsets.use { implicit c =>
			MultiLineTextView.contextual("Please type in some text and then press enter",
				standardWidth.optimal, useLowPriorityForScalingSides = true)
		}
		// Creates controls to bottom
		val bottomRow = bc.forGrayFields.use { implicit fieldContext =>
			// Creates text input
			val textInput = TextField.contextualForStrings(standardWidth, prompt = "Type your own text and press enter")
			textInput.addEnterListener { s => textView.text = (s: LocalString).localizationSkipped }

			val alignSelect = JDropDownWrapper.contextual("Select Alignment", initialChoices = Alignment.values)
			alignSelect.selectOne(Alignment.Left)
			alignSelect.addValueListener { _.newValue.foreach { a => textView.alignment = a } }

			Stack.buildRowWithContext(isRelated = true) { row =>
				row += textInput
				row += alignSelect
			}
		}
		// Places the items in a stack
		bc.use { implicit stackContext =>
			Stack.buildColumnWithContext() { mainStack =>
				mainStack += textView
				mainStack += bottomRow
			}
		}
	}.framed(margins.medium.any, background)

	new SingleFrameSetup(actorHandler, Frame.windowed(content, "Multi Line Text View Test")).start()
}
