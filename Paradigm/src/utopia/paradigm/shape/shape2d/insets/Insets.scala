package utopia.paradigm.shape.shape2d.insets

import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Direction2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions

import scala.collection.immutable.HashMap

object Insets extends SidesFactory[Double, Insets]
{
    // TYPES    ----------------------------
    
    /**
      * A builder that generates insets
      */
    type InsetsBuilder = SidedBuilder[Double, Insets]
    
    
    // ATTRIBUTES   ------------------------
    
    /**
      * A set of insets where each side is 0
      */
    val zero = new Insets(Map())
    
    
    // IMPLEMENTED  ------------------------
    
    override def withSides(sides: Map[Direction2D, Double]): Insets = apply(sides)
    
    
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
    
    @deprecated("Please use .withSides(Map) or .apply(Map) instead", "v1.5")
    def withAmounts(sides: Map[Direction2D, Double]): Insets = apply(sides)
}

/**
* Insets can be used for describing an area around a component (top, bottom, left and right)
* @author Mikko Hilpinen
* @since Genesis 25.3.2019
**/
case class Insets(sides: Map[Direction2D, Double]) extends ScalableSidesLike[Double, Size, Insets]
{
    // ATTRIBUTES   --------------
    
    override protected val zeroLength = 0.0
    lazy override val dimensions = super.dimensions
    
    override lazy val total: Size = Size(totalAlong(X), totalAlong(Y))
    
    
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
    def positive = Insets(sides.filter { _._2 > 0 })
    /**
      * @return Copy of these insets where every value is rounded to the nearest integer
      */
    def round = copy(sides.view.mapValues { _.round.toDouble }.toMap)
    
    @deprecated("Please use .sides instead", "v1.5")
    def amounts = sides
    
    
    // IMPLEMENTED  --------------
    
    override def self = this
    
    override protected def join(a: Double, b: Double): Double = a + b
    override protected def subtract(from: Double, amount: Double): Double = from - amount
    override protected def multiply(a: Double, multiplier: Double) = a * multiplier
    
    override protected def withSides(sides: Map[Direction2D, Double]): Insets = Insets(sides)
}