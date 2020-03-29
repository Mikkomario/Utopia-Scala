package utopia.genesis.handling

import java.awt.Shape

import utopia.genesis.shape.shape2D.Transformation
import utopia.genesis.util.Drawer

/**
 * Cameras are used for viewing the game world from a different angle
 * (relative view) and projecting that view to a certain shape in the 'absolute' super view.
 */
trait Camera[H <: DrawableHandler]
{
    // ATTRIBUTES    ------------------------
    
    /**
     * The handler for all the content that may be displayed in the camera's view
     */
    lazy val drawHandler = makeDrawableHandler(customDrawer)
    
    
    // ABSTRACT METHODS    ------------------
    
    /**
      * Creates a new drawableHandler with the specified customized drawer
      * @param customizer A function that produces a custom drawer
      * @return A new Drawable handler
      */
    protected def makeDrawableHandler(customizer: Drawer => Drawer): H
    
    /**
     * This transformation determines the camera's transformation in the 'view world'. For example,
     * if this transformation has 2x scaling, the camera projects twice as much game world to the
     * same projection area.
     */
    def viewTransformation: Transformation
    /**
     * This transformation determines how the camera projects the 'view world' into the 'absolute
     * world'. For example, the transformation determines the position of the projection's origin
     * ((0, 0) of the projection area). If this transformation has 2x scaling, the projected
     * 'camera display' will be twice as large while still drawing the same 'view world'
     * information.<br>
     * Usually you only need to determine the drawing origin (translation)
     */
    def projectionTransformation: Transformation
    /**
     * This determines both the size and shape of the camera's projection. You can draw a circular
     * area by using a circle and a rectangular area by using a rectangle, etc. The shape's center
     * should always be at the (0, 0) coordinates since that's what the camera is rotated and scaled
     * around.
     * <br> You can instead move both the projected and viewed area by using the projection- and
     * view transformations respectively.
     */
    def projectionArea: Shape
    
    
    // OTHER METHODS    -------------------
    
    // Transforms and clips the drawer
    private def customDrawer(drawer: Drawer) = drawer.transformed(projectionTransformation)
        .clippedTo(projectionArea).transformed(-viewTransformation)
}
