package utopia.paradigm.shape.shape2d.insets

import utopia.paradigm.enumeration.{Axis2D, Direction2D}

/**
  * Common trait for factories which produce insets
  * @tparam L Type of inset lengths used
  * @tparam I Type of insets produced by this factory
  */
@deprecated("Please use SidedFactory instead", "v1.5")
trait InsetsFactory[-L, +I] extends SidesFactory[L, I]
{
    // ABSTRACT ---------------------------
    
    /**
      * Creates a new set of insets
      * @param amounts Lengths of each side in these insets
      * @return A set of insets with specified lengths
      */
    def withAmounts(amounts: Map[Direction2D, L]): I
    
    
    // IMPLEMENTED  -----------------------
    
    override def withSides(sides: Map[Direction2D, L]): I = withAmounts(sides)
}

/**
* Insets can be used for describing an area around a component (top, bottom, left and right)
* @author Mikko Hilpinen
* @since Genesis 25.3.2019
  * @tparam L Type of lengths used by these insets
  * @tparam S Type of 2-dimensional sizes acquired by combining lengths
  * @tparam Repr Concrete implementation class of these insets
**/
@deprecated("Please extend ScalableSidesLike instead", "v1.5")
trait InsetsLike[L, +S, +Repr] extends ScalableSidesLike[L, S, Repr]
{
    // ABSTRACT ------------------
    
    /**
      * Combines two lengths into a size
      * @param horizontal Horizontal length
      * @param vertical Vertical length
      * @return A size
      */
    protected def make2D(horizontal: L, vertical: L): S
    
    
	// COMPUTED    ---------------
    
    @deprecated("Please use .sides instead", "v1.5")
    def amounts: Map[Direction2D, L] = sides
    /**
      * @return Lengths of all the sides in these insets
      */
    @deprecated("Please use .lengthsIterator instead", "v1.5")
    def lengths = lengthsIterator.toVector
    
    
    // IMPLEMENTED  --------------
    
    def total = make2D(horizontal, vertical)
    
    
    // OTHER    ------------------
    
    /**
     * @param axis Target axis
     * @return Total length of these insets along specified axis
     */
    @deprecated("Please use .totalAlong(Axis2D) instead", "v1.5")
    def along(axis: Axis2D) = totalAlong(axis)
}