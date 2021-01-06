package utopia.reach.test

import utopia.flow.async.Loop
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.util.TimeExtensions._
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.shape.Axis.X
import utopia.reach.component.label.TextLabel
import utopia.reach.component.wrapper.ComponentCreationResult
import utopia.reach.container.{Framing, ReachCanvas, Stack, ViewStack}
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.localization.LocalString._
import utopia.reflection.util.SingleFrameSetup

/**
  * Tests view stack component
  * @author Mikko Hilpinen
  * @since 6.1.2021, v1
  */
object ViewStackTest extends App
{
	System.setProperty("sun.java2d.noddraw", true.toString)
	GenesisDataType.setup()
	import utopia.reflection.test.TestContext._
	import TestCursors._
	
	val numberPointer = new PointerWithEvents[Int](1)
	
	val canvas = ReachCanvas(cursors) { hierarchy =>
		Framing(hierarchy).buildFilledWithContext(baseContext, colorScheme.primary.light, ViewStack)
			.apply(margins.medium.any.square) { stackF =>
				stackF.mapContext { _.forTextComponents.expandingToRight }.build(TextLabel).withFixedStyle(X) { labelFactories =>
					(1 to 9).map { i =>
						labelFactories.next().apply(i.toString.noLanguageLocalizationSkipped) ->
							Some(numberPointer.map { _ >= i })
					}.toVector
				}
			}
	}.parent
	
	val frame = Frame.windowed(canvas, "Reach Test", Program)
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
	
	// Updates the number within a background loop
	Loop(1.seconds) { numberPointer.update { i => if (i >= 9) 1 else i + 1 } }.startAsync()
}
