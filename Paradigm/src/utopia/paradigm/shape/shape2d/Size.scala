package utopia.paradigm.shape.shape2d

import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.template.typeless
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.{Model, ModelConvertible, Property, ValueConvertible}
import utopia.flow.operator.SignedOrZero
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.generic.SizeType
import utopia.paradigm.shape.shape3d.Vector3D

import java.awt.{Dimension, Insets}
import scala.util.Success

object Size extends FromModelFactory[Size]
{
    // ATTRIBUTES   --------------------------
    
    /**
     * A zero size
     */
    val zero = Size(0, 0)
    
    
    // IMPLEMENTED  --------------------------
    
    def apply(model: Model[Property]) = Success(
            Size(model("width").getDouble, model("height").getDouble))
    
    
    // OTHER    ------------------------------
    
    /**
      * @param width Width for this pair
      * @param height Height for this pair
      * @return A size based on that pair
      */
    def apply(width: Double, height: Double): Size = apply(Pair(width, height))
    
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
* @since Genesis 20.11.2018
**/
case class Size(override val dimensions2D: Pair[Double])
    extends Vector2DLike[Size] with ValueConvertible with ModelConvertible
        with TwoDimensional[Double] with SignedOrZero[Size] with SizedLike[Size]
{
    // COMPUTED    --------------------------
    
    /**
      * @return Width of this size
      */
    override def width = dimensions2D.first
    /**
      * @return Height of this size
      */
    override def height = dimensions2D.second
    
    /**
     * The area of this size (width * height)
     */
    def area = dimensions2D.product
    
    /**
     * A vector representation of this size
     */
    def toVector = Vector2D(dimensions2D)
    /**
      * A point representation of this size
      */
    def toPoint = Point(dimensions2D)
    
    /**
     * An awt representation of this size
     */
	def toDimension = new Dimension(width.ceil.toInt, height.ceil.toInt)
    
    /**
      * @return This size as a square shape (equal width and height).
      *         Decreases the larger side to match the smaller side.
      */
    def fitToSquare = Size(Pair.twice(minDimension))
    /**
      * @return This size as a square shape (equal width and height).
      *         Increases the smaller side to match the larger side.
      */
    def fillToSquare = Size(Pair.twice(maxDimension))
    
    
    // IMPLEMENTED    -----------------------
    
    override def repr = this
    override def size = this
    
    override def isPositive = dimensions2D.forall { _ > 0 }
    
    override protected def zero = Size.zero
    
    override def buildCopy(vector: Vector2D) = Size(vector.x, vector.y)
    override def buildCopy(vector: Vector3D) = Size(vector.x, vector.y)
    override def buildCopy(dimensions: IndexedSeq[Double]) =
    {
        if (dimensions.size >= 2)
            Size(dimensions.head, dimensions(1))
        else if (dimensions.isEmpty)
            Size.zero
        else
            Size(dimensions.head, 0)
    }
    override def withSize(size: Size) = size
    
    override def toValue = new Value(Some(this), SizeType)
    def toModel = Model.from("width" -> width, "height" -> height)
    override def toString = s"$width x $height"
    
    
    // OTHER    -----------------------------
    
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
      * Checks whether this size would fit into the other size
      * @param another Another size
      * @return Whether this size would fit into the other size
      */
    @deprecated("Replaced with fitsWithin(Size)", "v1.1")
    def fitsInto(another: Size) = testEqualityWith(another) { _ <= _ }
    
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