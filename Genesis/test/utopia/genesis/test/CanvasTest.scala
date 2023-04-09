package utopia.genesis.test

import utopia.flow.test.TestContext._
import utopia.genesis.graphics.{DrawSettings, Drawer, StrokeSettings}
import utopia.genesis.handling.Drawable
import utopia.genesis.handling.mutable.DrawableHandler
import utopia.genesis.view.{Canvas, MainFrame, RepaintLoop}
import utopia.inception.handling.HandlerType
import utopia.paradigm.shape.shape2d.{Bounds, Circle, Point, ShapeConvertible, Size}

import java.awt.Color

/**
 * This test tests the basic canvas drawing
 * @author Mikko Hilpinen
 * @since 29.12.2016
 */
object CanvasTest extends App
{
    private class TestDrawable(val shape: ShapeConvertible, override val drawDepth: Int) extends Drawable
    {
	    private implicit val ds: DrawSettings = StrokeSettings(Color.red)
	    
		override def allowsHandlingFrom(handlerType: HandlerType) = true
	
		override def draw(drawer: Drawer) = drawer.draw(shape)
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
	
	val repaintLoop = new RepaintLoop(canvas)
	repaintLoop.runAsync()
    frame.display()
}