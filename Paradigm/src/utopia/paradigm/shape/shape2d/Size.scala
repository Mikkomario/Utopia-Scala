package utopia.paradigm.shape.shape2d

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.generic.model.template
import utopia.flow.generic.model.template.{ModelConvertible, Property, ValueConvertible}
import utopia.flow.operator.{EqualsBy, SignedOrZero}
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.generic.ParadigmDataType.SizeType
import utopia.paradigm.shape.template.{Dimensions, DoubleVector, DoubleVectorFactory, DoubleVectorLike, HasDimensions}
import utopia.paradigm.transform.LinearSizeAdjustable

import java.awt.Dimension
import scala.math.Numeric.DoubleIsFractional
import scala.util.Success

object Size extends SizeFactoryLike[Double, Size] with DoubleVectorFactory[Size] with FromModelFactory[Size]
{
    // ATTRIBUTES   --------------------------
    
    /**
     * A zero size
     */
    override val zero = Size(0, 0)
    
    
    // IMPLEMENTED  --------------------------
    
    override def apply(dimensions: Dimensions[Double]) = new Size(dimensions.withLength(2))
    
    override def from(other: HasDimensions[Double]) = other match {
        case s: Size => s
        case o => apply(o.dimensions)
    }
    
    def apply(model: template.ModelLike[Property]) = Success(
            Size(model("width").getDouble, model("height").getDouble))
    
    
    // OTHER    ------------------------------
    
    /**
     * Converts an awt dimension into size
     */
    @deprecated("Renamed to .apply", "v1.4")
    def of(dimension: Dimension) = apply(dimension)
}

/**
* This class is used for representing 2 dimensional size of an area, doesn't specify position
* @author Mikko Hilpinen
* @since Genesis 20.11.2018
**/
class Size private(override val dimensions: Dimensions[Double])
    extends SizeLike[Double, Size, Size] with DoubleVectorLike[Size] with DoubleVector
        with ValueConvertible with ModelConvertible
        with SignedOrZero[Size] with Sized[Size] with LinearSizeAdjustable[Size] with EqualsBy
{
    // COMPUTED    --------------------------
    
    /**
     * A vector representation of this size
     */
    def toVector = toVector2D
    
    
    // IMPLEMENTED    -----------------------
    
    override implicit def n: Fractional[Double] = DoubleIsFractional
    
    override def self = this
    override def size = this
    
    override def width = x
    override def height = y
    
    override def zero = Size.zero
    
    override protected def factory = Size
    
    override protected def equalsProperties = dimensions
    
    override def toValue = new Value(Some(this), SizeType)
    def toModel = Model.from("width" -> width, "height" -> height)
    
    
    // OTHER    -----------------------------
    
    /**
     * A copy of this size with specified length along the target axis
     */
    @deprecated("Replaced with withLength(Vector1D, Boolean)", "v1.1")
    def withLength(l: Double, axis: Axis2D) = axis match {
        case X => withWidth(l)
        case Y => withHeight(l)
    }
    
    /**
      * @param position New position for this size (default = (0, 0))
      * @return A set of bounds with this size and specified position
      */
    def toBounds(position: Point = Point.origin) = Bounds(position, this)
    
    /**
      * @param centerPosition Position for the resulting bounds center point
      * @return A new set of bounds which are centered on the specified point and have this size
      */
    def centeredAt(centerPosition: Point) = Bounds(centerPosition - this / 2, this)
    
    /**
     * @param another Another size
      * @param preserveShape Whether the width/height -ratio of this size should be preserved when altering size
      *                      (default = false)
     * @return A copy of this size that fits into specified size. If this size already fits, returns this.
     */
    @deprecated("Replaced with fittingWithin(Size, Boolean) and croppedToFit(Size). Notice also the different functionality.", "v1.1")
    def fittedInto(another: Size, preserveShape: Boolean = false) = {
        if (width <= another.width) {
            if (height <= another.height)
                this
            else if (preserveShape)
                this * (another.height / height)
            else
                withHeight(another.height)
        }
        else if (height <= another.height) {
            if (preserveShape)
                this * (another.width / width)
            else
                withWidth(another.width)
        }
        else if (preserveShape)
            this * (another / this).minDimension
        else
            another
    }
}