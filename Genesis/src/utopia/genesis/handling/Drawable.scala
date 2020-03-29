package utopia.genesis.handling

import utopia.genesis.util.{DepthRange, Drawer}
import utopia.inception.handling.Handleable

/**
 * Drawable instances can be drawn on a canvas using a graphics object and support depth sorting
 * @author Mikko Hilpinen
 * @since 28.12.2016
 */
trait Drawable extends Handleable
{
    /**
      * @return Whether this drawable is visible (allowing drawing)
      */
    def isVisible = allowsHandlingFrom(DrawableHandlerType)
    
    /**
     * Draws the drawable instance using a specific graphics object. The graphics transformations
     * should always be set back to original after drawing
     * @param drawer The drawer object used for drawing this instance
     */
    def draw(drawer: Drawer)
    
    /**
     * The drawing depth of the drawable instance. The higher the depth, the 'deeper' it will be
     * drawn. Instances with less depth are drawn atop of those with higher depth. By default, all
     * objects are drawn to the 0 layer
     */
    def drawDepth = DepthRange.default
}
