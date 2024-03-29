package utopia.reflection.test.swing

import utopia.flow.parse.file.FileExtensions._
import utopia.genesis.image.Image
import utopia.paradigm.color.Color
import utopia.paradigm.color.ColorRole.Gray
import utopia.paradigm.generic.ParadigmDataType
import utopia.reflection.component.swing.input.DropDown
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.layout.wrapper.AnimatedSizeContainer
import utopia.reflection.container.swing.window.Frame
import utopia.firmament.model.enumeration.WindowResizePolicy.Program
import utopia.reflection.test.TestContext
import utopia.reflection.util.SingleFrameSetup

import scala.collection.immutable.HashMap

/**
  * Rewritten version of drop down test for the new drop down implementation
  * @author Mikko Hilpinen
  * @since 15.3.2020, v1
  */
object DropDownTest2 extends App
{
	ParadigmDataType.setup()
	
	import TestContext._
	
	// Creates view content
	val data = HashMap("Fighter" -> Vector("Aragorn", "Gimli", "Boromir"), "Archer" -> Vector("Legolas"),
		"Wizard" -> Vector("Gandalf", "Radagast"))
	
	val ddIcon = Image.readOrEmpty("Reflection/test-images/arrow-back-48dp.png")
	
	val backgroundColor = colorScheme.primary
	val stack = (baseContext.against(backgroundColor).forTextComponents/Gray).use { implicit context =>
		val categorySelect = DropDown.contextualWithTextOnly[String](
			TextLabel.contextual("No content available", isHint = true), ddIcon, "Select Class")
		val characterSelect = DropDown.contextualWithTextOnly[String](
			TextLabel.contextual("No content available", isHint = true), ddIcon, "Select Character")
		
		// Adds item listeners
		categorySelect.addValueListener { c => characterSelect.content = c.newValue.flatMap(data.get) getOrElse Vector() }
		characterSelect.addValueListener { c => println(c.newValue.map { _ + " is ready for adventure!" } getOrElse "No character selected") }
		
		// Adds initial content
		categorySelect.content = data.keySet.toVector.sorted
		
		Stack.buildColumnWithContext() { stack =>
			stack += AnimatedSizeContainer(categorySelect, actorHandler)
			stack += AnimatedSizeContainer(characterSelect, actorHandler)
		}
	}
	stack.background = Color.magenta
	val content = stack//.framed(margins.medium.any, backgroundColor)
	
	// Starts test
	val frame = Frame.windowed(content, "Collection View Test", Program)
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
}
