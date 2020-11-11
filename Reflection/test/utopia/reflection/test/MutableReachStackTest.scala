package utopia.reflection.test

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.event.KeyTypedEvent
import utopia.reflection.color.ColorRole.Secondary
import utopia.reflection.component.reach.label.MutableViewTextLabel
import utopia.reflection.component.reach.wrapper.Open
import utopia.reflection.container.reach.MutableStack
import utopia.reflection.container.swing.ReachCanvas
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.controller.data.ContainerContentDisplayer2
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.util.SingleFrameSetup

/**
  * Tests mutable Reach stack implementation and the new version of container content displayer
  * @author Mikko Hilpinen
  * @since 21.10.2020, v2
  */
object MutableReachStackTest extends App
{
	import TestContext._
	
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
	frame.addKeyTypedListener { event: KeyTypedEvent =>
		if (event.typedChar.isDigit)
		{
			val newIndex = event.typedChar.asDigit
			if (newIndex >= lastIndex)
				dataPointer.value = (lastIndex to newIndex).toVector
			else
				dataPointer.value = (newIndex to lastIndex).reverseIterator.toVector
			lastIndex = newIndex
		}
	}
}
