package utopia.reflection.test

import java.awt.Color

import utopia.flow.async.ThreadPool
import utopia.flow.generic.DataType
import utopia.genesis.event.{MouseEvent, MouseMoveEvent}
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.handling.{ActorLoop, KeyStateListener, MouseButtonStateListener, MouseMoveListener, MouseWheelListener}
import utopia.genesis.shape.shape2D.Size
import utopia.inception.handling.immutable.Handleable
import utopia.reflection.component.Area
import utopia.reflection.component.swing.label.Label
import utopia.reflection.container.swing.Stack
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.localization.{Localizer, NoLocalization}
import utopia.reflection.shape.{StackLength, StackSize}

import scala.concurrent.ExecutionContext

/**
  * This app tests mouse listening in components
  * @author Mikko Hilpinen
  * @since 21.4.2019, v1+
  */
object MouseTest extends App
{
	DataType.setup()
	
	implicit val language: String = "en"
	implicit val localizer: Localizer = NoLocalization
	
	private class MouseEnterExitListener(val area: Area) extends MouseMoveListener with Handleable
	{
		override val mouseMoveEventFilter = e =>
		{
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
		println("Moving " + e.mousePosition) })
	items(2).addMouseButtonListener(MouseButtonStateListener.onLeftPressedInside(items(2).bounds) {
		e => println(e.mousePosition); None })
	items(2).addMouseWheelListener(MouseWheelListener.onWheelInsideArea(items(2).bounds) { e => println(e.wheelTurn); None })
	
	frame.addKeyStateListener(KeyStateListener()(println))
	
	// Starts the program
	val actorHandler = ActorHandler()
	val actorLoop = new ActorLoop(actorHandler)
	
	implicit val context: ExecutionContext = new ThreadPool("Mouse Test").executionContext
	
	frame.startEventGenerators(actorHandler)
	actorLoop.startAsync()
	frame.isVisible = true
}
