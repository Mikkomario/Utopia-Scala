package utopia.paradigm.shape.shape2d

import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Direction2D
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions

import scala.collection.immutable.HashMap

object Insets extends InsetsFactory[Double, Insets]
{
    // ATTRIBUTES   ------------------------
    
    /**
      * A set of insets where each side is 0
      */
    val zero = new Insets(HashMap())
    
    
    // OTHER    ----------------------------
    
    /**
     * Converts an awt insets into insets
     */
    def of(insets: java.awt.Insets) = Insets(insets.left, insets.right, insets.top, insets.bottom)
    
    /**
      * Creates a symmetric set of insets where top = bottom and left = right
      * @param size the total size of the insets
      */
    def symmetric(size: HasDoubleDimensions): Insets = symmetric(size(X) / 2, size(Y) / 2)
}

/**
* Insets can be used for describing an area around a component (top, bottom, left and right)
* @author Mikko Hilpinen
* @since Genesis 25.3.2019
**/
case class Insets(amounts: Map[Direction2D, Double]) extends InsetsLike[Double, Size, Insets]
{
    // IMPLEMENTED  --------------
    
    override def self = this
    
    override protected def withAmounts(newAmounts: Map[Direction2D, Double]) = Insets(newAmounts)
    
    override protected def zeroLength = 0.0
    
    override protected def plus(first: Double, second: Double) = first + second
    
    override protected def multiply(a: Double, multiplier: Double) = a * multiplier
    
    override protected def make2D(horizontal: Double, vertical: Double) = Size(horizontal, vertical)
    
    
	// COMPUTED    ---------------
    
    /**
     * Converts this insets instance to awt equivalent
     */
    def toAwt = new java.awt.Insets(top.toInt, left.toInt, bottom.toInt, right.toInt)
    
    /**
     * The top left position inside these insets
     */
    def toPoint = Point(left, top)
    
    /**
      * @return A non-negative version of these insets
      */
    def positive = Insets(amounts.map { case (k, v) => k -> (v max 0) })
}