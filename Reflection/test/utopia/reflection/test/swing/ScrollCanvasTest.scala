package utopia.reflection.test.swing

import utopia.genesis.event._
import utopia.genesis.graphics.{DrawSettings, Drawer}
import utopia.genesis.handling.mutable._
import utopia.genesis.handling.{ActorLoop, Drawable}
import utopia.genesis.util.Fps
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.inception.handling.immutable.Handleable
import utopia.inception.handling.mutable.HandlerRelay
import utopia.paradigm.color.Color
import utopia.paradigm.generic.ParadigmDataType
import utopia.firmament.drawing.immutable.BoxScrollBarDrawer
import utopia.reflection.component.swing.display.ScrollCanvas
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.swing.window.Frame
import utopia.firmament.model.enumeration.WindowResizePolicy.User
import utopia.firmament.model.stack.LengthExtensions._
import utopia.paradigm.shape.shape2d.area.Circle
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reflection.test.TestContext._

import java.awt.event.KeyEvent

/**
  * This is a simple test implementation of scroll view
  * @author Mikko Hilpinen
  * @since 24.4.2019, v1+
  */
object ScrollCanvasTest extends App
{
	ParadigmDataType.setup()
	
	// Creates the handlers
	val actorHandler = ActorHandler()
	val drawHandler = DrawableHandler()
	val mouseButtonHandler = MouseButtonStateHandler()
	val mouseWheelHandler = MouseWheelHandler()
	val mouseMoveHandler = MouseMoveHandler()
	
	val handlers = HandlerRelay(actorHandler, drawHandler, mouseButtonHandler, mouseWheelHandler, mouseMoveHandler)
	
	// Creates the drawable items
	val worldSize = Size(320, 320)
	handlers ++= Vector(new TestCircle(Point.origin), new TestCircle(worldSize.toPoint), new TestCircle(worldSize.toPoint / 2))
	
	// Creates the canvas
	val canvas = new ScrollCanvas(worldSize, drawHandler, actorHandler, mouseButtonHandler, mouseMoveHandler,
		mouseWheelHandler, None, BoxScrollBarDrawer(Color.black, Color.gray(0.5)))
	
	println(canvas.stackSize)
	
	// Adds mouse wheel handling (zoom)
	private val zoomer = new Zoomer(canvas)
	GlobalKeyboardEventHandler += zoomer
	mouseWheelHandler += zoomer
	
	// Creates the frame and displays it
	val actionLoop = new ActorLoop(actorHandler)
	
	val framing = canvas.framed(0.any x 0.any)
	framing.background = Color.blue
	val frame = Frame.windowed(framing, "Scroll Canvas Test", User)
	frame.setToExitOnClose()
	
	actionLoop.runAsync()
	StackHierarchyManager.startRevalidationLoop()
	frame.startEventGenerators(actorHandler)
	canvas.startDrawing(Fps(30))
	frame.visible = true
	
	println(StackHierarchyManager.description)
}

private class TestCircle(val position: Point) extends Drawable with Handleable with MouseButtonStateListener
{
	// ATTRIBUTES	---------------------
	
	private implicit val ds: DrawSettings = DrawSettings.onlyFill(Color.yellow)
	
	private var circle = Circle(position, 128)
	
	
	// IMPLEMENTED	---------------------
	
	override def draw(drawer: Drawer) = drawer.draw(circle)
	
	override def mouseButtonStateEventFilter = Consumable.notConsumedFilter &&
		MouseButtonStateEvent.leftPressedFilter && MouseEvent.isOverAreaFilter(circle)
	
	override def onMouseButtonState(event: MouseButtonStateEvent) =
	{
		circle = Circle(position, circle.radius * 0.8)
		Some(ConsumeEvent("Circle was clicked"))
	}
}

private class Zoomer(private val canvas: ScrollCanvas) extends MouseWheelListener with KeyStateListener with Handleable
{
	// ATTRIBUTES	---------------
	
	private var listening = false
	
	
	// IMPLEMENTED	---------------
	
	// Only listens to mouse wheel events while cursor is inside canvas
	override def mouseWheelEventFilter = MouseEvent.isOverAreaFilter(Bounds(Point.origin, canvas.worldSize))
	
	override def onMouseWheelRotated(event: MouseWheelEvent) =
	{
		if (listening)
		{
			canvas.scaling *= (1 + event.wheelTurn * 0.1)
			Some(ConsumeEvent("Canvas zoom"))
		}
		else
			None
	}
	
	override def keyStateEventFilter = KeyStateEvent.keyFilter(KeyEvent.VK_CONTROL)
	
	override def onKeyState(event: KeyStateEvent) = listening = event.isDown
}
