package utopia.reflection.test.swing

import utopia.flow.async.Loop
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.CollectionExtensions._
import utopia.paradigm.generic.ParadigmDataType
import utopia.reflection.component.swing.label.ItemLabel
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.User
import utopia.reflection.controller.data.ContainerContentManager
import utopia.reflection.test.TestContext
import utopia.reflection.util.SingleFrameSetup
import utopia.reflection.shape.LengthExtensions._

/**
  * Tests stack content management
  * @author Mikko Hilpinen
  * @since 17.4.2020, v1.2
  */
object StackContentManagementTest extends App
{
	ParadigmDataType.setup()
	
	import TestContext._
	
	// Creates the main content
	val stack = Stack.column[ItemLabel[Int]]()
	
	val background = colorScheme.gray
	val manager = baseContext.inContextWithBackground(background).forTextComponents.use { implicit txc =>
		ContainerContentManager.forStatelessItems[Int, ItemLabel[Int]](stack, Vector(1, 4, 6)) { i =>
			println(s"Creating a new label ($i)")
			val label = ItemLabel.contextual(i)
			label.contentPointer.addContinuousListener { e => println(s"Label content changed: $e") }
			println(s"\tLabel created ($i)")
			label
		}
	}
	val content = stack.framed(64.any x 16.any, background)
	
	// Starts test
	val frame = Frame.windowed(content, "Component to Image Test", User)
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
	
	println("Starting the loop")
	var nextNumber = 8
	Loop.regularly(1.seconds) {
			println(s"Updating content by adding $nextNumber")
			val index = (math.random() * manager.content.size).toInt
			println(s"\tInserts $nextNumber to index $index")
			manager.content = manager.content.inserted(nextNumber, index)
			nextNumber += 2
			println("\tNumbers updated")
	}
}
