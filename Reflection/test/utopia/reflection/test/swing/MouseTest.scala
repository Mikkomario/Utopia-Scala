package utopia.reflection.test.swing

import utopia.flow.generic.model.mutable.DataType

import java.awt.Color
import utopia.genesis.event.{MouseEvent, MouseMoveEvent}
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.handling._
import utopia.paradigm.shape.shape2d.Size
import utopia.inception.handling.immutable.Handleable
import utopia.reflection.component.swing.label.Label
import utopia.reflection.component.template.layout.Area
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.shape.stack.{StackLength, StackSize}
import utopia.reflection.test.TestContext._

/**
  * This app tests mouse listening in components
  * @author Mikko Hilpinen
  * @since 21.4.2019, v1+
  */
object MouseTest extends App
{
	
	
	private class MouseEnterExitListener(val area: Area) extends MouseMoveListener with Handleable
	{
		override val mouseMoveEventFilter = e => {
			val b = area.bounds
			e.enteredArea(b) || e.exitedArea(b)
		}
		
		override def onMouseMove(event: MouseMoveEvent) = println("Mouse entered or exited area")
	}
	
	// Creates the basic components & wrap as Stackable
	def makeItem() =
	{
		val item = Label().withStackSize(StackSize.any(Size(64, 64)))
		item.background = Color.BLUE
		item
	}
	
	// Creates the stack
	val items = Vector.fill(3)(makeItem())
	val stack = Stack.rowWithItems(items, StackLength.fixed(16), StackLength.fixed(16))
	stack.background = Color.ORANGE
	
	// Creates the frame
	val frame = Frame.windowed(stack, "Test")
	frame.setToExitOnClose()
	
	// Sets up mouse listening
	items.head.addMouseMoveListener(new MouseEnterExitListener(items.head))
	items(1).addMouseMoveListener(MouseMoveListener(MouseEvent.isOverAreaFilter(items(1).bounds)) { e =>
		println("Moving " + e.mousePosition)
	})
	items(2).addMouseButtonListener(MouseButtonStateListener.onLeftPressedInside(items(2).bounds) {
		e => println(e.mousePosition); None
	})
	items(2).addMouseWheelListener(MouseWheelListener.onWheelInsideArea(items(2).bounds) { e => println(e.wheelTurn); None })
	
	frame.addKeyStateListener(KeyStateListener()(println))
	
	// Starts the program
	val actorHandler = ActorHandler()
	val actorLoop = new ActorLoop(actorHandler)
	
	frame.startEventGenerators(actorHandler)
	actorLoop.runAsync()
	frame.visible = true
}
