package utopia.reflection.test.swing

import utopia.flow.async.process.Loop
import utopia.flow.generic.model.mutable.DataType

import java.awt.Color
import javax.swing.JLabel
import utopia.flow.time.TimeExtensions._
import utopia.reflection.component.swing.template.{JStackableWrapper, JWrapper}
import utopia.reflection.component.template.layout.stack.ReflectionStackLeaf
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.Frame
import utopia.firmament.model.enumeration.WindowResizePolicy.Program
import utopia.firmament.model.stack.{StackLength, StackSize}
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reflection.test.TestContext._

/**
  * This test creates a simple stack and sees whether the components are positioned properly
  * @author Mikko Hilpinen
  * @since 26.3.2019
  */
object StackHierarchyTest extends App
{
	

	private class ChangingWrapper extends JStackableWrapper with ReflectionStackLeaf
	{
		// ATTRIBUTES   -----------------

		val component = new JLabel()

		private var currentSize = StackSize.fixed(Size(64, 64))
		private var isBuffed = false


		// INITIAL CODE -----------------

		component.setBackground(Color.RED)
		component.setOpaque(true)


		// IMPLEMENTED  -----------------

		override def updateLayout() = ()

		override def calculatedStackSize =
		{
			println("Requesting up-to-date stack size calculation")
			currentSize
		}


		// OTHER    ---------------------

		def pulse() =
		{
			if (isBuffed)
				currentSize /= 2
			else
				currentSize *= 2

			isBuffed = !isBuffed
			revalidate()
		}
	}

	// Creates the basic components & wrap as Stackable
	def makeItem() =
	{
		val item = JWrapper(new JLabel()).withStackSize(StackSize.any(Size(64, 64)))
		item.background = Color.BLUE
		item
	}

	// Creates the stack
	private val item = new ChangingWrapper()
	val items = Vector.fill(3)(makeItem()) :+ item
	val stack = Stack.rowWithItems(items, StackLength.fixed(16), StackLength.fixed(16))

	stack.background = Color.ORANGE

	// Creates the frame
	val frame = Frame.windowed(stack, "Test", Program)
	frame.setToExitOnClose()

	// The last item will pulse every second
	Loop.regularly(1.seconds, waitFirst = true) { item.pulse() }
	
	// Start the program
	StackHierarchyManager.startRevalidationLoop()

	frame.visible = true
}
