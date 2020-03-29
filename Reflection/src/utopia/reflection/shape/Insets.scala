package utopia.reflection.shape

import utopia.reflection.shape.LengthExtensions._
import utopia.genesis.shape.shape2D.{Direction2D, Point, Size}

import scala.collection.immutable.HashMap

object Insets extends InsetsFactory[Double, Size, Insets, Insets]
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
    def symmetric(size: Size): Insets = symmetric(size.width / 2, size.height / 2)
}

/**
* Insets can be used for describing an area around a component (top, bottom, left and right)
* @author Mikko Hilpinen
* @since 25.3.2019
**/
case class Insets(amounts: Map[Direction2D, Double]) extends InsetsLike[Double, Size, Insets]
{
    // IMPLEMENTED  --------------
    
    override protected def makeCopy(newAmounts: Map[Direction2D, Double]) = Insets(newAmounts)
    
    override protected def makeZero = 0.0
    
    override protected def combine(first: Double, second: Double) = first + second
    
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
    
    /**
      * @return A stack-compatible copy of these insets that supports any other value but prefers these
      */
    def any = toStackInsets { _.any }
    
    /**
      * @return A stack-compatible copy of these insets that only allows these exact insets
      */
    def fixed = toStackInsets { _.fixed }
    
    /**
      * @return A stack-compatible copy of these insets that allows these or smaller insets
      */
    def downscaling = toStackInsets { _.downscaling }
    
    /**
      * @return A stack-compatible copy of these insets that allows these or larger insets
      */
    def upscaling = toStackInsets { _.upscaling }
    
    /**
      * @param min Minimum set of insets
      * @return A set of stack insets that allows values below these insets, down to specified insets
      */
    def downTo(min: Insets) = toStackInsetsWith(min) { (opt, min) => opt.downTo(min) }
    
    /**
      * @param max Maximum set of insets
      * @return A set of stack insets that allows values above these insets, up to specified insets
      */
    def upTo(max: Insets) = toStackInsetsWith(max) { (opt, max) => opt.upTo(max) }
    
    
    // OTHER    ---------------------
    
    /**
      * Converts these insets to stack insets by using the specified function
      * @param f A function for converting a static length to a stack length
      * @return Stack insets based on these insets
      */
    def toStackInsets(f: Double => StackLength) = StackInsets(amounts.map { case (d, l) => d -> f(l) })
    
    /**
      * Creates a set of stack insets by combining these insets with another set of insets and using the specified merge function
      * @param other Another set of insets
      * @param f Function for producing stack lengths
      * @return A new set of stack insets
      */
    def toStackInsetsWith(other: Insets)(f: (Double, Double) => StackLength) = StackInsets(
        (amounts.keySet ++ other.amounts.keySet).map { d => d -> f(apply(d), other(d)) }.toMap)
}