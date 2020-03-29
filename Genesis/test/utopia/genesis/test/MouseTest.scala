package utopia.genesis.test

import utopia.genesis.shape.shape2D.{Circle, Line, Point, Size, Transformation}
import utopia.genesis.util.Drawer
import java.awt.Color

import utopia.flow.async.ThreadPool
import utopia.genesis.event.MouseMoveEvent
import utopia.genesis.shape.Axis._
import utopia.genesis.view.Canvas
import utopia.genesis.view.MainFrame
import utopia.genesis.view.CanvasMouseEventGenerator
import utopia.genesis.event.MouseButtonStateEvent
import utopia.genesis.event.MouseEvent
import utopia.genesis.event.MouseWheelEvent
import utopia.genesis.handling.mutable.{ActorHandler, DrawableHandler, MouseButtonStateHandler, MouseMoveHandler, MouseWheelHandler}
import utopia.genesis.handling.{ActorLoop, Drawable, MouseButtonStateListener, MouseMoveListener, MouseWheelListener}
import utopia.inception.handling.immutable.Handleable
import utopia.inception.handling.mutable.HandlerRelay

import scala.concurrent.ExecutionContext

/**
 * This is a visual test for mouse event features. In the test, the two lines should point to the
 * mouse cursor whenever any mouse button is down. The two circles should change colour when the
 * mouse cursor hovers over the shapes. When the shapes are clicked, their colours should change.
 * Rotating the mouse wheel should increase / decrease the circle radius.
 * @author Mikko Hilpinen
 * @since 4.2.2017
 */
object MouseTest extends App
{
    class TestObject(position: Point, radius: Double) extends Drawable with
            MouseMoveListener with MouseButtonStateListener with MouseWheelListener with Handleable
	{
        private val area = Circle(Point.origin, radius)
        
        private var lastMousePosition = Point.origin
        private var mouseOver = false
        private var isOn = false
        private var transformation = Transformation.translation(position.toVector)
        
        override def draw(drawer: Drawer) = 
        {
            val copy = drawer.withPaint(Some(if (isOn) Color.BLUE else if (mouseOver) Color.CYAN
                    else Color.LIGHT_GRAY)).withAlpha(0.75)
            copy.transformed(transformation).draw(area)
            
            drawer.draw(Line(transformation.position, lastMousePosition))
        }
        
        override def onMouseMove(event: MouseMoveEvent) = 
        {
            lastMousePosition = event.mousePosition
            mouseOver = contains2D(event.mousePosition)
        }
        
        // It is possible to use super type filters in event filters, nice!
        override def mouseMoveEventFilter = MouseEvent.isLeftDownFilter
        
        // Only accepts mouse press events
        override def mouseButtonStateEventFilter = MouseButtonStateEvent.wasPressedFilter
        
        // Switches the state
        override def onMouseButtonState(event: MouseButtonStateEvent) =
		{
			if (contains2D(event.mousePosition)) isOn = !isOn
			None
		}
        
        override def onMouseWheelRotated(event: MouseWheelEvent) =
		{
			transformation = transformation.scaled(1 + event.wheelTurn * 0.2)
			None
		}
        
        private def contains2D(point: Point) = area.contains(transformation.invert(point))
    }
    
    // Creates the handlers
    val gameWorldSize = Size(800, 600)
    
	val drawHandler = DrawableHandler()
	val actorHandler = ActorHandler()
	val mouseStateHandler = MouseButtonStateHandler()
	val mouseMoveHandler = MouseMoveHandler()
	val mouseWheelHandler = MouseWheelHandler()
 
	val handlers = HandlerRelay(drawHandler, actorHandler, mouseStateHandler, mouseMoveHandler, mouseWheelHandler)
	
    // Creates event generators
    val actorLoop = new ActorLoop(actorHandler, 10 to 120)
    
    // Creates test objects
    val area1 = new TestObject((gameWorldSize / 2).toPoint, 128)
    val area2 = new TestObject((gameWorldSize / 2).toPoint + X(128), 64)
    
    handlers ++= (area1, area2)
    
	// Creates the frame
	val canvas = new Canvas(drawHandler, gameWorldSize)
	val frame = new MainFrame(canvas, gameWorldSize, "Mouse Test")
	
	val mouseEventGen = new CanvasMouseEventGenerator(canvas, mouseMoveHandler, mouseStateHandler, mouseWheelHandler)
	actorHandler += mouseEventGen
	
    // Displays the frame
	implicit val context: ExecutionContext = new ThreadPool("Test").executionContext
	
	actorLoop.startAsync()
	canvas.startAutoRefresh()
	
    frame.display()
}