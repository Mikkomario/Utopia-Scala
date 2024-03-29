package utopia.reflection.test.swing

import utopia.flow.parse.file.FileExtensions._
import utopia.paradigm.color.Color
import utopia.paradigm.generic.ParadigmDataType
import utopia.genesis.image.Image
import utopia.reflection.component.swing.button.TextButton
import utopia.reflection.component.swing.input.SearchFrom
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.Frame
import utopia.firmament.model.enumeration.WindowResizePolicy.Program
import utopia.paradigm.enumeration.Alignment.Center
import utopia.reflection.test.TestContext
import utopia.reflection.util.SingleFrameSetup
import utopia.firmament.model.stack.LengthExtensions._
import utopia.paradigm.color.ColorRole.{Gray, Primary, Secondary}

/**
  * Tests SearchFromField
  * @author Mikko Hilpinen
  * @since 29.2.2020, v1
  */
object SearchFromFieldTest extends App
{
	ParadigmDataType.setup()

	import TestContext._

	val searchImage = Image.readFrom("test-images/arrow-back-48dp.png").map { _.withColorOverlay(Color.white) }

	val background = colorScheme(Gray)
	val standardWidth = 320.any
	val content = baseContext.against(background).use { bc =>
		val field = bc.forTextComponents.withBackground(Primary).use { implicit fieldC =>
			SearchFrom.contextualWithTextOnly[String]("Search for string", standardWidth,
				searchIcon = searchImage.toOption) { p => SearchFrom.noResultsLabel("No results for '%s'", p) }
		}

		field.content = Vector("The first string", "Another piece of text", "More text", "Lorem ipsum", "Tramboliini",
			"Keijupuisto", "Ääkkösiä", "Pulppura", "Potentiaalinen koneisto")
		field.valuePointer.addContinuousListener { println(_) }

		val button = bc.forTextComponents.withTextAlignment(Center).withBackground(Secondary).use { implicit btnC =>
			TextButton.contextual("OK") { println(field.value) }
		}

		Stack.buildColumnWithContext() { s =>
			s += field
			s += button
		}(bc).framed(margins.medium.any, background)
	}

	val frame = Frame.windowed(content, "Search Field Test", Program)
	frame.setToCloseOnEsc()

	new SingleFrameSetup(actorHandler, frame).start()
}
