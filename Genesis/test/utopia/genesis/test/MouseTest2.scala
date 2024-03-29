package utopia.genesis.test

import utopia.flow.test.TestContext._
import utopia.paradigm.color.Color
import utopia.genesis.event.{MouseButtonStateEvent, MouseEvent, MouseMoveEvent, MouseWheelEvent}
import utopia.genesis.graphics.{DrawSettings, Drawer, StrokeSettings}
import utopia.genesis.handling.mutable.{ActorHandler, DrawableHandler}
import utopia.genesis.handling.{ActorLoop, Drawable, MouseButtonStateListener, MouseMoveListener, MouseWheelListener}
import utopia.paradigm.enumeration.Axis._
import utopia.paradigm.transform.{AffineTransformation, LinearTransformation}
import utopia.genesis.view.{Canvas, CanvasMouseEventGenerator, MainFrame}
import utopia.inception.handling.immutable.Handleable
import utopia.inception.handling.mutable.HandlerRelay
import utopia.paradigm.shape.shape2d.area.Circle
import utopia.paradigm.shape.shape2d.line.Line
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

/**
 * This is a visual test for mouse event features. In the test, the two lines should point to the
 * mouse cursor whenever any mouse button is down. The two circles should change colour when the
 * mouse cursor hovers over the shapes. When the shapes are clicked, their colours should change.
 * Rotating the mouse wheel should increase / decrease the circle radius.
 * @author Mikko Hilpinen
 * @since 4.2.2017
 */
object MouseTest2 extends App
{
    class TestObject(position: Point, radius: Double) extends Drawable with
            MouseMoveListener with MouseButtonStateListener with MouseWheelListener with Handleable
	{
        private val area = Circle(Point.origin, radius)
        
        private var lastMousePosition = Point.origin
        private var mouseOver = false
        private var isOn = false
        private var transformation = AffineTransformation.translation(position.toVector)
        
        override def draw(drawer: Drawer) =
        {
	        val color = if (isOn) Color.blue else if (mouseOver) Color.cyan else Color.gray(0.5)
	        (drawer * transformation).draw(area)(DrawSettings.onlyFill(color.withAlpha(0.7)))
            drawer.draw(Line(transformation.position, lastMousePosition))(
	            StrokeSettings.apply(Color.textBlack, strokeWidth = 3))
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
			if (contains2D(event.mousePosition))
				isOn = !isOn
			None
		}
        
        override def onMouseWheelRotated(event: MouseWheelEvent) =
		{
			transformation = transformation + LinearTransformation.scaling(1 + event.wheelTurn * 0.2)
			None
		}
        
        private def contains2D(point: Point) = transformation.invert(point).exists(area.contains)
    }
    
    // Creates the handlers
    val gameWorldSize = Size(800, 600)
    
	val drawHandler = DrawableHandler()
	val actorHandler = ActorHandler()
	
	val canvas = new Canvas(drawHandler, gameWorldSize)
	val mouseEventGen = new CanvasMouseEventGenerator(canvas)
	
	val handlers = HandlerRelay(drawHandler, actorHandler, mouseEventGen.buttonHandler, mouseEventGen.moveHandler,
		mouseEventGen.wheelHandler)
	
    // Creates event generators
    val actorLoop = new ActorLoop(actorHandler, 10 to 120)
    
    // Creates test objects
    val area1 = new TestObject((gameWorldSize / 2).toPoint, 128)
    val area2 = new TestObject((gameWorldSize / 2).toPoint + X(128), 64)
    
    handlers ++= Vector(area1, area2)
    
	// Creates the frame
	val frame = new MainFrame(canvas, gameWorldSize, "Mouse Test")
	
	actorHandler += mouseEventGen
	
    // Displays the frame
	actorLoop.runAsync()
	canvas.startAutoRefresh()
	
    frame.display()
}