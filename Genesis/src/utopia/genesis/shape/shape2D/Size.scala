package utopia.genesis.shape.shape2D

import utopia.genesis.util.Extensions._
import utopia.flow.generic.ValueConversions._
import java.awt.Dimension

import utopia.flow.generic.ValueConvertible
import utopia.flow.datastructure.immutable.Value
import utopia.genesis.generic.SizeType
import utopia.flow.generic.ModelConvertible
import utopia.flow.datastructure.immutable.Model

import scala.collection.immutable.HashMap
import utopia.flow.generic.FromModelFactory
import utopia.flow.datastructure.template.Property
import utopia.genesis.util.ApproximatelyEquatable
import utopia.genesis.shape.{Axis2D, Vector3D, VectorLike}
import utopia.genesis.shape.Axis._
import java.awt.Insets

import scala.util.Success

object Size extends FromModelFactory[Size]
{
    /**
     * A zero size
     */
    val zero = Size(0, 0)
    
    def apply(model: utopia.flow.datastructure.template.Model[Property]) = Success(
            Size(model("width").getDouble, model("height").getDouble))
    
    /**
      * Creates a new size
      * @param length Length
      * @param breadth Breadth
      * @param axis Axis that determines which side length is for
      * @return A new size
      */
    def apply(length: Double, breadth: Double, axis: Axis2D): Size = axis match
    {
        case X => Size(length, breadth)
        case Y => Size(breadth, length)
    }
    
    /**
     * Converts an awt dimension into size
     */
    def of(dimension: Dimension) = Size(dimension.width, dimension.height)
    
    /**
     * Converts awt insets into a size
     */
    def of(insets: Insets) = Size(insets.left + insets.right, insets.top + insets.bottom)
    
    /**
      * Creates a square shaped size
      * @param side Length of a single side
      * @return A new size with equal width and height
      */
    def square(side: Double) = Size(side, side)
}

/**
* This class is used for representing 2 dimensional size of an area, doesn't specify position
* @author Mikko Hilpinen
* @since 20.11.2018
**/
case class Size(width: Double, height: Double) extends VectorLike[Size] with ApproximatelyEquatable[Size]
        with ValueConvertible with ModelConvertible
{
    // COMPUTED    --------------------------
    
    /**
     * @return whether this size is positive (not zero or smaller)
     */
    def isPositive = width > 0 && height > 0
    
    /**
     * @return Whether this size is zero or smaller
     */
    def isNegative = !isPositive
    
    /**
     * @return Whether this size has zero area (width and/or height is zero). Zero sizes cannot be divided with
     */
    override def isZero = width == 0 || height == 0
    
    /**
     * @return Whether this size has a non-zero area (positive or negative)
     */
    def nonZero = !isZero
    
    /**
     * The area of this size (width * height)
     */
    def area = width * height
    
    /**
     * A vector representation of this size
     */
    def toVector = Vector3D(width, height)
    
    /**
      * A point representation of this size
      */
    def toPoint = Point(width, height)
    
    /**
     * An awt representation of this size
     */
	def toDimension = new Dimension(width.toInt, height.toInt)
    
    
    // IMPLEMENTED    -----------------------
    
    override protected def repr = this
    
    lazy val dimensions = Vector(width, height)
    
    override def buildCopy(dimensions: Vector[Double]) =
    {
        if (dimensions.size >= 2)
            Size(dimensions(0), dimensions(1))
        else if (dimensions.isEmpty)
            Size.zero
        else
            Size(dimensions(0), 0)
    }
    
    def toValue = new Value(Some(this), SizeType)
    
    def toModel = Model.fromMap(HashMap("width" -> width, "height" -> height))
    
    def ~==(other: Size) = (width ~== other.width) && (height ~== other.height)
    
    override def toString = s"$width x $height"
    
    
    // OTHER    -----------------------------
    
    /**
     * The length of a side of this size along the specified axis
     */
    @deprecated("Please use along(Axis2D) instead", "v1.1.2")
    def lengthAlong(axis: Axis2D) = along(axis)
    
    /**
     * A copy of this size with specified width
     */
    def withWidth(w: Double) = Size(w, height)
    
    /**
     * A copy of this size with specified height
     */
    def withHeight(h: Double) = Size(width, h)
    
    /**
     * A copy of this size with specified length along the target axis
     */
    def withLength(l: Double, axis: Axis2D) = axis match 
    {
        case X => withWidth(l)
        case Y => withHeight(l)
    }
    
    /**
      * Checks whether this size would fit into the other size
      * @param another Another size
      * @return Whether this size would fit into the other size
      */
    def fitsInto(another: Size) = width <= another.width && height <= another.height
    
    /**
     * @param another Another size
     * @return A copy of this size that fits into specified size. If this size already fits, returns this.
     */
    def fittedInto(another: Size) =
    {
        if (width <= another.width)
        {
            if (height <= another.height)
                this
            else
                withHeight(another.height)
        }
        else if (height <= another.height)
            withWidth(another.width)
        else
            another
    }
}