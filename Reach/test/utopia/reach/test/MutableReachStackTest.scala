package utopia.reach.test

import utopia.firmament.controller.data.ContainerContentDisplayer
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.genesis.event.KeyTypedEvent
import utopia.genesis.handling.KeyTypedListener
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.paradigm.color.ColorRole.Secondary
import utopia.reach.component.label.text.MutableViewTextLabel
import utopia.reach.component.wrapper.Open
import utopia.reach.container.ReachCanvas
import utopia.reach.container.multi.MutableStack
import utopia.reach.window.ReachWindow

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
		stackF.column[MutableViewTextLabel[Int]](cap = margins.aroundMedium)
	}
	
	// Adds stack content management
	val dataPointer = new PointerWithEvents[Vector[Int]](Vector(1, 2, 3))
	ContainerContentDisplayer.forStatelessItems(window.content, dataPointer) { i =>
		implicit val c: ReachCanvas = window.canvas
		Open.withContext(windowContext.withHorizontallyCenteredText)(MutableViewTextLabel) { labelF =>
			labelF.withBackground(i, Secondary)
		}
	}
	
	// Displays the window
	window.setToExitOnClose()
	window.centerOnParent()
	window.display()
	start()
	
	// Updates content in background
	var lastIndex = 3
	GlobalKeyboardEventHandler += KeyTypedListener { event: KeyTypedEvent =>
		if (event.typedChar.isDigit) {
			val newIndex = event.typedChar.asDigit
			if (newIndex >= lastIndex)
				dataPointer.value = (lastIndex to newIndex).toVector
			else
				dataPointer.value = (newIndex to lastIndex).reverseIterator.toVector
			lastIndex = newIndex
		}
	}
}
