package utopia.reach.test.interactive

import utopia.firmament.controller.data.ContainerContentDisplayer
import utopia.firmament.drawing.immutable.BorderDrawer
import utopia.firmament.model.Border
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.genesis.handling.event.keyboard.{KeyTypedEvent, KeyTypedListener, KeyboardEvents}
import utopia.paradigm.color.Color
import utopia.paradigm.color.ColorRole.Secondary
import utopia.reach.component.label.text.MutableViewTextLabel
import utopia.reach.component.wrapper.Open
import utopia.reach.container.ReachCanvas
import utopia.reach.container.multi.MutableStack
import utopia.reach.test.ReachTestContext
import utopia.reach.window.ReachWindow

import scala.util.Random

/**
  * Tests mutable Reach stack implementation and the new version of container content displayer.
  *
  * Instructions:
  *     - There should be 3 labels (1, 2, 3) visible initially
  *     - You can change the visible labels with number keys 0-9
  *         - You should be able to see numbers between your two latest keystrokes
  *     - There should be a small margin between each label
  *     - Each label should have a solid background
  *
  * @author Mikko Hilpinen
  * @since 21.10.2020, v0.1
  */
object MutableReachStackTest extends App
{
	import ReachTestContext._
	
	// Creates content stack and the window
	val window = ReachWindow.contentContextual.using(MutableStack) { (_, stackF) =>
		stackF.column.apply[MutableViewTextLabel[Int]]()
	}
	
	// Adds stack content management
	val dataPointer = EventfulPointer[Vector[Int]](Vector(1, 2, 3))
	ContainerContentDisplayer.forStatelessItems(window.content, dataPointer) { i =>
		implicit val c: ReachCanvas = window.canvas
		Open
			.withContext(windowContext.withHorizontallyCenteredText.withHorizontallyExpandingText)
			.apply(MutableViewTextLabel) { labelF =>
				labelF.withBackground(Secondary)
					.withCustomDrawer(BorderDrawer(Border(1.0, Color.red)))(i)
			}
	}
	
	// Displays the window
	window.setToCloseOnEsc()
	window.setToExitOnClose()
	window.centerOnParent()
	window.display()
	start()
	
	// Updates content in background
	var lastIndex = 3
	KeyboardEvents += KeyTypedListener.unconditional { event: KeyTypedEvent =>
		if (event.typedChar.isDigit) {
			val newIndex = event.typedChar.asDigit
			val newDigits = {
				if (newIndex >= lastIndex)
					(lastIndex to newIndex).toVector
				else
					(newIndex to lastIndex).reverseIterator.toVector
			}
			dataPointer.value = newDigits.map { i =>
				val length = 1 + Random.nextInt(6)
				val tensMod = math.pow(10, length - 1).toInt
				i * tensMod + Random.nextInt(tensMod)
			}
			lastIndex = newIndex
		}
	}
}
