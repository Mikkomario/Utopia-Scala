package utopia.paradigm.shape.shape2d

import java.awt.Shape

/**
 * Classes extending this trait can be converted to shape objects
 * @author Mikko Hilpinen
 * @since Genesis 23.1.2017
 */
trait ShapeConvertible
{
    /**
     * Converts this instance to a shape
     */
    def toShape: Shape
    
    // Also add shape intersections and additions, etc.
}