package utopia.genesis.test

import utopia.flow.test.TestContext._
import utopia.genesis.event.MouseMoveEvent
import utopia.genesis.graphics.{DrawSettings, Drawer}
import utopia.genesis.handling.{Drawable, MouseMoveListener}
import utopia.genesis.util.{DefaultSetup, DepthRange}
import utopia.inception.handling.immutable.Handleable
import utopia.paradigm.shape.shape2d.{Circle, Point, ShapeConvertible, Size}
import utopia.paradigm.shape.shape3d.Vector3D

import java.awt.Color

/**
 * This test tests the drawer's clipping functionality
 * @author Mikko Hilpinen
 * @since 25.2.2017
 */
object ClippingTest extends App
{
    class HiddenShapeDrawer(val shapes: Iterable[ShapeConvertible]) extends Drawable with MouseMoveListener with Handleable
    {
        private val bgDs = DrawSettings.onlyFill(Color.white)
        private val dotDs = DrawSettings.onlyFill(Color.red)
        
        override val drawDepth = DepthRange.foreground
        
        private val clipRadius = 64
        private var clip = Circle(Point.origin, clipRadius).toPolygon(8)
        
        def draw(drawer: Drawer) = {
            drawer.draw(clip)(bgDs)
            val clipped = drawer.withClip(clip)
            shapes.foreach { clipped.draw(_)(dotDs) }
        }
        
        def onMouseMove(event: MouseMoveEvent) = clip = Circle(event.mousePosition, clipRadius).toPolygon(8)
    }
	
	// Sets up the program
	val worldSize = Size(800, 600)
	
	val setup = new DefaultSetup(worldSize, "Clipping Test")
	
    setup.registerObject(new GridDrawer(worldSize, Size(64, 64)))
    setup.registerObject(new HiddenShapeDrawer(Vector(
            Circle((worldSize / 2).toPoint, 32), Circle((worldSize * 0.7).toPoint, 64),
            Circle((worldSize.xProjection + Vector3D(- 64, 64)).toPoint, 32))))
    
	// Starts the program
	setup.start()
}