package utopia.reflection.test

import java.awt.event.KeyEvent

import utopia.flow.async.ThreadPool
import utopia.genesis.color.Color
import utopia.genesis.event.{Consumable, ConsumeEvent, KeyStateEvent, MouseButtonStateEvent, MouseEvent, MouseWheelEvent}
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.handling.{ActorLoop, Drawable, KeyStateListener, MouseButtonStateListener, MouseWheelListener}
import utopia.genesis.handling.mutable.{ActorHandler, DrawableHandler, MouseButtonStateHandler, MouseMoveHandler, MouseWheelHandler}
import utopia.genesis.shape.shape2D.{Bounds, Circle, Point, Size}
import utopia.genesis.util.{Drawer, FPS}
import utopia.inception.handling.immutable.Handleable
import utopia.inception.handling.mutable.HandlerRelay
import utopia.reflection.component.swing.ScrollCanvas
import utopia.reflection.container.stack.{BoxScrollBarDrawer, ScrollAreaLike, StackHierarchyManager}
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.User
import utopia.reflection.localization.{Localizer, NoLocalization}
import utopia.reflection.shape.LengthExtensions._

import scala.concurrent.ExecutionContext

/**
  * This is a simple test implementation of scroll view
  * @author Mikko Hilpinen
  * @since 24.4.2019, v1+
  */
object ScrollCanvasTest extends App
{
	GenesisDataType.setup()
	
	implicit val language: String = "en"
	implicit val localizer: Localizer = NoLocalization
	
	// Creates the handlers
	val actorHandler = ActorHandler()
	val drawHandler = DrawableHandler()
	val mouseButtonHandler = MouseButtonStateHandler()
	val mouseWheelHandler = MouseWheelHandler()
	val mouseMoveHandler = MouseMoveHandler()
	
	val handlers = HandlerRelay(actorHandler, drawHandler, mouseButtonHandler, mouseWheelHandler, mouseMoveHandler)
	
	// Creates the drawable items
	val worldSize = Size(320, 320)
	handlers ++= (new TestCircle(Point.origin), new TestCircle(worldSize.toPoint), new TestCircle(worldSize.toPoint / 2))
	
	// Creates the canvas
	val canvas = new ScrollCanvas(worldSize, drawHandler, actorHandler, mouseButtonHandler, mouseMoveHandler,
		mouseWheelHandler, 16, BoxScrollBarDrawer(Color.black, Color.gray(0.5)),
		16, false, ScrollAreaLike.defaultFriction, None)
	
	println(canvas.stackSize)
	
	// Adds mouse wheel handling (zoom)
	private val zoomer = new Zoomer(canvas)
	canvas.addKeyStateListener(zoomer)
	mouseWheelHandler += zoomer
	
	// Creates the frame and displays it
	val actionLoop = new ActorLoop(actorHandler)
	implicit val context: ExecutionContext = new ThreadPool("Reflection").executionContext
	
	val framing = canvas.framed(0.any x 0.any)
	framing.background = Color.blue
	val frame = Frame.windowed(framing, "Scroll Canvas Test", User)
	frame.setToExitOnClose()
	
	actionLoop.registerToStopOnceJVMCloses()
	actionLoop.startAsync()
	StackHierarchyManager.startRevalidationLoop()
	frame.startEventGenerators(actorHandler)
	canvas.startDrawing(FPS(30))
	frame.isVisible = true
	
	println(StackHierarchyManager.description)
}

private class TestCircle(val position: Point) extends Drawable with Handleable with MouseButtonStateListener
{
	// ATTRIBUTES	---------------------
	
	private var circle = Circle(position, 128)
	
	
	// IMPLEMENTED	---------------------
	
	override def draw(drawer: Drawer) =
	{
		drawer.withFillColor(Color.yellow).noEdges.draw(circle)
	}
	
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
