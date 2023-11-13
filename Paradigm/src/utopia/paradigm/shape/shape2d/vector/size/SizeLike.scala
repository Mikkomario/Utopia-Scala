package utopia.paradigm.shape.shape2d.vector.size

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.operator.sign.SignOrZero.Neutral
import utopia.flow.operator.sign.{SignOrZero, SignedOrZero}
import utopia.paradigm.shape.template.vector.{NumericVectorFactory, NumericVectorLike}
import utopia.paradigm.transform.{Adjustment, SizeAdjustable}

import java.awt.Dimension
import scala.math.Ordered.orderingToOrdered

/**
* Common trait for classes that represent 2D sizes
* @author Mikko Hilpinen
* @since 25.8.2023, v1.4
  * @tparam D Type of dimensions (width & height) used
  * @tparam Repr Type of this size class
  * @tparam FromDoubles Type of this size class when using double precision
**/
trait SizeLike[D, Repr <: NumericVectorLike[D, Repr, _], +FromDoubles]
    extends NumericVectorLike[D, Repr, FromDoubles] with SignedOrZero[Repr]
        with SizeAdjustable[FromDoubles] with SizedLike[D, Repr, Repr]
{
    // COMPUTED    --------------------------
    
    /**
     * The area of this size (width * height)
     */
    def area = dimensions.product
    
    /**
     * An awt representation of this size
     */
	def toDimension = new Dimension(n.toInt(width), n.toInt(height))
    
    /**
      * @return This size as a square shape (equal width and height).
      *         Decreases the larger side to match the smaller side.
      */
    def fitToSquare = factory(Pair.twice(minDimension))
    /**
      * @return This size as a square shape (equal width and height).
      *         Increases the smaller side to match the larger side.
      */
    def fillToSquare = factory(Pair.twice(maxDimension))
    
    
    // IMPLEMENTED    -----------------------
    
    /**
      * @return Width of this size
      */
    override def width = x
    /**
      * @return Height of this size
      */
    override def height = y
    
    override protected def sizeFactory: NumericVectorFactory[D, Repr] = factory
    
    /**
      * Sign of this size.
      * Positive only if both width and height are larger than 0.
      * Neutral if either of them is 0,
      * Negative otherwise.
      */
    override def sign: SignOrZero =
        if (dimensions.exists { _ == n.zero }) Neutral else if (dimensions.exists { _ < n.zero }) Negative else Positive
    
    override def toString = s"$width x $height"
    
    override def withSize(size: Repr) = size
    
    override protected def adjustedBy(impact: Int)(implicit adjustment: Adjustment): FromDoubles =
        scaledBy(adjustment(impact))
    
    
    // OTHER    -----------------------------
    
    /**
     * A copy of this size with specified width
     */
    def withWidth(w: D) = factory(w, height)
    /**
     * A copy of this size with specified height
     */
    def withHeight(h: D) = factory(width, h)
}