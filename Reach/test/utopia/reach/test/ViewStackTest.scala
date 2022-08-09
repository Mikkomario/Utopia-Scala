package utopia.reach.test

import utopia.flow.async.Loop
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.time.TimeExtensions._
import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.enumeration.Axis.X
import utopia.reach.component.label.text.TextLabel
import utopia.reach.container.multi.stack.ViewStack
import utopia.reach.container.ReachCanvas
import utopia.reach.container.wrapper.Framing
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.localization.LocalString._
import utopia.reflection.util.SingleFrameSetup

/**
  * Tests view stack component
  * @author Mikko Hilpinen
  * @since 6.1.2021, v0.1
  */
object ViewStackTest extends App
{
	System.setProperty("sun.java2d.noddraw", true.toString)
	ParadigmDataType.setup()
	import utopia.reflection.test.TestContext._
	import TestCursors._
	
	val numberPointer = new PointerWithEvents[Int](1)
	
	val canvas = ReachCanvas(cursors) { hierarchy =>
		Framing(hierarchy).buildFilledWithContext(baseContext, colorScheme.primary.light, ViewStack)
			.apply(margins.medium.any.square) { stackF =>
				stackF.mapContext { _.forTextComponents.expandingToRight }.build(TextLabel).withFixedStyle(X) { labelFactories =>
					(1 to 9).map { i =>
						labelFactories.next().apply(i.toString.noLanguageLocalizationSkipped) ->
							numberPointer.map { _ >= i }
					}.toVector
				}
			}
	}.parent
	
	val frame = Frame.windowed(canvas, "Reach Test", Program)
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
	
	// Updates the number within a background loop
	Loop.regularly(1.seconds, waitFirst = true) { numberPointer.update { i => if (i >= 9) 1 else i + 1 } }
}
