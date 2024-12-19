package utopia.reflection.reach.test

import utopia.firmament.model.enumeration.WindowResizePolicy.Program
import utopia.firmament.model.stack.LengthExtensions._
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.paradigm.color.Color
import utopia.paradigm.color.ColorRole.Secondary
import utopia.paradigm.generic.ParadigmDataType
import utopia.reach.component.input.text.TextField
import utopia.reflection.component.swing.button.TextButton
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.reach.ReflectionReachCanvas
import utopia.reflection.util.SingleFrameSetup

/**
  * Tests use of Reach components inside a Reflection component context
  * @author Mikko Hilpinen
  * @since 29.1.2021, v0.1
  */
object ReachWithinReflectionTest extends App
{
	System.setProperty("sun.java2d.noddraw", true.toString)
	
	ParadigmDataType.setup()
	import utopia.reach.test.TestCursors._
	import utopia.reflection.test.TestContext._
	
	val textPointer = EventfulPointer[String]("")
	val background = colorScheme.gray.light
	val canvas = ReflectionReachCanvas(Color.black.withAlpha(0.0), cursors) { hierarchy =>
		TextField.withContext(hierarchy, baseContext.against(background).forTextComponents.toVariableContext)
			.withFieldName("Test Field")
			.string(320.any, textPointer = textPointer)
	}
	
	val button = baseContext.against(background).forTextComponents.withBackground(Secondary).use { implicit c =>
		TextButton.contextual("OK") { textPointer.value = "" }
	}
	println(button.component.isFocusable)
	
	val mainContent = baseContext.against(background).use { implicit c =>
		Stack.buildRowWithContext() { s =>
			s += canvas.parent
			s += button
		}.framed(margins.medium.any, c.background)
	}
	
	val frame = Frame.windowed(mainContent, "Reach Reflection Test", Program)
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
}
