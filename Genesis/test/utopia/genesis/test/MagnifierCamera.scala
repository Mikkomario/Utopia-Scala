package utopia.genesis.test

import utopia.genesis.event.MouseMoveEvent
import utopia.genesis.graphics.{DrawSettings, Drawer3, StrokeSettings}
import utopia.genesis.handling.mutable.DrawableHandler2
import utopia.genesis.handling.{Camera2, Drawable2, MouseMoveListener}
import utopia.genesis.util.DepthRange
import utopia.inception.handling.mutable.Handleable
import utopia.paradigm.shape.shape2d.{Circle, Point}
import utopia.paradigm.transform.LinearTransformation

/**
 * This camera magnifies the area under the mouse cursor
 * @author Mikko Hilpinen
 * @since 26.2.2017
 */
class MagnifierCamera(radius: Double, override val drawDepth: Int = DepthRange.top)
    extends Camera2[DrawableHandler2](customize => DrawableHandler2(Vector(), drawDepth, Some(customize)))
        with Drawable2 with MouseMoveListener with Handleable
{
    // ATTRIBUTES    ------------------
    
    private implicit val ds: DrawSettings = StrokeSettings.default
    
    private val projectionCircle = Circle(Point.origin, radius)
    override val projectionArea = projectionCircle.toPolygon(32)
    
    private var absoluteTransform = LinearTransformation.scaling(2).toAffineTransformation
    private var viewTransform = LinearTransformation.scaling(0.75).toAffineTransformation
    
    
    // IMPLEMENTED PROPERTIES    -----
    
    override def projectionTransformation = absoluteTransform.toMatrix
    override def viewTransformation = viewTransform.toMatrix
    
    
    // IMPLEMENTED METHODS    --------
    
    // Draws the edges of the projection area
    override def draw(drawer: Drawer3) = (drawer * projectionTransformation).draw(projectionCircle)
    
    // Moves the camera 'lens' to the mouse position
    override def onMouseMove(event: MouseMoveEvent) = {
        absoluteTransform = absoluteTransform.withPosition(event.mousePosition)
        viewTransform = viewTransform.withPosition(event.mousePosition)
    }
}