package utopia.reflection.test.swing

import utopia.flow.generic.DataType
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.Size
import utopia.reflection.component.swing.label.EmptyLabel
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.shape.stack.{StackLength, StackSize}
import utopia.reflection.shape.LengthExtensions._

/**
  * This test creates a simple stack and sees whether the components are positioned properly
  * @author Mikko Hilpinen
  * @since 26.3.2019
  */
object StackTest extends App
{
	import utopia.reflection.test.TestContext._
	DataType.setup()

	// Creates the basic components & wrap as Stackable
	def makeItem() =
	{
		val item = new EmptyLabel().withStackSize(StackSize.any(Size(64, 64)))
		item.background = Color.cyan
		item
	}

	// Creates the stack
	val items = Vector.fill(3)(makeItem()) :+ {
		val item = new EmptyLabel().withStackSize(64.any.withLowPriority x 64.any)
		item.background = Color.cyan
		item
	}
	items.foreach { i => println(i.stackSize) }
	val stack = Stack.rowWithItems(items, StackLength.fixed(16), StackLength.fixed(16))

	stack.addResizeListener(e => println(e.newSize))
	stack.addAlternatingRowBackground(Color.yellow, Color.yellow.darkened(1.2))

	// Creates the frame
	val frame = Frame.windowed(stack, "Test")
	frame.setToExitOnClose()

	// Start the program
	frame.visible = true
}
