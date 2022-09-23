package utopia.reflection.test.swing

import utopia.flow.async.process.Loop
import utopia.flow.time.TimeExtensions._
import utopia.paradigm.color.Color
import utopia.paradigm.generic.ParadigmDataType
import utopia.genesis.handling.ActorLoop
import utopia.genesis.handling.mutable.ActorHandler
import utopia.paradigm.shape.shape2d.Size
import utopia.reflection.component.swing.button.TextButton
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.stack.StackLayout.Leading
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.layout.wrapper.Framing
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.User
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.Font
import utopia.reflection.text.FontStyle.Plain
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.test.TestContext._

/**
  * This is a simple test implementation of text labels in a stack
  * @author Mikko Hilpinen
  * @since 24.4.2019, v1+
  */
object TextLabelStackTest extends App
{
	ParadigmDataType.setup()

	// Creates the labels
	val basicFont = Font("Arial", 12, Plain, 2)
	val labels = Vector("Here are some labels", "just", "for you").map { s =>
		TextLabel(s, basicFont,
			insets = StackInsets.symmetric(16.any, 8.fixed))
	}
	labels.foreach { _.background = Color.yellow }

	// Creates a button too
	val button = TextButton("A Button!", basicFont, Color.magenta, insets = StackInsets.symmetric(32.any, 8.any),
		borderWidth = 4) { println("The Button was pressed") }

	// Creates the stack
	val stack = Stack.columnWithItems(labels :+ button, 8.any, 16.any, Leading)
	stack.background = Color.black

	// Tests custom drawing
	labels(1).addCustomDrawer() { (d, b) =>
		d.withColor(Color.red.withAlpha(0.5).toAwt,
			Color.red.toAwt).draw(b.shrunk(Size.square(4)))
	}

	// Creates the frame and displays it
	val actorHandler = ActorHandler()
	val actionLoop = new ActorLoop(actorHandler)

	val framing = Framing.symmetric(stack, 24.downscaling.square)
	val frame = Frame.windowed(framing, "TextLabel Stack Test", User)
	frame.setToExitOnClose()

	Loop.regularly(2.seconds, waitFirst = true) {
		button.visible = !button.visible
		// println(StackHierarchyManager.description)
	}

	actionLoop.runAsync()
	StackHierarchyManager.startRevalidationLoop()
	frame.startEventGenerators(actorHandler)
	frame.visible = true
}
