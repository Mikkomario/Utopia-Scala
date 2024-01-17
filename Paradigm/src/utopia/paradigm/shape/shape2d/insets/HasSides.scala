package utopia.paradigm.shape.shape2d.insets

import utopia.flow.collection.immutable.Pair
import utopia.paradigm.enumeration.Direction2D.{Down, Up}
import utopia.paradigm.enumeration.{Axis2D, Direction2D}
import utopia.paradigm.shape.template.{Dimensions, HasDimensions}

/**
* Represents an item which specifies a (length) value for 0-4 2-dimensional sides (top, bottom, left and/or right)
* @author Mikko Hilpinen
* @since 16.1.2024 for v1.5.
  * @tparam L Type of lengths used
**/
trait HasSides[+L] extends HasDimensions[Pair[L]]
{
    // ABSTRACT ------------------
    
    /**
      * @return Lengths of each side in this item
      */
    def sides: Map[Direction2D, L]
    
    /**
      * @return A zero length item
      */
    protected def zeroLength: L
    
    
	// COMPUTED    ---------------
    
    /**
     * @return Insets for the left side
     */
    def left = apply(Direction2D.Left)
    /**
     * @return Insets for the right side
     */
    def right = apply(Direction2D.Right)
    /**
     * @return Insets for the top side
     */
    def top = apply(Up)
    /**
     * @return Insets for the bottom side
     */
    def bottom = apply(Down)
    
    /**
      * @return An iterator that returns the lengths of all the sides in these insets.
      *         Might not contain all zero lengths.
      */
    def lengthsIterator = sides.valuesIterator
    
    
    // IMPLEMENTED  --------------
    
    override def dimensions =
        Dimensions(Pair(zeroLength, zeroLength))(Pair(left, right), Pair(top, bottom))
    
    override def toString = s"[${sides.map { case (d, l) => s"$d:$l" }.mkString(", ")}]"
    
    
    // OTHER    ------------------
    
    /**
      * @param direction Target direction
      * @return The length of these insets towards that direction
      */
    def apply(direction: Direction2D) = sides.getOrElse(direction, zeroLength)
    
    /**
      * @param axis Target axis
      * @return The two sides of insets along that axis as a Pair
      *         (i.e. for x-axis, returns left -> right and for y-axis top -> bottom)
      */
    def sidesAlong(axis: Axis2D) = axis.directions.map(apply)
}