package utopia.reflection.test

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.image.Image
import utopia.reflection.component.swing.SearchFrom
import utopia.reflection.component.swing.button.TextButton
import utopia.reflection.container.swing.Stack
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.util.SingleFrameSetup
import utopia.reflection.shape.LengthExtensions._
import utopia.flow.util.FileExtensions._
import utopia.genesis.color.Color
import utopia.reflection.shape.Alignment.Center
import utopia.reflection.shape.Alignment

/**
  * Tests SearchFromField
  * @author Mikko Hilpinen
  * @since 29.2.2020, v1
  */
object SearchFromFieldTest extends App
{
	GenesisDataType.setup()
	
	import TestContext._
	
	val searchImage = Image.readFrom("test-images/arrow-back-48dp.png").map { _.withColorOverlay(Color.white) }
	val searchPointer = new PointerWithEvents[Option[String]](None)
	
	val background = colorScheme.gray
	val standardWidth = 320.any
	val content = baseContext.inContextWithBackground(background).use { bc =>
		val field = bc.forTextComponents(Alignment.Left).forPrimaryColorButtons.use { implicit fieldC =>
			SearchFrom.contextualWithTextOnly[String](
				SearchFrom.noResultsLabel("No results for '%s'", searchPointer),
				"Search for string", standardWidth, searchIcon = searchImage.toOption,
				searchFieldPointer = searchPointer)
		}
		
		field.content = Vector("The first string", "Another piece of text", "More text", "Lorem ipsum", "Tramboliini",
			"Keijupuisto", "Ääkkösiä", "Pulppura", "Potentiaalinen koneisto")
		field.addValueListener { println }
		
		val button = bc.forTextComponents(Center).forSecondaryColorButtons.use { implicit btnC =>
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
