package utopia.reflection.test.swing

import utopia.firmament.component.HasMutableBounds
import utopia.firmament.model.stack.{StackLength, StackSize}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.FlagLike
import utopia.genesis.handling._
import utopia.genesis.handling.action.{ActionLoop, ActorHandler2}
import utopia.genesis.handling.event.mouse.{MouseButtonStateListener2, MouseEvent2, MouseMoveEvent2, MouseMoveListener2, MouseWheelListener2}
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reflection.component.swing.label.Label
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.test.TestContext._

import java.awt.Color

/**
  * This app tests mouse listening in components
  * @author Mikko Hilpinen
  * @since 21.4.2019, v1+
  */
object MouseTest extends App
{
	private class MouseEnterExitListener(val area: HasMutableBounds) extends MouseMoveListener2
	{
		override val mouseMoveEventFilter = MouseMoveEvent2.filter.enteredOrExited(area.bounds)
		
		override def handleCondition: FlagLike = AlwaysTrue
		
		override def onMouseMove(event: MouseMoveEvent2) = println("Mouse entered or exited area")
	}
	
	// Creates the basic components & wrap as Stackable
	def makeItem() = {
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
	items(1).addMouseMoveListener(MouseMoveListener2.filtering(MouseEvent2.filter.over(items(1).bounds)) { e =>
		println(s"Moving ${e.position.relative}")
	})
	items(2).addMouseButtonListener(MouseButtonStateListener2
		.leftPressed.over(items(2).bounds) { e => println(e.position.relative) })
	items(2).addMouseWheelListener(MouseWheelListener2.over(items(2).bounds) { e => println(e.wheelTurn) })
	
	frame.addKeyStateListener(KeyStateListener()(println))
	
	// Starts the program
	val actorHandler = ActorHandler2()
	val actorLoop = new ActionLoop(actorHandler)
	
	frame.startEventGenerators(actorHandler)
	actorLoop.runAsync()
	frame.visible = true
}
