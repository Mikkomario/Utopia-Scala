package utopia.paradigm.shape.shape2d.insets

import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

/**
* Insets can be used for describing an area around a component (top, bottom, left and right)
* @author Mikko Hilpinen
* @since Genesis 25.3.2019
  * @tparam Repr Concrete implementation class of these insets
**/
trait InsetsLike[+Repr] extends ScalableSidesLike[Double, Size, Repr]
{
	// COMPUTED    ---------------
	
	/**
	 * The top left position inside these insets
	 */
	def toPoint = Point(left, top)
	
	/**
	 * @return A non-negative version of these insets
	 */
	def positive = filter { (_, len) => len > 0 }
	/**
	 * @return Copy of these insets where every value is rounded to the nearest integer
	 */
	def round = mapDefined { _.round.toDouble }
    
    
    // IMPLEMENTED  --------------
	
	override protected def zeroLength = 0.0
	
    override def total = Size(horizontal, vertical)
	
	override protected def join(a: Double, b: Double): Double = a + b
	override protected def subtract(from: Double, amount: Double): Double = from - amount
	override protected def multiply(a: Double, multiplier: Double) = a * multiplier
}