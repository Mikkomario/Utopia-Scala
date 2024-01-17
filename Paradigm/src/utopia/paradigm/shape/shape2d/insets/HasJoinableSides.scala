package utopia.paradigm.shape.shape2d.insets

import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D

/**
* Represents an item which specifies a (length) value for 0-4 2-dimensional sides (top, bottom, left and/or right).
  * This version of this trait supports combining of values
* @author Mikko Hilpinen
* @since 16.1.2024 for v1.5.
  * @tparam L Type of lengths used
  * @tparam C Type of combination results
  * @tparam C2D Two-dimensional combination of the combined lengths
**/
trait HasJoinableSides[L, +C, +C2D] extends HasSides[L]
{
    // ABSTRACT ------------------
    
    /**
      * @return The combined size of these insets, in 2 dimensions
      */
    def total: C2D
    
    /**
      * Combines the length of two sides together
      * @param a First side
      * @param b Second side
      * @return Combined length of those sides
      */
    protected def join(a: L, b: L): C
    
    
	// COMPUTED    ---------------
    
    /**
     * @return Total length of this inset's horizontal components
     */
    def horizontal = totalAlong(X)
    /**
     * @return Total length of this inset's vertical components
     */
    def vertical = totalAlong(Y)
    
    
    // OTHER    ------------------
    
    /**
     * @param axis Target axis
     * @return Total length of these insets along specified axis
     */
    def totalAlong(axis: Axis2D) = sidesAlong(axis).merge(join)
}