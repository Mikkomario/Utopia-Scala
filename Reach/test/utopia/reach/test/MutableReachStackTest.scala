package utopia.reach.test

import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.genesis.event.KeyTypedEvent
import utopia.genesis.handling.KeyTypedListener
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.reach.component.label.text.MutableViewTextLabel
import utopia.reach.component.wrapper.Open
import utopia.reach.container.ReachCanvas
import utopia.reach.container.multi.stack.MutableStack
import utopia.reflection.color.ColorRole.Secondary
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.controller.data.ContainerContentDisplayer2
import utopia.paradigm.enumeration.Alignment
import utopia.reflection.util.SingleFrameSetup
import utopia.reflection.shape.LengthExtensions._

/**
  * Tests mutable Reach stack implementation and the new version of container content displayer
  * @author Mikko Hilpinen
  * @since 21.10.2020, v0.1
  */
object MutableReachStackTest extends App
{
	import utopia.reflection.test.TestContext._
	import TestCursors._
	
	// Creates content stack
	val (canvas, stack) = ReachCanvas(cursors) { canvasHierarchy =>
		MutableStack(canvasHierarchy).column[MutableViewTextLabel[Int]](margin = margins.small.any,
			cap = margins.medium.any)
	}.toTuple
	
	// Adds stack content management
	val bg = colorScheme.primary.light
	val dataPointer = new PointerWithEvents[Vector[Int]](Vector(1, 2, 3))
	ContainerContentDisplayer2.forStatelessItems(stack, dataPointer) { i =>
		Open.withContext(MutableViewTextLabel,
			baseContext.inContextWithBackground(bg).forTextComponents.withTextAlignment(Alignment.Center)) { f =>
			f.withBackground(i, Secondary)
		}(canvas)
	}
	canvas.background = bg
	
	// Creates and displays the frame
	val frame = Frame.windowed(canvas, "Reach Mutable Stack Test", Program)
	new SingleFrameSetup(actorHandler, frame).start()
	
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
