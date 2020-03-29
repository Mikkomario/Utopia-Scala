package utopia.genesis.test

import utopia.genesis.shape.shape2D.Circle
import utopia.genesis.util.DepthRange
import utopia.genesis.util.Drawer
import utopia.genesis.event.MouseMoveEvent
import utopia.genesis.handling.mutable.DrawableHandler
import utopia.genesis.handling.{Camera, Drawable, MouseMoveListener}
import utopia.genesis.shape.shape2D.Transformation
import utopia.genesis.shape.shape2D.Point
import utopia.inception.handling.mutable.Handleable

/**
 * This camera magnifies the area under the mouse cursor
 * @author Mikko Hilpinen
 * @since 26.2.2017
 */
class MagnifierCamera(radius: Double) extends Camera[DrawableHandler] with Drawable with MouseMoveListener with Handleable
{
    // ATTRIBUTES    ------------------
    
    override val drawDepth = DepthRange.top
    override val projectionArea = Circle(Point.origin, radius).toShape
    
    private var absoluteTransform = Transformation.scaling(2)
    private var viewTransform = Transformation.scaling(0.75)
    
    
    // IMPLEMENTED PROPERTIES    -----
    
    /**
      * Creates a new drawableHandler with the specified customized drawer
      * @param customizer A function that produces a custom drawer
      * @return A new Drawable handler
      */
    override protected def makeDrawableHandler(customizer: Drawer => Drawer) = DrawableHandler(Vector(), drawDepth, Some(customizer))
    
    override def projectionTransformation = absoluteTransform
    override def viewTransformation = viewTransform
    
    
    // IMPLEMENTED METHODS    --------
    
    // Draws the edges of the projection area
    override def draw(drawer: Drawer) = drawer.withPaint(None).transformed(
            projectionTransformation).draw(projectionArea)
    
    // Moves the camera 'lens' to the mouse position
    override def onMouseMove(event: MouseMoveEvent) = 
    {
        absoluteTransform = absoluteTransform.withPosition(event.mousePosition)
        viewTransform = viewTransform.withPosition(event.mousePosition)
    }
}