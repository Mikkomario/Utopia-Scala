package utopia.genesis.test

import utopia.genesis.util.{DefaultSetup, DepthRange, Drawer}
import utopia.genesis.shape.Vector3D
import java.awt.Color

import utopia.flow.async.ThreadPool
import utopia.genesis.event.MouseMoveEvent
import utopia.genesis.handling.{Drawable, MouseMoveListener}
import utopia.genesis.shape.shape2D.{Circle, Point, ShapeConvertible, Size}
import utopia.inception.handling.immutable.Handleable

import scala.concurrent.ExecutionContext

/**
 * This test tests the drawer's clipping functionality
 * @author Mikko Hilpinen
 * @since 25.2.2017
 */
object ClippingTest extends App
{
    class HiddenShapeDrawer(val shapes: Iterable[ShapeConvertible]) extends Drawable with MouseMoveListener with Handleable
    {
        override val drawDepth = DepthRange.foreground
        
        private val clipRadius = 64
        private var clip = Circle(Point.origin, clipRadius)
        
        def draw(drawer: Drawer) = 
        {
            drawer.withEdgePaint(None).draw(clip)
            
            val clipped = drawer.withPaint(Some(Color.RED)).clippedTo(clip)
            shapes.foreach(clipped.draw)
        }
        
        def onMouseMove(event: MouseMoveEvent) = clip = Circle(event.mousePosition, clipRadius)
    }
	
	// Sets up the program
	val worldSize = Size(800, 600)
	
	val setup = new DefaultSetup(worldSize, "Clipping Test")
	
    setup.registerObject(new GridDrawer(worldSize, Size(64, 64)))
    setup.registerObject(new HiddenShapeDrawer(Vector(
            Circle((worldSize / 2).toPoint, 32), Circle((worldSize * 0.7).toPoint, 64),
            Circle((worldSize.xProjection + Vector3D(- 64, 64)).toPoint, 32))))
    
	// Starts the program
	implicit val context: ExecutionContext = new ThreadPool("Test").executionContext
	setup.start()
}