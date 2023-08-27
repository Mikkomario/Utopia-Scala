package utopia.paradigm.shape.shape2d.vector.size

import utopia.paradigm.shape.template.vector.NumericVectorFactory

/**
  * Common trait for factories used for constructing 2-dimensional sizes
  * @author Mikko Hilpinen
  * @since 25.8.2023, v1.4
  * @tparam D Type of dimensions used in the resulting sizes
  * @tparam S Type of the resulting sizes
  */
trait SizeFactoryLike[D, +S] extends NumericVectorFactory[D, S]
{
    // OTHER    ------------------------------
    
    /**
      * Creates a square shaped size
      * @param side Length of a single side
      * @return A new size with equal width and height
      */
    def square(side: D) = apply(side, side)
    
    /**
     * Converts an awt dimension into size
     */
    def apply(dimension: java.awt.Dimension): S = apply(n.fromInt(dimension.width), n.fromInt(dimension.height))
    /**
     * Converts awt insets into a size
     */
    def of(insets: java.awt.Insets): S =
        apply(n.fromInt(insets.left + insets.right), n.fromInt(insets.top + insets.bottom))
}

