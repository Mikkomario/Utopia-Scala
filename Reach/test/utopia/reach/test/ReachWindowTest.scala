package utopia.reach.test

import utopia.firmament.context.{TextContext, WindowContext}
import utopia.firmament.drawing.immutable.BackgroundDrawer
import utopia.firmament.model.enumeration.WindowResizePolicy.User
import utopia.flow.time.TimeExtensions._
import utopia.paradigm.color.Color
import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.shape.shape2d.Size
import utopia.reach.component.label.empty.EmptyLabel
import utopia.reach.window.{ReachWindow, ReachWindowContext}
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackSize
import utopia.flow.async.process.Loop
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.reach.component.label.text.ViewTextLabel

/**
  * Tests simple reach window creation
  * @author Mikko Hilpinen
  * @since 13.4.2023, v1.0
  */
object ReachWindowTest extends App
{
	System.setProperty("sun.java2d.noddraw", true.toString)
	ParadigmDataType.setup()
	
	import utopia.reflection.test.TestContext._
	import TestCursors._
	
	val baseWc = WindowContext(actorHandler/*, User*/)
	implicit val wc: ReachWindowContext = ReachWindowContext(baseWc, cursors)//.revalidatingAfter(0.1.seconds, 0.5.seconds)
	
	val textPointer = new PointerWithEvents("Text")
	
	val (window, canvas) = ReachWindow(title = "Test") { hierarchy =>
		// EmptyLabel(hierarchy).withBackground(Color.magenta, StackSize.any(Size(400, 200)))
		val bg = colorScheme.primary
		implicit val c: TextContext = baseContext.against(bg).forTextComponents.larger.larger.withTextInsetsScaledBy(4)
		ViewTextLabel(hierarchy).contextual.apply(textPointer, customDrawers = Vector(BackgroundDrawer(bg)))
	}
	
	println(s"Initial window size: ${ window.size }")
	window.sizePointer.addContinuousListener { e => println(s"Window size: ${ e.newValue }") }
	
	Loop.regularly(5.seconds, waitFirst = true) {
		println()
		textPointer.update { t => s"more $t\n$t" }
	}
	
	window.setToExitOnClose()
	window.setToCloseOnEsc()
	window.display()
}
