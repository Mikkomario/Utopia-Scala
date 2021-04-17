package utopia.reach.test

import utopia.flow.event.Fixed
import utopia.genesis.generic.GenesisDataType
import utopia.reach.component.input.TextField
import utopia.reach.container.ReachCanvas
import utopia.reflection.component.swing.button.TextButton
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.util.SingleFrameSetup

/**
  * Tests use of Reach components inside a Reflection component context
  * @author Mikko Hilpinen
  * @since 29.1.2021, v0.1
  */
object ReachWithinReflectionTest extends App
{
	System.setProperty("sun.java2d.noddraw", true.toString)
	
	GenesisDataType.setup()
	import utopia.reflection.test.TestContext._
	import TestCursors._
	
	val background = colorScheme.gray.light
	
	val canvas = ReachCanvas(cursors) { hierarchy =>
		TextField(hierarchy).withContext(baseContext.inContextWithBackground(background).forTextComponents)
			.forString(320.any, fieldNamePointer = Fixed("Test Field"))
	}
	
	val button = baseContext.inContextWithBackground(background).forTextComponents.forSecondaryColorButtons.use { implicit c =>
		TextButton.contextual("OK") { canvas.child.clear() }
	}
	println(button.component.isFocusable)
	
	val mainContent = baseContext.inContextWithBackground(background).use { implicit c =>
		Stack.buildRowWithContext() { s =>
			s += canvas.parent
			s += button
		}.framed(margins.medium.any, c.containerBackground)
	}
	
	val frame = Frame.windowed(mainContent, "Reach Reflection Test", Program)
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
}
