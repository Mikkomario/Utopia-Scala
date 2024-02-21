package utopia.reflection.test.swing

import utopia.firmament.drawing.immutable.BoxScrollBarDrawer
import utopia.firmament.model.enumeration.WindowResizePolicy.User
import utopia.firmament.model.stack.LengthExtensions._
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.FlagLike
import utopia.genesis.graphics.{DrawSettings, Drawer}
import utopia.genesis.handling.Drawable
import utopia.genesis.handling.action.{ActionLoop, ActorHandler}
import utopia.genesis.handling.event.consume.Consumable
import utopia.genesis.handling.event.consume.ConsumeChoice.{Consume, Preserve}
import utopia.genesis.handling.event.keyboard.Key.Control
import utopia.genesis.handling.event.keyboard.{KeyStateEvent, KeyStateListener, KeyboardEvents}
import utopia.genesis.handling.event.mouse._
import utopia.genesis.handling.mutable._
import utopia.genesis.util.Fps
import utopia.inception.handling.immutable.Handleable
import utopia.inception.handling.mutable.HandlerRelay
import utopia.paradigm.color.Color
import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.shape.shape2d.area.Circle
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reflection.component.swing.display.ScrollCanvas
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.test.TestContext._

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
	
	// FIXME: Create a new set of handlers, which includes
	//  (actorHandler, drawHandler, mouseButtonHandler, mouseWheelHandler, mouseMoveHandler)
	val handlers = HandlerRelay(drawHandler)
	
	// Creates the drawable items
	val worldSize = Size(320, 320)
	handlers ++= Vector(new TestCircle(Point.origin), new TestCircle(worldSize.toPoint), new TestCircle(worldSize.toPoint / 2))
	
	// Creates the canvas
	val canvas = new ScrollCanvas(worldSize, drawHandler, actorHandler, mouseButtonHandler, mouseMoveHandler,
		mouseWheelHandler, None, BoxScrollBarDrawer(Color.black, Color.gray(0.5)))
	
	println(canvas.stackSize)
	
	// Adds mouse wheel handling (zoom)
	private val zoomer = new Zoomer(canvas)
	KeyboardEvents += zoomer
	mouseWheelHandler += zoomer
	
	// Creates the frame and displays it
	val actionLoop = new ActionLoop(actorHandler)
	
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
	
	override val mouseButtonStateEventFilter = Consumable.unconsumedFilter &&
		MouseButtonStateEvent.filter.leftPressed && MouseEvent.filter.over(circle)
	
	
	// IMPLEMENTED	---------------------
	
	override def handleCondition: FlagLike = AlwaysTrue
	
	override def draw(drawer: Drawer) = drawer.draw(circle)
	
	override def onMouseButtonStateEvent(event: MouseButtonStateEvent) = {
		circle = Circle(position, circle.radius * 0.8)
		Consume("Circle was clicked")
	}
}

private class Zoomer(private val canvas: ScrollCanvas) extends MouseWheelListener with KeyStateListener
{
	// ATTRIBUTES	---------------
	
	private var listening = false
	
	// Only listens to mouse wheel events while cursor is inside canvas
	override val mouseWheelEventFilter = MouseEvent.filter.over(Bounds(Point.origin, canvas.worldSize))
	override val keyStateEventFilter = KeyStateEvent.filter(Control)
	
	
	// IMPLEMENTED	---------------
	
	override def handleCondition: FlagLike = AlwaysTrue
	
	override def onMouseWheelRotated(event: MouseWheelEvent) = {
		if (listening) {
			canvas.scaling *= (1 + event.wheelTurn * 0.1)
			Consume("Canvas zoom")
		}
		else
			Preserve
	}
	
	override def onKeyState(event: KeyStateEvent) = listening = event.pressed
}
