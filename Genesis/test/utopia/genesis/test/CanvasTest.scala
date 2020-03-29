package utopia.genesis.test

import java.awt.Color

import utopia.flow.async.ThreadPool
import utopia.genesis.handling.Drawable
import utopia.genesis.handling.immutable.DrawableHandler
import utopia.genesis.view.{Canvas, MainFrame, RepaintLoop}
import utopia.genesis.util.Drawer
import utopia.genesis.shape.shape2D.{Bounds, Circle, Point, ShapeConvertible, Size}
import utopia.inception.handling.HandlerType

import scala.concurrent.ExecutionContext

/**
 * This test tests the basic canvas drawing
 * @author Mikko Hilpinen
 * @since 29.12.2016
 */
object CanvasTest extends App
{
    private class TestDrawable(val shape: ShapeConvertible, override val drawDepth: Int) extends Drawable
    {
		override def parent = None
	
		override def allowsHandlingFrom(handlerType: HandlerType) = true
	
		override def draw(drawer: Drawer) = drawer.withEdgePaint(Color.RED).draw(shape)
    }
	
	val gameWorldSize = Size(800, 600)
	
    private val drawables = Vector(new TestDrawable(Circle((gameWorldSize / 2).toPoint, 96), 0),
		new TestDrawable(Circle((gameWorldSize / 2).toPoint, 96), 0),
		new TestDrawable(Circle((gameWorldSize / 2).toPoint, 16), -100),
		new TestDrawable(Circle(Point.origin, gameWorldSize.width), 100),
		new TestDrawable(Bounds((gameWorldSize * 0.2).toPoint, gameWorldSize * 0.6), 50)
	)
	
	val handler = DrawableHandler(drawables)
	
	val canvas = new Canvas(handler, gameWorldSize)
	val frame = new MainFrame(canvas, gameWorldSize, "CanvastTest")
	
	implicit val context: ExecutionContext = new ThreadPool("Test").executionContext
	
	val repaintLoop = new RepaintLoop(canvas)
	repaintLoop.registerToStopOnceJVMCloses()
	repaintLoop.startAsync()
    frame.display()
}