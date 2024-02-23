package utopia.reflection.test.swing

import utopia.firmament.controller.data.ContainerContentDisplayer
import utopia.firmament.model.enumeration.WindowResizePolicy.UserAndProgram
import utopia.firmament.model.stack.LengthExtensions._
import utopia.flow.async.process.Loop
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.paradigm.color.ColorRole.Gray
import utopia.paradigm.generic.ParadigmDataType
import utopia.reflection.component.swing.label.ItemLabel
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.test.TestContext
import utopia.reflection.util.SingleFrameSetup

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
	
	val background = colorScheme(Gray)
	val contentPointer = EventfulPointer(Vector(1, 4, 6))
	val manager = baseContext.against(background).forTextComponents.use { implicit txc =>
		ContainerContentDisplayer.forStatelessItems(stack, contentPointer) { i: Int =>
			println(s"Creating a new label ($i)")
			val label = ItemLabel.contextual(i)
			label.contentPointer.addContinuousListener { e => println(s"Label content changed: $e") }
			println(s"\tLabel created ($i)")
			label
		}
	}
	val content = stack.framed(64.any x 16.any, background)
	
	// Starts test
	val frame = Frame.windowed(content, "Component to Image Test", UserAndProgram)
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
	
	println("Starting the loop")
	val numberIter = Iterator.iterate(8) { _ + 2 }
	Loop.regularly(1.5.seconds) {
		contentPointer.update { content =>
			if (content.size > 12)
				content.take(2)
			else {
				val index = (math.random() * content.size).toInt
				content.take(index) ++ numberIter.takeNext(if (math.random() < 0.5) 1 else 2) ++ content.drop(index)
			}
		}
		println("\tNumbers updated")
	}
}
